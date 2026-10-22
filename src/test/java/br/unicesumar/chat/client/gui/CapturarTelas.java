package br.unicesumar.chat.client.gui;

import br.unicesumar.chat.client.LinhaServidor;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
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
                String[] mensagens = {"Já encontrei o material da aula.", "Vou compartilhar o resumo aqui.",
                        "Perfeito, vou conferir.", "A atividade ficou para sexta.", "Obrigado pelo aviso.",
                        "Combinado. Até mais tarde.", "O histórico já está atualizado.", "Boa noite, pessoal."};
                for (int indice = 0; indice < mensagens.length; indice++) {
                    String remetente = indice % 2 == 0 ? "bruno" : "ana";
                    chat.adicionar(LinhaServidor.interpretar("[13/10 09:" + (33 + indice) + "] "
                            + remetente + ": " + mensagens[indice]));
                }
                chat.adicionar(LinhaServidor.interpretar("* Assistente técnico: "
                        + "Use consultas preparadas com parâmetros para separar os dados do comando SQL. "
                        + "Assim, o conteúdo recebido não é executado como parte da consulta."));
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
        if (arquivo.getFileName().toString().equals("tela-chat.png")) {
            rolarAoFim(componente);
        }
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

    private static void rolarAoFim(Component componente) {
        if (componente instanceof JScrollPane painel) {
            painel.getVerticalScrollBar().setValue(painel.getVerticalScrollBar().getMaximum());
        }
        if (componente instanceof Container recipiente) {
            for (Component filho : recipiente.getComponents()) rolarAoFim(filho);
        }
    }
}
