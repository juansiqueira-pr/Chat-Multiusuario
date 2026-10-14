package br.unicesumar.chat.client.gui;

import br.unicesumar.chat.client.ConexaoChat;
import br.unicesumar.chat.client.LinhaServidor;
import br.unicesumar.chat.client.OuvinteChat;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Inicializa a janela, troca telas e adapta eventos TCP à Event Dispatch Thread. */
public final class ClienteChatGui {
    private final Logger registrador = Logger.getLogger(ClienteChatGui.class.getName());
    private final Animacao animacao = new Animacao();
    private final TelaConexao telaConexao = new TelaConexao(animacao);
    private final TelaChat telaChat = new TelaChat(animacao);
    private final PainelTransicao telas = new PainelTransicao(telaConexao, animacao);
    private final ExecutorService envio = Executors.newSingleThreadExecutor(criarFabricaThreads());
    private final JFrame janela = new JFrame("Chat Multiusuário");
    private volatile ConexaoChat conexao;
    private volatile boolean encerrando;

    private ClienteChatGui() {
        configurarTelas();
        janela.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        janela.setContentPane(telas);
        janela.setMinimumSize(new Dimension(720, 520));
        janela.setSize(1000, 680);
        janela.setLocationRelativeTo(null);
        janela.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent evento) { encerrar(); }
            @Override public void windowClosed(WindowEvent evento) { encerrar(); }
        });
    }

    /** Abre a interface ou informa a limitação de ambiente sem cabeça de vídeo. */
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            Logger.getLogger(ClienteChatGui.class.getName())
                    .info("Ambiente headless: janela não iniciada; componentes podem ser renderizados em BufferedImage.");
            return;
        }
        SwingUtilities.invokeLater(() -> new ClienteChatGui().exibir());
    }

    private static ThreadFactory criarFabricaThreads() {
        return tarefa -> {
            Thread thread = new Thread(tarefa, "chat-envio");
            thread.setDaemon(false);
            return thread;
        };
    }

    private void exibir() {
        janela.setVisible(true);
        telaConexao.focarNome();
    }

    private void configurarTelas() {
        telaConexao.setAoConectar(this::conectar);
        telaChat.setAoEnviar(this::enviar);
        telaChat.setAoReconectar(this::voltarAConexao);
    }

    private void conectar(TelaConexao.PedidoConexao pedido) {
        telaConexao.setCarregando(true);
        telaConexao.mostrarErro(" ");
        ConexaoChat nova = new ConexaoChat(pedido.host(), pedido.porta(), pedido.nick(), criarOuvinte(pedido));
        conexao = nova;
        envio.execute(() -> {
            try {
                nova.conectar();
            } catch (IOException | RuntimeException excecao) {
                executarNaEdt(() -> {
                    if (encerrando || conexao != nova) return;
                    conexao = null;
                    telaConexao.setCarregando(false);
                    telaConexao.mostrarErro("Não foi possível conectar ao servidor.");
                    registrador.log(Level.INFO, "Tentativa de conexão não concluída.", excecao);
                });
            }
        });
    }

    private OuvinteChat criarOuvinte(TelaConexao.PedidoConexao pedido) {
        return new OuvinteChat() {
            @Override public void aoAceitar(String nome) {
                executarNaEdt(() -> {
                    if (encerrando) return;
                    telaConexao.setCarregando(false);
                    telaChat.setConectado(nome);
                    telas.mostrar(telaChat);
                });
            }

            @Override public void aoReceber(LinhaServidor linha) {
                executarNaEdt(() -> {
                    if (!encerrando) telaChat.adicionar(linha);
                });
            }

            @Override public void aoRejeitar(String motivo) {
                executarNaEdt(() -> {
                    if (encerrando) return;
                    telaConexao.setCarregando(false);
                    telaConexao.mostrarErro(motivo.toLowerCase(java.util.Locale.ROOT).contains("uso")
                            ? "Esse nome já está em uso" : motivo);
                    telas.mostrar(telaConexao);
                    ConexaoChat atual = conexao;
                    if (atual != null) atual.desconectar();
                });
            }

            @Override public void aoDesconectar(String motivo) {
                executarNaEdt(() -> {
                    if (encerrando) return;
                    if (conexao != null) conexao = null;
                    telaConexao.preencher(pedido.host(), pedido.porta(), pedido.nick());
                    telaChat.setDesconectado(motivo);
                });
            }
        };
    }

    private void enviar(String texto) {
        ConexaoChat atual = conexao;
        if (atual == null) return;
        envio.execute(() -> {
            try {
                atual.enviar(texto);
            } catch (IOException excecao) {
                registrador.log(Level.WARNING, "Não foi possível enviar a mensagem.", excecao);
                executarNaEdt(() -> {
                    if (!encerrando) telaChat.setDesconectado("Conexão perdida");
                });
            }
        });
    }

    private void voltarAConexao() {
        telas.mostrar(telaConexao);
        telaConexao.focarNome();
    }

    private void encerrar() {
        if (encerrando) return;
        encerrando = true;
        ConexaoChat atual = conexao;
        conexao = null;
        if (atual != null && !envio.isShutdown()) envio.execute(atual::desconectar);
        envio.shutdown();
        animacao.parar();
    }

    private void executarNaEdt(Runnable acao) {
        if (SwingUtilities.isEventDispatchThread()) acao.run();
        else SwingUtilities.invokeLater(acao);
    }
}
