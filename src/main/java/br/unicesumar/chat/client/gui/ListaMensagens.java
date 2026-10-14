package br.unicesumar.chat.client.gui;

import br.unicesumar.chat.client.LinhaServidor;
import br.unicesumar.chat.client.ModeloConversa;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/** Apresenta até 500 itens, respeita rolagem manual e anima a chegada/rolagem. */
public final class ListaMensagens extends JPanel {
    private final ModeloConversa modelo;
    private final Animacao animacao;
    private final JPanel itens = new JPanel();
    private final JScrollPane rolagem = new JScrollPane(itens);
    private final JButton novasMensagens = new JButton("Novas mensagens ↓");
    private final JPanel vazio = new JPanel();
    private final JLabel mensagemVazia = new JLabel("Diga olá", javax.swing.SwingConstants.CENTER);
    private final Deque<JPanel> componentes = new ArrayDeque<>();
    private boolean seguindoFim = true;
    private boolean temaClaro;

    /** Cria a lista virtualmente simples com rolagem vertical e anúncio acessível. */
    public ListaMensagens(String nick, Animacao animacao) {
        super(new BorderLayout());
        this.modelo = new ModeloConversa(nick);
        this.animacao = animacao;
        setOpaque(false);
        itens.setLayout(new javax.swing.BoxLayout(itens, javax.swing.BoxLayout.Y_AXIS));
        itens.setOpaque(false);
        itens.setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.setBorder(null);
        rolagem.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.getVerticalScrollBar().setUnitIncrement(18);
        add(rolagem, BorderLayout.CENTER);
        JPanel superior = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        superior.setOpaque(false);
        novasMensagens.setFont(Tema.fonte(11, Font.BOLD));
        novasMensagens.setForeground(Tema.TEXTO);
        novasMensagens.setBackground(new Color(30, 42, 65, 230));
        novasMensagens.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        novasMensagens.setVisible(false);
        novasMensagens.setToolTipText("Rolar para as mensagens mais recentes");
        novasMensagens.getAccessibleContext().setAccessibleName("Novas mensagens; rolar até o fim");
        novasMensagens.addActionListener(evento -> rolarAoFim());
        superior.add(novasMensagens);
        add(superior, BorderLayout.NORTH);
        vazio.setLayout(new javax.swing.BoxLayout(vazio, javax.swing.BoxLayout.Y_AXIS));
        vazio.setOpaque(false);
        vazio.setPreferredSize(new java.awt.Dimension(260, 190));
        vazio.setMaximumSize(new java.awt.Dimension(260, 190));
        vazio.setVisible(true);
        Orbe orbe = new Orbe();
        orbe.setAlignmentX(CENTER_ALIGNMENT);
        mensagemVazia.setFont(Tema.fonte(18, Font.PLAIN));
        mensagemVazia.setForeground(Tema.SECUNDARIO);
        mensagemVazia.setAlignmentX(CENTER_ALIGNMENT);
        mensagemVazia.getAccessibleContext().setAccessibleName("A conversa está vazia. Diga olá.");
        vazio.add(javax.swing.Box.createVerticalStrut(18));
        vazio.add(orbe);
        vazio.add(javax.swing.Box.createVerticalStrut(12));
        vazio.add(mensagemVazia);
        itens.add(vazio);
        rolagem.getVerticalScrollBar().addAdjustmentListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                int restante = evento.getAdjustable().getMaximum()
                        - (evento.getAdjustable().getValue() + evento.getAdjustable().getVisibleAmount());
                if (restante < 32) {
                    seguindoFim = true;
                    novasMensagens.setVisible(false);
                } else {
                    seguindoFim = false;
                }
            }
        });
        itens.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { ajustarLarguras(); }
        });
        ChangeListener viewportListener = evento -> ajustarLarguras();
        rolagem.getViewport().addChangeListener(viewportListener);
    }

    /** Adiciona uma linha recebida, mantendo até 500 itens e agrupando mensagens. */
    public void adicionar(LinhaServidor linha) {
        boolean auto = seguindoFim;
        modelo.adicionar(linha);
        ModeloConversa.Item item = modelo.itens().getLast();
        if (item.linha().tipo() == LinhaServidor.Tipo.MENSAGEM && vazio.getParent() == itens) itens.remove(vazio);
        JPanel componente = criarLinha(item);
        componentes.addLast(componente);
        itens.add(componente);
        if (componentes.size() > 500) itens.remove(componentes.removeFirst());
        itens.revalidate();
        itens.repaint(componente.getBounds());
        if (auto) SwingUtilities.invokeLater(this::animarRolagemAoFim);
        else novasMensagens.setVisible(true);
    }

    /** Insere um divisor local antes das mensagens em tempo real após o histórico. */
    public void adicionarSeparadorHistorico() {
        JPanel separador = new JPanel(new FlowLayout(FlowLayout.CENTER));
        separador.setOpaque(false);
        JLabel legenda = new JLabel("Histórico anterior");
        legenda.setFont(Tema.fonte(10, Font.PLAIN));
        legenda.setForeground(temaClaro ? new Color(0x667085) : Tema.SECUNDARIO);
        separador.add(legenda);
        separador.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,
                separador.getPreferredSize().height + 8));
        itens.add(separador);
        itens.revalidate();
        if (seguindoFim) SwingUtilities.invokeLater(this::animarRolagemAoFim);
    }

    /** Aplica as cores de tema às bolhas e ao estado vazio. */
    public void setTemaClaro(boolean claro) {
        temaClaro = claro;
        mensagemVazia.setForeground(claro ? new Color(0x667085) : Tema.SECUNDARIO);
        for (JPanel painel : componentes) {
            for (java.awt.Component filho : painel.getComponents()) {
                if (filho instanceof BolhaMensagem bolha) bolha.setTemaClaro(claro);
            }
        }
        novasMensagens.setForeground(claro ? new Color(0x101828) : Tema.TEXTO);
        repaint();
    }

    /** Atualiza a identidade local usada para destacar mensagens próprias. */
    public void setNick(String nick) {
        modelo.setNick(nick);
    }

    private JPanel criarLinha(ModeloConversa.Item item) {
        LinhaServidor linha = item.linha();
        JPanel linhaPainel = new JPanel(new FlowLayout(
                item.propria() ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 5));
        linhaPainel.setOpaque(false);
        if (linha.tipo() == LinhaServidor.Tipo.MENSAGEM) {
            BolhaMensagem bolha = new BolhaMensagem(linha, item.propria(), item.agrupada());
            bolha.setTemaClaro(temaClaro);
            bolha.setEntrada(0);
            linhaPainel.add(bolha);
            animacao.animar(bolha, 220, bolha::setEntrada);
        } else {
            JLabel pílula = new JLabel(linha.texto());
            pílula.setFont(Tema.fonte(11, Font.PLAIN));
            pílula.setForeground(linha.tipo() == LinhaServidor.Tipo.ERRO
                    ? Tema.ERRO : (temaClaro ? new Color(0x667085) : Tema.SECUNDARIO));
            pílula.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            linhaPainel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 5));
            linhaPainel.add(pílula);
        }
        linhaPainel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,
                Math.max(54, linhaPainel.getPreferredSize().height + 8)));
        return linhaPainel;
    }

    private void ajustarLarguras() {
        int largura = Math.max(260, rolagem.getViewport().getWidth() - 52);
        int limite = Math.min(620, (int) (largura * 0.78));
        for (JPanel linha : componentes) {
            for (java.awt.Component componente : linha.getComponents()) {
                if (componente instanceof BolhaMensagem bolha) bolha.setMaximumSize(
                        new java.awt.Dimension(limite, Integer.MAX_VALUE));
                if (componente instanceof BolhaMensagem bolha) bolha.setLarguraMaxima(limite);
            }
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        ajustarLarguras();
    }

    private void rolarAoFim() {
        seguindoFim = true;
        novasMensagens.setVisible(false);
        animarRolagemAoFim();
    }

    private void animarRolagemAoFim() {
        animacao.cancelar(rolagem);
        int origem = rolagem.getVerticalScrollBar().getValue();
        animacao.animar(rolagem, 220, progresso -> {
            int maximoAtual = rolagem.getVerticalScrollBar().getMaximum();
            rolagem.getVerticalScrollBar().setValue((int) (origem + (maximoAtual - origem) * progresso));
        });
    }

    private static final class Orbe extends javax.swing.JComponent {
        private Orbe() { setPreferredSize(new java.awt.Dimension(96, 96)); }
        @Override protected void paintComponent(java.awt.Graphics contexto) {
            java.awt.Graphics2D grafico = (java.awt.Graphics2D) contexto.create();
            Tema.preparar(grafico);
            int d = Math.min(getWidth(), getHeight());
            grafico.setPaint(new java.awt.RadialGradientPaint(getWidth() / 2f, getHeight() / 2f, d / 2f,
                    new float[]{0f, 0.5f, 1f}, new Color[]{new Color(76, 201, 240, 90),
                    new Color(123, 97, 255, 35), new Color(123, 97, 255, 0)}));
            grafico.fillOval(0, 0, d, d);
            grafico.setColor(new Color(76, 201, 240, 120));
            grafico.fillOval(d / 3, d / 3, d / 3, d / 3);
            grafico.dispose();
        }
    }
}
