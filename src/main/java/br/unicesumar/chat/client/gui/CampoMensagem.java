package br.unicesumar.chat.client.gui;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/** Editor multilinha com atalhos, contador e envio por botão desenhado. */
public final class CampoMensagem extends JPanel {
    private final JTextArea texto = new JTextArea(2, 32);
    private final JLabel contador = new JLabel(" ");
    private final BotaoEnvio enviar;
    private Consumer<String> aoEnviar = ignorar -> { };
    private boolean temaClaro;

    /** Cria o campo multilinha e instala os atalhos de teclado solicitados. */
    public CampoMensagem() {
        this(new Animacao());
    }

    /** Cria o editor usando o temporizador compartilhado da janela. */
    public CampoMensagem(Animacao animacao) {
        super(new BorderLayout(8, 4));
        enviar = new BotaoEnvio(animacao);
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 10));
        texto.setFont(Tema.fonte(14, java.awt.Font.PLAIN));
        texto.setForeground(Tema.TEXTO);
        texto.setCaretColor(Tema.CIANO);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setOpaque(false);
        texto.setMargin(new Insets(5, 4, 5, 4));
        texto.getAccessibleContext().setAccessibleName("Mensagem a enviar");
        ((javax.swing.text.AbstractDocument) texto.getDocument()).setDocumentFilter(new LimiteTexto());
        JScrollPane rolagem = new JScrollPane(texto);
        rolagem.setBorder(null);
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(rolagem, BorderLayout.CENTER);
        JPanel lateral = new JPanel(new BorderLayout(4, 4));
        lateral.setOpaque(false);
        contador.setFont(Tema.fonte(10, java.awt.Font.PLAIN));
        contador.setForeground(Tema.SECUNDARIO);
        contador.setHorizontalAlignment(JLabel.CENTER);
        lateral.add(contador, BorderLayout.NORTH);
        lateral.add(enviar, BorderLayout.SOUTH);
        add(lateral, BorderLayout.EAST);
        texto.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { atualizarContagem(); }
            @Override public void removeUpdate(DocumentEvent e) { atualizarContagem(); }
            @Override public void changedUpdate(DocumentEvent e) { atualizarContagem(); }
        });
        texto.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enviar");
        texto.getActionMap().put("enviar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { enviarTexto(); }
        });
        texto.getInputMap().put(KeyStroke.getKeyStroke("shift ENTER"), "quebra");
        texto.getActionMap().put("quebra", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { texto.replaceSelection("\n"); }
        });
        texto.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "limpar");
        texto.getActionMap().put("limpar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { texto.setText(""); }
        });
        enviar.addActionListener(evento -> enviarTexto());
        atualizarContagem();
    }

    /** Instala a ação que recebe o texto validado quando o usuário envia. */
    public void setAoEnviar(Consumer<String> acao) {
        aoEnviar = acao == null ? ignorar -> { } : acao;
    }

    /** Habilita ou desabilita o envio quando a conexão muda. */
    public void setHabilitado(boolean habilitado) {
        texto.setEnabled(habilitado);
        atualizarContagem();
    }

    /** Aplica cores de texto adequadas ao tema claro ou escuro. */
    public void setTemaClaro(boolean claro) {
        temaClaro = claro;
        texto.setForeground(claro ? new Color(0x101828) : Tema.TEXTO);
        texto.setCaretColor(claro ? Tema.CORAL : Tema.CIANO);
        contador.setForeground(claro ? new Color(0x667085) : Tema.SECUNDARIO);
        repaint();
    }

    /** Dá foco ao editor para uso imediato pelo teclado. */
    public void solicitarFoco() {
        texto.requestFocusInWindow();
    }

    /** Pinta a área de escrita com contraste e foco visível. */
    @Override
    protected void paintComponent(Graphics contexto) {
        Graphics2D grafico = (Graphics2D) contexto.create();
        Tema.preparar(grafico);
        grafico.setColor(temaClaro ? Color.WHITE : new Color(0x172B36));
        grafico.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        grafico.setColor(texto.isFocusOwner() ? Tema.CIANO : Tema.BORDA);
        grafico.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        grafico.dispose();
        super.paintComponent(contexto);
    }

    private void enviarTexto() {
        String valor = texto.getText().trim().replace('\n', ' ');
        if (valor.isEmpty() || valor.length() > 500 || !texto.isEnabled()) return;
        aoEnviar.accept(valor);
        texto.setText("");
    }

    private void atualizarContagem() {
        int tamanho = texto.getText().length();
        boolean valido = tamanho > 0 && tamanho <= 500 && texto.isEnabled();
        enviar.setEnabled(valido);
        contador.setText(tamanho >= 430 ? tamanho + "/500" : " ");
        contador.setForeground(tamanho > 500 ? Tema.ERRO : Tema.SECUNDARIO);
    }

    private static final class BotaoEnvio extends JButton {
        private final Animacao animacao;
        private double hover;
        private double pressionado;

        private BotaoEnvio(Animacao animacao) {
            this.animacao = animacao;
            setPreferredSize(new java.awt.Dimension(42, 38));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setToolTipText("Enviar mensagem");
            getAccessibleContext().setAccessibleName("Enviar mensagem");
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { animar(1, false); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { animar(0, false); }
                @Override public void mousePressed(java.awt.event.MouseEvent e) { animar(1, true); }
                @Override public void mouseReleased(java.awt.event.MouseEvent e) { animar(0, true); }
            });
        }

        private void animar(double destino, boolean escala) {
            double origem = escala ? pressionado : hover;
            animacao.animar(this, 180, progresso -> {
                if (escala) pressionado = origem + (destino - origem) * progresso;
                else hover = origem + (destino - origem) * progresso;
            });
        }

        @Override protected void paintComponent(Graphics contexto) {
            Graphics2D grafico = (Graphics2D) contexto.create();
            Tema.preparar(grafico);
            double escala = 1.0 - pressionado * 0.03;
            grafico.translate(getWidth() * (1.0 - escala) / 2.0, getHeight() * (1.0 - escala) / 2.0);
            grafico.scale(escala, escala);
            int alpha = isEnabled() ? 215 : 55;
            alpha = (int) Math.min(255, alpha + hover * 30);
            grafico.setColor(new Color(Tema.CIANO.getRed(), Tema.CIANO.getGreen(), Tema.CIANO.getBlue(), alpha));
            grafico.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
            grafico.setColor(isEnabled() ? Tema.TEXTO : Tema.SECUNDARIO);
            int centroX = getWidth() / 2;
            int centroY = getHeight() / 2;
            grafico.drawLine(centroX - 7, centroY, centroX + 7, centroY);
            grafico.drawLine(centroX + 7, centroY, centroX + 1, centroY - 6);
            grafico.drawLine(centroX + 7, centroY, centroX + 1, centroY + 6);
            if (isFocusOwner()) grafico.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            grafico.dispose();
        }
    }

    private static final class LimiteTexto extends javax.swing.text.DocumentFilter {
        @Override
        public void insertString(FilterBypass bypass, int offset, String texto,
                                 javax.swing.text.AttributeSet atributos)
                throws javax.swing.text.BadLocationException {
            if (texto != null && bypass.getDocument().getLength() + texto.length() <= 500) {
                bypass.insertString(offset, texto, atributos);
            }
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int tamanho, String texto,
                            javax.swing.text.AttributeSet atributos)
                throws javax.swing.text.BadLocationException {
            String substituto = texto == null ? "" : texto;
            if (bypass.getDocument().getLength() - tamanho + substituto.length() <= 500) {
                bypass.replace(offset, tamanho, substituto, atributos);
            }
        }
    }
}
