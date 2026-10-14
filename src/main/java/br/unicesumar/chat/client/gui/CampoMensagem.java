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
    private final BotaoEnvio enviar = new BotaoEnvio();
    private Consumer<String> aoEnviar = ignorar -> { };

    /** Cria o campo multilinha e instala os atalhos de teclado solicitados. */
    public CampoMensagem() {
        super(new BorderLayout(8, 4));
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

    /** Dá foco ao editor para uso imediato pelo teclado. */
    public void solicitarFoco() {
        texto.requestFocusInWindow();
    }

    /** Pinta a superfície de vidro com borda de foco suave. */
    @Override
    protected void paintComponent(Graphics contexto) {
        Graphics2D grafico = (Graphics2D) contexto.create();
        Tema.preparar(grafico);
        grafico.setColor(new Color(255, 255, 255, 18));
        grafico.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        grafico.setColor(texto.isFocusOwner() ? new Color(76, 201, 240, 110) : Tema.BORDA_VIDRO);
        grafico.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        grafico.dispose();
        super.paintComponent(contexto);
    }

    private void enviarTexto() {
        String valor = texto.getText().trim();
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
        private BotaoEnvio() {
            setPreferredSize(new java.awt.Dimension(42, 38));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setToolTipText("Enviar mensagem");
            getAccessibleContext().setAccessibleName("Enviar mensagem");
        }

        @Override protected void paintComponent(Graphics contexto) {
            Graphics2D grafico = (Graphics2D) contexto.create();
            Tema.preparar(grafico);
            int alpha = isEnabled() ? 215 : 55;
            grafico.setColor(new Color(76, 201, 240, alpha));
            grafico.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
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
}
