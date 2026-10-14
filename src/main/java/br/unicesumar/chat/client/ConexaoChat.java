package br.unicesumar.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Mantém a conexão TCP do cliente e publica eventos em uma thread própria de leitura. */
public final class ConexaoChat implements AutoCloseable {
    private final String host;
    private final int porta;
    private final String nick;
    private final OuvinteChat ouvinte;
    private final AtomicBoolean encerrada = new AtomicBoolean();
    private final CountDownLatch leitorEncerrado = new CountDownLatch(1);
    private final CountDownLatch handshakeConcluido = new CountDownLatch(1);
    private final Logger registrador = Logger.getLogger(ConexaoChat.class.getName());
    private volatile Socket socket;
    private volatile PrintWriter saida;
    private volatile boolean aceita;

    /** Prepara uma conexão TCP para o host, porta e nick informados. */
    public ConexaoChat(String host, int porta, String nick, OuvinteChat ouvinte) {
        this.host = Objects.requireNonNull(host);
        this.porta = porta;
        this.nick = Objects.requireNonNull(nick);
        this.ouvinte = Objects.requireNonNull(ouvinte);
    }

    /** Abre o socket, envia o nick e inicia a leitura em uma thread nomeada. */
    public void conectar() throws IOException {
        if (socket != null) throw new IllegalStateException("Conexão já iniciada.");
        Socket novoSocket = new Socket(host, porta);
        socket = novoSocket;
        PrintWriter novoEscritor = new PrintWriter(
                new OutputStreamWriter(novoSocket.getOutputStream(), StandardCharsets.UTF_8), true);
        saida = novoEscritor;
        enviarLinha(nick);
        Thread leitor = new Thread(this::lerLinhas, "chat-rede-leitura");
        leitor.start();
    }

    /** Envia uma mensagem sem bloquear a thread de leitura. */
    public void enviar(String texto) throws IOException {
        if (!aceita) throw new IOException("O servidor ainda não aceitou o nick.");
        if (encerrada.get()) throw new IOException("A conexão está encerrada.");
        enviarLinha(texto);
    }

    /** Envia /quit quando possível e fecha os recursos uma única vez. */
    public void desconectar() {
        if (!encerrada.compareAndSet(false, true)) return;
        PrintWriter escritor = saida;
        if (aceita && escritor != null && socket != null && !socket.isClosed()) {
            escritor.println("/quit");
            escritor.flush();
        }
        fecharSocket();
    }

    /** Aguarda o término da thread de leitura. */
    public void aguardarEncerramento() throws InterruptedException {
        leitorEncerrado.await();
    }

    /** Aguarda o aceite ou a recusa do nick; útil para clientes de terminal. */
    public boolean aguardarAceite() throws InterruptedException {
        handshakeConcluido.await();
        return aceita;
    }

    /** Fecha a conexão pelo contrato AutoCloseable. */
    @Override
    public void close() {
        desconectar();
    }

    private void enviarLinha(String linha) throws IOException {
        PrintWriter escritor = saida;
        if (escritor == null) throw new IOException("Conexão ainda não foi aberta.");
        synchronized (escritor) {
            escritor.println(linha);
            escritor.flush();
            if (escritor.checkError()) throw new IOException("Falha ao escrever no socket.");
        }
    }

    private void lerLinhas() {
        String motivo = "Servidor encerrou a conexão.";
        try (BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String recebida;
            while ((recebida = entrada.readLine()) != null) {
                LinhaServidor linha = LinhaServidor.interpretar(recebida);
                if (!aceita) {
                    if (linha.tipo() == LinhaServidor.Tipo.ACEITE) {
                        aceita = true;
                        handshakeConcluido.countDown();
                        notificar(() -> ouvinte.aoAceitar(linha.remetente()));
                    } else if (linha.tipo() == LinhaServidor.Tipo.ERRO) {
                        handshakeConcluido.countDown();
                        notificar(() -> ouvinte.aoRejeitar(linha.texto()));
                    }
                    continue;
                }
                notificar(() -> ouvinte.aoReceber(linha));
            }
        } catch (IOException excecao) {
            motivo = "Conexão perdida: " + excecao.getMessage();
            if (!encerrada.get()) registrador.log(Level.FINE, "Leitura da conexão terminou.", excecao);
        } finally {
            encerrada.set(true);
            handshakeConcluido.countDown();
            fecharSocket();
            String motivoFinal = motivo;
            notificar(() -> ouvinte.aoDesconectar(motivoFinal));
            leitorEncerrado.countDown();
        }
    }

    private void fecharSocket() {
        Socket atual = socket;
        if (atual != null && !atual.isClosed()) {
            try {
                atual.close();
            } catch (IOException excecao) {
                registrador.log(Level.WARNING, "Falha ao fechar o socket do cliente.", excecao);
            }
        }
    }

    private void notificar(Runnable notificacao) {
        try {
            notificacao.run();
        } catch (RuntimeException excecao) {
            registrador.log(Level.WARNING, "O ouvinte do chat falhou ao processar um evento.", excecao);
        }
    }
}
