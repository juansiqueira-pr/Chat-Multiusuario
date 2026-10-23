package br.unicesumar.chat.client.gui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Map;

/** Exibe o último nível recebido sem interpretar o conteúdo da conversa. */
final class BarraClima extends JPanel {
    private static final Map<String, Aparencia> NIVEIS = Map.of(
            "CALMO", new Aparencia("Calmo", Tema.CLIMA_CALMO),
            "NEUTRO", new Aparencia("Neutro", Tema.CLIMA_NEUTRO),
            "TENSO", new Aparencia("Tenso", Tema.CLIMA_TENSO));

    private final JLabel rotulo = new JLabel("Clima da conversa");
    private final JLabel nivel = new JLabel("Neutro");
    private final Faixa faixa = new Faixa();
    private boolean temaClaro;
    private String valor = "NEUTRO";

    BarraClima() {
        super(new BorderLayout(10, 5));
        setOpaque(false);
        rotulo.setFont(Tema.fonte(11, Font.PLAIN));
        rotulo.setForeground(Tema.SECUNDARIO);
        nivel.setFont(Tema.fonte(11, Font.BOLD));
        nivel.setHorizontalAlignment(JLabel.RIGHT);
        nivel.setName("barra-clima-nivel");
        nivel.getAccessibleContext().setAccessibleName("Nível do clima da conversa: Neutro");
        faixa.setPreferredSize(new Dimension(1, 4));
        add(rotulo, BorderLayout.WEST);
        add(nivel, BorderLayout.EAST);
        add(faixa, BorderLayout.SOUTH);
        aplicarAparencia();
    }

    void setNivel(String recebido) {
        Runnable atualizar = () -> {
            if (!NIVEIS.containsKey(recebido)) return;
            valor = recebido;
            aplicarAparencia();
        };
        if (SwingUtilities.isEventDispatchThread()) atualizar.run();
        else SwingUtilities.invokeLater(atualizar);
    }

    void setTemaClaro(boolean claro) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setTemaClaro(claro));
            return;
        }
        temaClaro = claro;
        rotulo.setForeground(claro ? new Color(0x667085) : Tema.SECUNDARIO);
        aplicarAparencia();
    }

    String getNivel() { return valor; }

    private void aplicarAparencia() {
        Aparencia aparencia = NIVEIS.get(valor);
        nivel.setText(aparencia.nome());
        nivel.setForeground(temaClaro ? aparencia.cor().darker() : aparencia.cor());
        nivel.getAccessibleContext().setAccessibleName("Nível do clima da conversa: " + aparencia.nome());
        faixa.setCor(aparencia.cor());
    }

    private record Aparencia(String nome, Color cor) { }

    private static final class Faixa extends javax.swing.JComponent {
        private Color cor = Tema.CLIMA_NEUTRO;
        private Faixa() { setOpaque(false); }
        private void setCor(Color novaCor) { cor = novaCor; repaint(); }
        @Override protected void paintComponent(java.awt.Graphics contexto) {
            java.awt.Graphics2D grafico = (java.awt.Graphics2D) contexto.create();
            Tema.preparar(grafico);
            grafico.setColor(cor);
            grafico.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            grafico.dispose();
        }
    }
}
