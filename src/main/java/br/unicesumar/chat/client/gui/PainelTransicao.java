package br.unicesumar.chat.client.gui;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Sobrepõe duas telas e faz uma troca curta em fade cruzado com leve deslocamento. */
public final class PainelTransicao extends JPanel {
    private final Animacao animacao;
    private Component atual;
    private Component anterior;
    private double progresso = 1;

    /** Cria o painel para a primeira tela visível e o animador da janela. */
    public PainelTransicao(Component inicial, Animacao animacao) {
        super(null);
        this.animacao = animacao;
        atual = inicial;
        add(inicial);
        setOpaque(false);
    }

    /** Substitui a tela atual por outra com fade cruzado e deslocamento de poucos pixels. */
    public void mostrar(Component nova) {
        if (nova == atual) return;
        if (anterior != null) remove(anterior);
        anterior = atual;
        atual = nova;
        add(nova);
        progresso = 0;
        revalidate();
        animacao.animar((JComponent) this, 230, valor -> {
            progresso = valor;
            if (valor >= 1 && anterior != null) {
                remove(anterior);
                anterior = null;
                revalidate();
            }
        });
    }

    /** Mantém as telas sobrepostas no tamanho atual da janela. */
    @Override
    public void doLayout() {
        if (anterior != null) anterior.setBounds(0, 0, getWidth(), getHeight());
        if (atual != null) atual.setBounds(0, 0, getWidth(), getHeight());
    }

    /** Renderiza as duas telas com alfa complementar apenas durante a transição. */
    @Override
    protected void paintChildren(Graphics contexto) {
        if (anterior == null) {
            super.paintChildren(contexto);
            return;
        }
        pintar(contexto, anterior, (float) (1 - progresso), (int) (-8 * progresso));
        pintar(contexto, atual, (float) progresso, (int) (8 * (1 - progresso)));
    }

    private void pintar(Graphics contexto, Component componente, float alpha, int deslocamento) {
        Graphics2D grafico = (Graphics2D) contexto.create();
        grafico.translate(deslocamento, 0);
        grafico.setComposite(java.awt.AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alpha))));
        componente.paint(grafico);
        grafico.dispose();
    }
}
