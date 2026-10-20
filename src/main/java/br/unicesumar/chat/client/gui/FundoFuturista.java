package br.unicesumar.chat.client.gui;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Pinta o fundo uniforme compartilhado pelas telas do chat. */
public final class FundoFuturista extends JPanel {
    private boolean claro;
    /** Cria a superfície de fundo sem depender de janela. */
    public FundoFuturista() {
        setOpaque(true);
        setBackground(Tema.FUNDO_INICIO);
    }

    /** Alterna o fundo entre as paletas escura e clara. */
    public void setTemaClaro(boolean ativo) {
        claro = ativo;
        repaint();
    }

    /** Pinta uma superfície uniforme para manter o contraste dos textos. */
    @Override
    protected void paintComponent(Graphics contexto) {
        Graphics2D grafico = (Graphics2D) contexto.create();
        Tema.preparar(grafico);
        if (claro) {
            grafico.setColor(new java.awt.Color(0xEDF1EB));
        } else {
            grafico.setColor(Tema.FUNDO_INICIO);
        }
        grafico.fillRect(0, 0, getWidth(), getHeight());
        grafico.dispose();
    }
}
