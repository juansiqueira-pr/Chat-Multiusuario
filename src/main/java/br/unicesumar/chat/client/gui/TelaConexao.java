package br.unicesumar.chat.client.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

/** Formulário de conexão com validação inline e execução delegada à janela. */
public final class TelaConexao extends JPanel {
    private final FundoFuturista fundo = new FundoFuturista();
    private final JTextField servidor = new JTextField("localhost", 22);
    private final JTextField porta = new JTextField("5555", 22);
    private final JTextField nick = new JTextField(22);
    private final JLabel erro = new JLabel(" ");
    private final JLabel subtitulo = new JLabel("Converse em tempo real, com histórico compartilhado.");
    private final BotaoArredondado botao;
    private Consumer<PedidoConexao> aoConectar = pedido -> { };
    private boolean temaClaro;

    /** Cria o formulário com estilos, acessibilidade e confirmação por Enter. */
    public TelaConexao(Animacao animacao) {
        super(new BorderLayout());
        botao = new BotaoArredondado("Entrar", animacao);
        setOpaque(false);
        adicionarEstilosCampo(servidor, "Endereço do servidor");
        adicionarEstilosCampo(porta, "Porta TCP");
        adicionarEstilosCampo(nick, "Seu nome no chat");
        nick.addActionListener(evento -> validarEConectar());
        servidor.addActionListener(evento -> porta.requestFocusInWindow());
        porta.addActionListener(evento -> nick.requestFocusInWindow());
        botao.setAlignmentX(CENTER_ALIGNMENT);
        botao.addActionListener(evento -> validarEConectar());
        erro.setFont(Tema.fonte(12, Font.PLAIN));
        erro.setForeground(Tema.ERRO);
        erro.setAlignmentX(CENTER_ALIGNMENT);
        subtitulo.setForeground(Tema.SECUNDARIO);
        subtitulo.setFont(Tema.fonte(14, Font.PLAIN));

        JPanel cartao = new JPanel();
        cartao.setOpaque(false);
        cartao.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));
        cartao.setLayout(new javax.swing.BoxLayout(cartao, javax.swing.BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Entre na conversa");
        titulo.setFont(Tema.fonte(27, Font.BOLD));
        titulo.setForeground(Tema.TEXTO);
        titulo.setAlignmentX(CENTER_ALIGNMENT);
        subtitulo.setAlignmentX(CENTER_ALIGNMENT);
        cartao.add(titulo);
        cartao.add(javax.swing.Box.createVerticalStrut(8));
        cartao.add(subtitulo);
        cartao.add(javax.swing.Box.createVerticalStrut(26));
        cartao.add(linhaCampo("Servidor", servidor));
        cartao.add(javax.swing.Box.createVerticalStrut(14));
        cartao.add(linhaCampo("Porta", porta));
        cartao.add(javax.swing.Box.createVerticalStrut(14));
        cartao.add(linhaCampo("Nome", nick));
        cartao.add(javax.swing.Box.createVerticalStrut(12));
        cartao.add(erro);
        cartao.add(javax.swing.Box.createVerticalStrut(8));
        cartao.add(botao);

        JPanel central = new JPanel(new GridBagLayout());
        central.setOpaque(false);
        PainelVidro vidro = new PainelVidro();
        vidro.setLayout(new BorderLayout());
        vidro.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        vidro.add(cartao);
        vidro.setPreferredSize(new Dimension(430, 430));
        vidro.setBorder(BorderFactory.createCompoundBorder(
                new BordaVidro(), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        central.add(vidro, new GridBagConstraints());
        fundo.setLayout(new BorderLayout());
        fundo.add(central, BorderLayout.CENTER);
        add(fundo, BorderLayout.CENTER);
        setMinimumSize(new Dimension(720, 520));
    }

    /** Define a ação chamada depois que os campos locais forem validados. */
    public void setAoConectar(Consumer<PedidoConexao> acao) {
        aoConectar = acao == null ? pedido -> { } : acao;
    }

    /** Exibe o indicador animado durante a tentativa de conexão. */
    public void setCarregando(boolean ativo) {
        botao.setEnabled(!ativo);
        botao.setText(ativo ? "Conectando" : "Entrar");
        botao.setCarregando(ativo);
    }

    /** Mostra uma mensagem amigável na área de validação. */
    public void mostrarErro(String mensagem) {
        erro.setText(mensagem == null || mensagem.isBlank() ? " " : mensagem);
        erro.setForeground(Tema.ERRO);
    }

    /** Preenche novamente o último endereço e nick usado. */
    public void preencher(String host, int numeroPorta, String nome) {
        servidor.setText(host);
        porta.setText(Integer.toString(numeroPorta));
        nick.setText(nome);
    }

    /** Devolve os valores atuais para reconectar sem apagar os campos. */
    public PedidoConexao valoresAtuais() {
        try {
            return new PedidoConexao(servidor.getText().trim(), Integer.parseInt(porta.getText().trim()),
                    nick.getText().trim());
        } catch (NumberFormatException excecao) {
            return new PedidoConexao(servidor.getText().trim(), 5555, nick.getText().trim());
        }
    }

    /** Solicita foco inicial no campo de nome depois que a tela estiver visível. */
    public void focarNome() {
        SwingUtilities.invokeLater(nick::requestFocusInWindow);
    }

    /** Alterna fundo da tela de conexão entre as cores escuras e claras. */
    public void setTemaClaro(boolean claro) {
        temaClaro = claro;
        fundo.setTemaClaro(claro);
        Color texto = claro ? new Color(0x101828) : Tema.TEXTO;
        subtitulo.setForeground(claro ? new Color(0x667085) : Tema.SECUNDARIO);
        for (java.awt.Component componente : getComponents()) atualizarTexto(componente, texto, claro);
        repaint();
    }

    private void validarEConectar() {
        String host = servidor.getText().trim();
        String nome = nick.getText().trim();
        int numeroPorta;
        if (host.isEmpty()) { mostrarErro("Informe o servidor."); return; }
        if (nome.isEmpty() || nome.length() > 20 || nome.codePoints().anyMatch(Character::isWhitespace)) {
            mostrarErro("O nome precisa ter até 20 caracteres e não pode conter espaços.");
            nick.requestFocusInWindow();
            return;
        }
        try {
            numeroPorta = Integer.parseInt(porta.getText().trim());
            if (numeroPorta < 1 || numeroPorta > 65535) throw new NumberFormatException();
        } catch (NumberFormatException excecao) {
            mostrarErro("A porta deve ser um número entre 1 e 65535.");
            porta.requestFocusInWindow();
            return;
        }
        mostrarErro(" ");
        botao.requestFocusInWindow();
        aoConectar.accept(new PedidoConexao(host, numeroPorta, nome));
    }

    private JPanel linhaCampo(String rotulo, JTextField campo) {
        JPanel linha = new JPanel(new BorderLayout(0, 6));
        linha.setOpaque(false);
        JLabel titulo = new JLabel(rotulo);
        titulo.setFont(Tema.fonte(12, Font.BOLD));
        titulo.setForeground(temaClaro ? new Color(0x475467) : Tema.SECUNDARIO);
        linha.add(titulo, BorderLayout.NORTH);
        linha.add(campo, BorderLayout.CENTER);
        return linha;
    }

    private void adicionarEstilosCampo(JTextField campo, String nomeAcessivel) {
        campo.setFont(Tema.fonte(14, Font.PLAIN));
        campo.setForeground(Tema.TEXTO);
        campo.setCaretColor(Tema.CIANO);
        campo.setOpaque(false);
        campo.setBackground(new Color(255, 255, 255, 20));
        campo.setBorder(BorderFactory.createCompoundBorder(new BordaCampo(),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        campo.getAccessibleContext().setAccessibleName(nomeAcessivel);
    }

    private void atualizarTexto(java.awt.Component componente, Color texto, boolean claro) {
        if (componente instanceof JLabel etiqueta && componente != erro) etiqueta.setForeground(texto);
        if (componente instanceof JTextField campo) {
            campo.setForeground(texto);
            campo.setCaretColor(claro ? Tema.VIOLETA : Tema.CIANO);
            campo.setBackground(claro ? new Color(255, 255, 255, 220) : new Color(255, 255, 255, 20));
        }
        if (componente instanceof JPanel painel) {
            for (java.awt.Component filho : painel.getComponents()) atualizarTexto(filho, texto, claro);
        }
    }

    /** Dados validados necessários para iniciar uma conexão. */
    public record PedidoConexao(String host, int porta, String nick) { }

    private static final class BordaCampo extends javax.swing.border.AbstractBorder {
        @Override public void paintBorder(java.awt.Component c, java.awt.Graphics g, int x, int y, int w, int h) {
            Graphics2D grafico = (Graphics2D) g.create();
            Tema.preparar(grafico);
            grafico.setColor(c.getBackground());
            grafico.fillRoundRect(x, y, w - 1, h - 1, 14, 14);
            grafico.setColor(c.isFocusOwner() ? new Color(76, 201, 240, 180) : new Color(255, 255, 255, 35));
            grafico.drawRoundRect(x, y, w - 1, h - 1, 14, 14);
            grafico.dispose();
        }
        @Override public Insets getBorderInsets(java.awt.Component c) { return new Insets(6, 8, 6, 8); }
    }

    private static final class BordaVidro extends javax.swing.border.AbstractBorder {
        @Override public void paintBorder(java.awt.Component c, java.awt.Graphics g, int x, int y, int w, int h) {
            Graphics2D grafico = (Graphics2D) g.create();
            Tema.preparar(grafico);
            grafico.setColor(new Color(255, 255, 255, 25));
            grafico.drawRoundRect(x, y, w - 1, h - 1, 24, 24);
            grafico.dispose();
        }
        @Override public Insets getBorderInsets(java.awt.Component c) { return new Insets(1, 1, 1, 1); }
    }

    private static final class PainelVidro extends JPanel {
        private PainelVidro() { setOpaque(false); }
        @Override protected void paintComponent(java.awt.Graphics contexto) {
            Graphics2D grafico = (Graphics2D) contexto.create();
            Tema.preparar(grafico);
            grafico.setColor(new Color(255, 255, 255, 14));
            grafico.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            grafico.dispose();
        }
    }
}
