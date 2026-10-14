package br.unicesumar.chat.client.gui;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

/** Pinta um fundo escuro vertical com luzes ciano e violeta discretas. */
public final class FundoFuturista extends JPanel {
    /** Cria a superfície de fundo sem depender de janela. */
    public FundoFuturista() {
        setOpaque(true);
        setBackground(Tema.FUNDO_INICIO);
    }

    /** Desenha gradiente vertical e halos radiais suaves. */
    @Override
    protected void paintComponent(Graphics contexto) {
        Graphics2D grafico = (Graphics2D) contexto.create();
        Tema.preparar(grafico);
        grafico.setPaint(new java.awt.GradientPaint(0, 0, Tema.FUNDO_INICIO, 0, getHeight(), Tema.FUNDO_FIM));
        grafico.fillRect(0, 0, getWidth(), getHeight());
        pintarHalo(grafico, getWidth() * 0.76f, getHeight() * 0.08f, Math.max(150, getWidth() * 0.38f), Tema.CIANO);
        pintarHalo(grafico, getWidth() * 0.18f, getHeight() * 0.82f, Math.max(150, getWidth() * 0.34f), Tema.VIOLETA);
        grafico.dispose();
    }

    private void pintarHalo(Graphics2D grafico, float x, float y, float raio, java.awt.Color cor) {
        grafico.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), raio,
                new float[]{0f, 1f}, new java.awt.Color[]{new java.awt.Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 22),
                new java.awt.Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 0)}));
        grafico.fillRect(0, 0, getWidth(), getHeight());
    }
}
