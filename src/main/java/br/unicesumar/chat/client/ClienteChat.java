package br.unicesumar.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Executa um cliente TCP de console com leitura e escrita em threads separadas. */
public final class ClienteChat {
    private ClienteChat() {
    }

    /** Conecta usando [servidor] [porta] [nick] ou solicita o nick no teclado. */
    public static void main(String[] args) {
        Logger registrador = Logger.getLogger(ClienteChat.class.getName());
        String servidor = args.length > 0 ? args[0] : "localhost";
        int porta;
        try {
            porta = args.length > 1 ? Integer.parseInt(args[1]) : 5555;
        } catch (NumberFormatException excecao) {
            registrador.log(Level.SEVERE, "Porta inválida.", excecao);
            return;
        }

        PrintWriter tela = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        BufferedReader teclado = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String nick = args.length > 2 ? args[2] : null;
        if (nick == null) {
            tela.print("Nick: ");
            tela.flush();
            try {
                nick = teclado.readLine();
            } catch (IOException excecao) {
                registrador.log(Level.SEVERE, "Não foi possível ler o nick.", excecao);
                return;
            }
            if (nick == null) {
                return;
            }
        }

        try (Socket socket = new Socket(servidor, porta);
             BufferedReader entradaServidor = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saidaServidor = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String nickInicial = nick;
            Thread leitorServidor = new Thread(
                    () -> receberMensagens(socket, entradaServidor, tela, registrador), "cliente-receptor");
            Thread leitorTeclado = new Thread(
                    () -> enviarMensagens(socket, teclado, saidaServidor, nickInicial, registrador),
                    "cliente-teclado");
            leitorTeclado.setDaemon(true);
            leitorServidor.start();
            leitorTeclado.start();
            leitorServidor.join();
        } catch (IOException excecao) {
            registrador.log(Level.SEVERE, "Não foi possível conectar ao servidor.", excecao);
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            registrador.log(Level.WARNING, "Cliente interrompido.", excecao);
        }
    }

    private static void receberMensagens(Socket socket, BufferedReader entrada, PrintWriter tela,
                                         Logger registrador) {
        try {
            String linha;
            while ((linha = entrada.readLine()) != null) {
                tela.println(linha);
            }
            tela.println("Conexão encerrada pelo servidor.");
        } catch (IOException excecao) {
            if (!socket.isClosed()) {
                registrador.log(Level.WARNING, "Falha ao receber mensagens do servidor.", excecao);
            }
        } finally {
            try {
                socket.close();
            } catch (IOException excecao) {
                registrador.log(Level.WARNING, "Falha ao fechar conexão do cliente.", excecao);
            }
        }
    }

    private static void enviarMensagens(Socket socket, BufferedReader teclado, PrintWriter saida,
                                        String nickInicial, Logger registrador) {
        try {
            escreverLinha(saida, nickInicial);
            String linha;
            while ((linha = teclado.readLine()) != null) {
                escreverLinha(saida, linha);
                if ("/quit".equals(linha)) {
                    socket.shutdownOutput();
                    return;
                }
            }
            socket.shutdownOutput();
        } catch (IOException excecao) {
            if (!socket.isClosed()) {
                registrador.log(Level.WARNING, "Falha ao enviar dados ao servidor.", excecao);
            }
        }
    }

    private static void escreverLinha(PrintWriter saida, String linha) throws IOException {
        saida.println(linha);
        saida.flush();
        if (saida.checkError()) {
            throw new IOException("Escrita no socket falhou.");
        }
    }
}
