package br.unicesumar.chat.client.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

/** Apresenta a resposta do assistente em um cartão com texto legível e copiável. */
final class RespostaAssistente extends JPanel {
    private final JLabel titulo = new JLabel("Assistente técnico");
    private final JTextArea corpo;
    private boolean temaClaro;
    private int larguraMaxima = 620;

    RespostaAssistente(String resposta) {
        super(new BorderLayout(0, 7));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 15, 13, 15));
        titulo.setFont(Tema.fonte(11, Font.BOLD));
        add(titulo, BorderLayout.NORTH);

        corpo = new JTextArea(resposta);
        corpo.setFont(Tema.fonte(14, Font.PLAIN));
        corpo.setLineWrap(true);
        corpo.setWrapStyleWord(true);
        corpo.setEditable(false);
        corpo.setFocusable(true);
        corpo.setOpaque(false);
        corpo.setMargin(new Insets(0, 0, 0, 0));
        corpo.getAccessibleContext().setAccessibleName("Resposta do assistente técnico");
        add(corpo, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName("Resposta do assistente técnico");
        setToolTipText("Selecione o texto para copiá-lo");
        setLarguraMaxima(larguraMaxima);
        setTemaClaro(false);
    }

    /** Atualiza a largura e calcula a altura necessária para o texto quebrado. */
    public void setLarguraMaxima(int largura) {
        larguraMaxima = Math.max(220, largura);
        javax.swing.text.View vista = corpo.getUI().getRootView(corpo).getView(0);
        vista.setSize(larguraMaxima - 30, Integer.MAX_VALUE);
        int alturaTexto = (int) Math.ceil(vista.getPreferredSpan(javax.swing.text.View.Y_AXIS));
        int alturaTitulo = titulo.getPreferredSize().height;
        setPreferredSize(new Dimension(larguraMaxima, alturaTexto + alturaTitulo + 38));
        revalidate();
    }

    /** Aplica cores legíveis ao cartão no tema claro ou escuro. */
    public void setTemaClaro(boolean claro) {
        temaClaro = claro;
        titulo.setForeground(claro ? new Color(0x16765D) : Tema.CIANO);
        corpo.setForeground(claro ? new Color(0x172B2A) : Tema.TEXTO);
        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics contexto) {
        java.awt.Graphics2D grafico = (java.awt.Graphics2D) contexto.create();
        Tema.preparar(grafico);
        grafico.setColor(temaClaro ? new Color(0xEAF4EE) : new Color(0x203D47));
        grafico.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        grafico.setColor(temaClaro ? new Color(0xC7DED1) : new Color(0x3C625F));
        grafico.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        grafico.dispose();
        super.paintComponent(contexto);
    }
}
