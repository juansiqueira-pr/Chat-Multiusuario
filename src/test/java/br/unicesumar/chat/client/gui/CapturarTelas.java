package br.unicesumar.chat.client.gui;

import br.unicesumar.chat.client.LinhaServidor;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/** Gera capturas headless das duas telas principais para revisão visual manual. */
public final class CapturarTelas {
    private CapturarTelas() { }

    /** Renderiza telas de conexão e conversa em BufferedImage sem janela nativa. */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path destino = Path.of("docs", "img");
        Files.createDirectories(destino);
        AtomicReference<Throwable> falha = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            Animacao animacao = new Animacao();
            try {
                TelaConexao conexao = new TelaConexao(animacao);
                salvar(conexao, 1000, 680, destino.resolve("tela-conexao.png"));
                TelaChat chat = new TelaChat(animacao);
                chat.setConectado("ana");
                chat.adicionar(LinhaServidor.interpretar("[13/10 09:30] ana: Olá! Que bom encontrar vocês por aqui."));
                chat.adicionar(LinhaServidor.interpretar("* ana entrou"));
                chat.adicionar(LinhaServidor.interpretar("[13/10 09:31] bruno: Também gostei. A conversa fica clara e leve."));
                chat.adicionar(LinhaServidor.interpretar("[13/10 09:32] ana: E o histórico aparece quando alguém entra."));
                salvar(chat, 1000, 680, destino.resolve("tela-chat.png"));
            } catch (Throwable excecao) {
                falha.set(excecao);
            } finally {
                animacao.parar();
            }
        });
        if (falha.get() != null) throw new IllegalStateException("Falha ao gerar capturas.", falha.get());
    }

    private static void salvar(JComponent componente, int largura, int altura, Path arquivo) throws Exception {
        componente.setSize(largura, altura);
        organizar(componente);
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D grafico = imagem.createGraphics();
        Tema.preparar(grafico);
        componente.printAll(grafico);
        grafico.dispose();
        ImageIO.write(imagem, "png", arquivo.toFile());
    }

    private static void organizar(Component componente) {
        if (componente instanceof BolhaMensagem bolha) bolha.setEntrada(1);
        if (componente instanceof Container container) {
            container.doLayout();
            for (Component filho : container.getComponents()) organizar(filho);
        }
    }
}
