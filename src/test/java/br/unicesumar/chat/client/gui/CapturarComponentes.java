package br.unicesumar.chat.client.gui;

import br.unicesumar.chat.client.LinhaServidor;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Gera capturas de revisão dos componentes sem criar janelas nativas. */
public final class CapturarComponentes {
    private CapturarComponentes() { }

    /** Salva PNGs dos componentes Java2D em modo headless. */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path destino = Path.of("docs", "img");
        Files.createDirectories(destino);
        salvar(new FundoFuturista(), 1000, 680, destino.resolve("fundo-futurista.png"));
        BotaoArredondado botao = new BotaoArredondado("Entrar");
        salvar(botao, 160, 60, destino.resolve("botao-arredondado.png"));
        BolhaMensagem minha = new BolhaMensagem(
                LinhaServidor.interpretar("[13/10 09:30] ana: Olá! Como está o chat hoje?"), true, false);
        salvar(comFundo(minha), 520, 112, destino.resolve("bolha-propria.png"));
        BolhaMensagem outra = new BolhaMensagem(
                LinhaServidor.interpretar("[13/10 09:31] bruno: Tudo certo, ana."), false, false);
        salvar(comFundo(outra), 520, 112, destino.resolve("bolha-recebida.png"));
        salvar(comFundo(new CampoMensagem()), 620, 96, destino.resolve("campo-mensagem.png"));
    }

    private static JComponent comFundo(JComponent componente) {
        FundoFuturista fundo = new FundoFuturista();
        fundo.setLayout(new java.awt.GridBagLayout());
        fundo.add(componente);
        return fundo;
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
        if (componente instanceof Container container) {
            container.doLayout();
            for (Component filho : container.getComponents()) organizar(filho);
        }
    }
}
