package br.unicesumar.chat.server;

import br.unicesumar.chat.model.MensagemInvalidaException;
import br.unicesumar.chat.model.Usuario;
import br.unicesumar.chat.model.UsuarioInvalidoException;
import br.unicesumar.chat.service.ChatService;
import br.unicesumar.chat.service.ConexaoCliente;
import br.unicesumar.chat.service.FalhaEnvioException;
import br.unicesumar.chat.service.NomeEmUsoException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Atende o protocolo de uma conexão TCP e adapta a conexão ao serviço de chat. */
public final class ClienteHandler implements Runnable, ConexaoCliente {
    private final Socket socket;
    private final ChatService servico;
    private final Protocolo protocolo = new Protocolo();
    private final Consumer<ClienteHandler> aoEncerrar;
    private final Logger registrador = Logger.getLogger(ClienteHandler.class.getName());
    private volatile Usuario usuario;
    private volatile PrintWriter saida;

    /** Cria o handler com socket, serviço e callback para remoção da coleção ativa. */
    public ClienteHandler(Socket socket, ChatService servico, Consumer<ClienteHandler> aoEncerrar) {
        this.socket = Objects.requireNonNull(socket);
        this.servico = Objects.requireNonNull(servico);
        this.aoEncerrar = Objects.requireNonNull(aoEncerrar);
    }

    /** Executa handshake, leitura de linhas e desconexão segura do cliente. */
    @Override
    public void run() {
        try (BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter escritor = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            saida = escritor;
            if (negociarUsuario(entrada)) {
                processarLinhas(entrada);
            }
        } catch (IOException excecao) {
            if (!socket.isClosed()) {
                registrador.log(Level.WARNING, "Falha de leitura na conexão do cliente.", excecao);
            }
        } catch (RuntimeException excecao) {
            registrador.log(Level.SEVERE, "Falha inesperada no atendimento de um cliente.", excecao);
        } finally {
            if (usuario != null) {
                servico.desconectar(this);
            }
            fechar();
            try {
                aoEncerrar.accept(this);
            } catch (RuntimeException excecao) {
                registrador.log(Level.WARNING, "Não foi possível remover o handler encerrado.", excecao);
            }
        }
    }

    /** Retorna o usuário associado depois de um handshake válido. */
    @Override
    public Usuario getUsuario() {
        return usuario;
    }

    /** Escreve uma linha de forma sincronizada e detecta falha do PrintWriter. */
    @Override
    public synchronized void enviar(String linha) {
        PrintWriter escritor = saida;
        if (escritor == null || socket.isClosed()) {
            throw new FalhaEnvioException("A conexão do cliente está fechada.");
        }
        escritor.println(linha);
        escritor.flush();
        if (escritor.checkError()) {
            throw new FalhaEnvioException("Não foi possível escrever na conexão do cliente.");
        }
    }

    /** Fecha o socket para interromper leitura ou escrita em andamento. */
    public void fechar() {
        try {
            socket.close();
        } catch (IOException excecao) {
            registrador.log(Level.WARNING, "Falha ao fechar socket do cliente.", excecao);
        }
    }

    private boolean negociarUsuario(BufferedReader entrada) throws IOException {
        for (int tentativa = 0; tentativa < 3; tentativa++) {
            String nome = entrada.readLine();
            if (nome == null) {
                return false;
            }
            try {
                usuario = new Usuario(nome);
                servico.conectar(this);
                return true;
            } catch (UsuarioInvalidoException excecao) {
                enviar(ChatService.PREFIXO_ERRO + excecao.getMessage());
            } catch (NomeEmUsoException excecao) {
                enviar(ChatService.PREFIXO_ERRO + excecao.getMessage());
                usuario = null;
            }
        }
        return false;
    }

    private void processarLinhas(BufferedReader entrada) throws IOException {
        String linha;
        while ((linha = entrada.readLine()) != null) {
            Protocolo.Resultado resultado = protocolo.interpretar(linha);
            switch (resultado.tipo()) {
                case SAIR -> {
                    return;
                }
                case COMANDO_DESCONHECIDO -> enviar(ChatService.PREFIXO_ERRO + "comando desconhecido");
                case MENSAGEM -> {
                    try {
                        servico.enviarMensagem(usuario, resultado.conteudo());
                    } catch (MensagemInvalidaException excecao) {
                        enviar(ChatService.PREFIXO_ERRO + excecao.getMessage());
                    }
                }
            }
        }
    }
}
