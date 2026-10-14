package br.unicesumar.chat.integracao;

import br.unicesumar.chat.client.ConexaoChat;
import br.unicesumar.chat.client.LinhaServidor;
import br.unicesumar.chat.client.OuvinteChat;
import br.unicesumar.chat.client.gui.Animacao;
import br.unicesumar.chat.client.gui.TelaChat;
import br.unicesumar.chat.persistence.jdbc.ConnectionFactory;
import br.unicesumar.chat.persistence.jdbc.InicializadorSchema;
import br.unicesumar.chat.persistence.jdbc.JdbcMensagemRepository;
import br.unicesumar.chat.server.Servidor;
import br.unicesumar.chat.service.ChatService;
import br.unicesumar.chat.service.ClientRegistry;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Container;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversaGuiIntegracaoTest {
    @Test
    void doisClientesGuiConversamEDesconectamQuandoServidorPara() throws Exception {
        String nomeBanco = "gui_" + UUID.randomUUID().toString().replace("-", "");
        ConnectionFactory conexoes = new ConnectionFactory(
                "jdbc:h2:mem:" + nomeBanco + ";DB_CLOSE_DELAY=-1", "sa", "");
        new InicializadorSchema(conexoes).inicializar();
        ChatService servico = new ChatService(new JdbcMensagemRepository(conexoes), new ClientRegistry(),
                Clock.fixed(Instant.parse("2026-10-13T12:30:00Z"), ZoneOffset.UTC), 20);
        Servidor servidor = new Servidor(0, servico);
        Animacao animacao = new Animacao();
        try {
            servidor.iniciar();
            ClienteGrafico ana = criarCliente("ana", servidor.getPortaLocal(), animacao);
            ClienteGrafico bruno = criarCliente("bruno", servidor.getPortaLocal(), animacao);
            ConexaoChat conexaoAna = ana.conexao();
            ConexaoChat conexaoBruno = bruno.conexao();
            try (conexaoAna; conexaoBruno) {
                conexaoAna.conectar();
                conexaoBruno.conectar();
                assertTrue(ana.aceito().await(5, TimeUnit.SECONDS), "ana deveria conectar");
                assertTrue(bruno.aceito().await(5, TimeUnit.SECONDS), "bruno deveria conectar");

                conexaoBruno.enviar("boa noite");
                assertTrue(ana.aguardarMensagem("bruno: boa noite"), "ana deveria receber a mensagem");
                assertTrue(bruno.aguardarMensagem("bruno: boa noite"), "bruno deveria receber a mensagem");
                SwingUtilities.invokeAndWait(() -> { });
                assertTrue(ana.mensagemVisivel("boa noite"), "a tela de ana deveria renderizar a mensagem");
                assertTrue(bruno.mensagemVisivel("boa noite"), "a tela de bruno deveria renderizar a mensagem");

                servidor.parar();
                assertTrue(ana.desconectado().await(5, TimeUnit.SECONDS), "ana deveria ver o servidor encerrar");
                assertTrue(bruno.desconectado().await(5, TimeUnit.SECONDS), "bruno deveria ver o servidor encerrar");
                SwingUtilities.invokeAndWait(() -> { });
                assertTrue(ana.statusVisivel("Desconectado"), "a tela de ana deveria indicar a queda");
                assertTrue(bruno.statusVisivel("Desconectado"), "a tela de bruno deveria indicar a queda");
            }
        } finally {
            servidor.parar();
            SwingUtilities.invokeAndWait(animacao::parar);
        }
    }

    private ClienteGrafico criarCliente(String nome, int porta, Animacao animacao) throws Exception {
        AtomicReference<TelaChat> tela = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> tela.set(new TelaChat(animacao)));
        ClienteGrafico cliente = new ClienteGrafico(tela.get());
        cliente.definirConexao(new ConexaoChat("127.0.0.1", porta, nome, cliente));
        return cliente;
    }

    private static final class ClienteGrafico implements OuvinteChat {
        private final TelaChat tela;
        private final CountDownLatch aceito = new CountDownLatch(1);
        private final CountDownLatch desconectado = new CountDownLatch(1);
        private final BlockingQueue<LinhaServidor> linhas = new LinkedBlockingQueue<>();
        private ConexaoChat conexao;

        private ClienteGrafico(TelaChat tela) { this.tela = tela; }
        private void definirConexao(ConexaoChat conexao) { this.conexao = conexao; }
        private ConexaoChat conexao() { return conexao; }
        private CountDownLatch aceito() { return aceito; }
        private CountDownLatch desconectado() { return desconectado; }

        private boolean aguardarMensagem(String trecho) throws InterruptedException {
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < limite) {
                LinhaServidor linha = linhas.poll(100, TimeUnit.MILLISECONDS);
                if (linha != null && linha.original().contains(trecho)) return true;
            }
            return false;
        }

        private boolean mensagemVisivel(String trecho) throws Exception {
            AtomicReference<Boolean> encontrada = new AtomicReference<>(false);
            SwingUtilities.invokeAndWait(() -> encontrada.set(contemTexto(tela, trecho)));
            return encontrada.get();
        }

        private boolean statusVisivel(String texto) throws Exception {
            AtomicReference<Boolean> encontrado = new AtomicReference<>(false);
            SwingUtilities.invokeAndWait(() -> encontrado.set(contemTexto(tela, texto)));
            return encontrado.get();
        }

        private static boolean contemTexto(Component componente, String texto) {
            if (componente instanceof JTextArea area && area.getText().contains(texto)) return true;
            if (componente instanceof javax.swing.JLabel etiqueta && etiqueta.getText().equals(texto)) return true;
            if (componente instanceof Container recipiente) {
                for (Component filho : recipiente.getComponents()) {
                    if (contemTexto(filho, texto)) return true;
                }
            }
            return false;
        }

        @Override public void aoAceitar(String nome) {
            aceito.countDown();
            SwingUtilities.invokeLater(() -> tela.setConectado(nome));
        }
        @Override public void aoReceber(LinhaServidor linha) {
            linhas.add(linha);
            SwingUtilities.invokeLater(() -> tela.adicionar(linha));
        }
        @Override public void aoRejeitar(String motivo) { desconectado.countDown(); }
        @Override public void aoDesconectar(String motivo) {
            desconectado.countDown();
            SwingUtilities.invokeLater(() -> tela.setDesconectado(motivo));
        }
    }
}
