package br.unicesumar.chat.client.gui;

import br.unicesumar.chat.client.LinhaServidor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.RoundRectangle2D;

/** Bolha com texto selecionável e metadados discretos para uma linha de conversa. */
public final class BolhaMensagem extends JPanel {
    private final boolean propria;
    private final JTextArea editor;
    private final JLabel cabecalho;
    private double entrada = 1.0;
    private boolean claro;
    private int larguraMaxima = 460;

    /** Cria uma bolha acessível para a linha e regras de agrupamento fornecidas. */
    public BolhaMensagem(LinhaServidor linha, boolean propria, boolean agrupada) {
        super(new BorderLayout(8, 4));
        this.propria = propria;
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 10, 14));
        setAlignmentX(propria ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);
        if (!agrupada && linha.tipo() == LinhaServidor.Tipo.MENSAGEM) {
            String metadados = linha.remetente() + "  ·  " + (linha.horario() == null ? "" : linha.horario());
            cabecalho = new JLabel(metadados);
            cabecalho.setFont(Tema.fonte(11, java.awt.Font.PLAIN));
            cabecalho.setForeground(Tema.SECUNDARIO);
            add(cabecalho, BorderLayout.NORTH);
        } else {
            cabecalho = null;
        }
        JTextArea texto = new JTextArea(linha.texto());
        editor = texto;
        texto.setFont(Tema.fonte(14, java.awt.Font.PLAIN));
        texto.setForeground(Tema.TEXTO);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setEditable(false);
        texto.setFocusable(true);
        texto.setOpaque(false);
        texto.setMargin(new Insets(0, 0, 0, 0));
        texto.setSelectedTextColor(Tema.TEXTO);
        texto.setSelectionColor(new Color(76, 201, 240, 90));
        texto.getAccessibleContext().setAccessibleName("Mensagem de "
                + (linha.remetente().isEmpty() ? linha.tipo().name().toLowerCase() : linha.remetente()));
        add(texto, BorderLayout.CENTER);
        setToolTipText(linha.original());
        setPreferredSize(new Dimension(460, getPreferredSize().height));
    }

    /** Atualiza a entrada da bolha de forma incremental, chamada pela animação. */
    public void setEntrada(double progresso) {
        entrada = Math.max(0, Math.min(1, progresso));
    }

    /** Aplica as cores de texto adequadas ao tema atual. */
    public void setTemaClaro(boolean ativo) {
        claro = ativo;
        editor.setForeground(ativo && !propria ? new Color(0x101828) : Tema.TEXTO);
        if (cabecalho != null) cabecalho.setForeground(ativo ? new Color(0x667085) : Tema.SECUNDARIO);
        repaint();
    }

    /** Limita a largura e recalcula altura de texto com quebra real de linha. */
    public void setLarguraMaxima(int largura) {
        larguraMaxima = Math.max(200, largura);
        if (getPreferredSize().width == larguraMaxima) return;
        javax.swing.text.View vista = editor.getUI().getRootView(editor).getView(0);
        vista.setSize(larguraMaxima - 30, Integer.MAX_VALUE);
        int alturaTexto = (int) Math.ceil(vista.getPreferredSpan(javax.swing.text.View.Y_AXIS));
        int alturaCabecalho = cabecalho == null ? 0 : 18;
        setPreferredSize(new Dimension(larguraMaxima, alturaTexto + alturaCabecalho + 28));
        revalidate();
    }

    /** Pinta vidro ou gradiente próprio com entrada curta de transparência e deslocamento. */
    @Override
    protected void paintComponent(Graphics contexto) {
        Graphics2D grafico = (Graphics2D) contexto.create();
        Tema.preparar(grafico);
        grafico.translate(0, (1.0 - entrada) * 10.0);
        grafico.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) entrada));
        RoundRectangle2D forma = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        if (propria) {
            grafico.setPaint(new java.awt.GradientPaint(0, 0, new Color(12, 101, 131, 238),
                    getWidth(), getHeight(), new Color(83, 65, 171, 238)));
        } else {
            grafico.setColor(new Color(255, 255, 255, 20));
        }
        grafico.fill(forma);
        grafico.setColor(propria ? new Color(170, 220, 255, 70) : Tema.BORDA_VIDRO);
        grafico.draw(forma);
        grafico.dispose();
        super.paintComponent(contexto);
    }
}
