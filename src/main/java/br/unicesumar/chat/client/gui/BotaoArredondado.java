package br.unicesumar.chat.client.gui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Botão Java2D arredondado com foco visível e resposta suave ao ponteiro. */
public final class BotaoArredondado extends JButton {
    private final Animacao animacao;
    private double hover;
    private double pressionado;
    private boolean carregando;
    private double anguloCarregamento;

    /** Cria um botão preenchido com o texto acessível fornecido. */
    public BotaoArredondado(String texto) {
        this(texto, new Animacao());
    }

    /** Cria um botão que compartilha o temporizador da janela. */
    public BotaoArredondado(String texto, Animacao animacao) {
        super(texto);
        this.animacao = animacao;
        setFont(Tema.fonte(14, java.awt.Font.BOLD));
        setForeground(Tema.TEXTO);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setAccessibleContextNome(texto);
        setToolTipText(texto);
        setPreferredSize(new java.awt.Dimension(124, 44));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { animar(1, false); }
            @Override public void mouseExited(MouseEvent e) { animar(0, false); }
            @Override public void mousePressed(MouseEvent e) { animar(1, true); }
            @Override public void mouseReleased(MouseEvent e) { animar(0, true); }
        });
    }

    private void setAccessibleContextNome(String nome) {
        getAccessibleContext().setAccessibleName(nome);
    }

    private void animar(double destino, boolean escala) {
        double origem = escala ? pressionado : hover;
        animacao.animar(this, 190, progresso -> {
            double valor = origem + (destino - origem) * progresso;
            if (escala) pressionado = valor; else hover = valor;
        });
    }

    /** Mostra um indicador giratório enquanto a operação assíncrona está ativa. */
    public void setCarregando(boolean ativo) {
        carregando = ativo;
        if (!ativo) {
            animacao.cancelar(this);
            anguloCarregamento = 0;
        } else {
            girarIndicador();
        }
        repaint();
    }

    private void girarIndicador() {
        animacao.animar(this, 760, progresso -> {
            anguloCarregamento = progresso * 360;
            if (progresso >= 1 && carregando) girarIndicador();
        });
    }

    /** Pinta gradiente do acento, contorno de foco e escala de pressão. */
    @Override
    protected void paintComponent(Graphics contexto) {
        Graphics2D grafico = (Graphics2D) contexto.create();
        Tema.preparar(grafico);
        double escala = 1.0 - pressionado * 0.03;
        grafico.translate(getWidth() * (1.0 - escala) / 2.0, getHeight() * (1.0 - escala) / 2.0);
        grafico.scale(escala, escala);
        int alpha = isEnabled() ? (int) (205 + hover * 50) : 75;
        grafico.setPaint(new java.awt.GradientPaint(0, 0, comAlpha(new Color(12, 101, 131), alpha),
                getWidth(), getHeight(), comAlpha(new Color(83, 65, 171), alpha)));
        grafico.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 18, 18);
        if (carregando) {
            grafico.setColor(new Color(255, 255, 255, 220));
            grafico.setStroke(new java.awt.BasicStroke(2f));
            grafico.drawArc(getWidth() - 28, (getHeight() - 14) / 2, 14, 14,
                    (int) anguloCarregamento, 250);
        }
        if (isFocusOwner()) {
            grafico.setColor(new Color(232, 236, 245, 180));
            grafico.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 16, 16);
        }
        grafico.dispose();
        super.paintComponent(contexto);
    }

    private static Color comAlpha(Color cor, int alpha) {
        return new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), alpha);
    }
}
