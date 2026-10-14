package br.unicesumar.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Executa um cliente TCP de console com leitura e escrita em threads separadas. */
public final class ClienteChat {
    private ClienteChat() { }

    /** Conecta usando [servidor] [porta] [nick] ou solicita o nick no teclado. */
    public static void main(String[] args) {
        Logger registrador = Logger.getLogger(ClienteChat.class.getName());
        String host = args.length > 0 ? args[0] : "localhost";
        int porta;
        try {
            porta = args.length > 1 ? Integer.parseInt(args[1]) : 5555;
        } catch (NumberFormatException excecao) {
            registrador.log(Level.SEVERE, "Porta inválida.", excecao);
            return;
        }

        PrintWriter tela = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String nick = args.length > 2 ? args[2] : lerNick(teclado, tela, registrador);
        if (nick == null) return;

        OuvinteChat ouvinte = new OuvinteChat() {
            @Override public void aoAceitar(String nome) { tela.println("* Conectado como " + nome); }
            @Override public void aoReceber(LinhaServidor linha) { tela.println(linha.original()); }
            @Override public void aoRejeitar(String motivo) { tela.println("! " + motivo); }
            @Override public void aoDesconectar(String motivo) { tela.println(motivo); }
        };
        try (ConexaoChat conexao = new ConexaoChat(host, porta, nick, ouvinte)) {
            conexao.conectar();
            if (!conexao.aguardarAceite()) return;
            Thread tecladoThread = new Thread(() -> enviarDoTeclado(teclado, conexao, registrador),
                    "cliente-teclado");
            tecladoThread.setDaemon(true);
            tecladoThread.start();
            conexao.aguardarEncerramento();
        } catch (IOException excecao) {
            registrador.log(Level.SEVERE, "Não foi possível conectar ou enviar ao servidor.", excecao);
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            registrador.log(Level.WARNING, "Cliente interrompido.", excecao);
        }
    }

    private static String lerNick(BufferedReader teclado, PrintWriter tela, Logger registrador) {
        tela.print("Nick: ");
        tela.flush();
        try {
            return teclado.readLine();
        } catch (IOException excecao) {
            registrador.log(Level.SEVERE, "Não foi possível ler o nick.", excecao);
            return null;
        }
    }

    private static void enviarDoTeclado(BufferedReader teclado, ConexaoChat conexao, Logger registrador) {
        try {
            String linha;
            while ((linha = teclado.readLine()) != null) {
                if ("/quit".equals(linha)) {
                    conexao.desconectar();
                    return;
                }
                conexao.enviar(linha);
            }
            conexao.desconectar();
        } catch (IOException excecao) {
            registrador.log(Level.WARNING, "Falha ao enviar dados ao servidor.", excecao);
            conexao.desconectar();
        }
    }
}
