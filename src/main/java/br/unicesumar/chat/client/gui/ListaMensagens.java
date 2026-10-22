package br.unicesumar.chat.client.gui;

import br.unicesumar.chat.client.LinhaServidor;
import br.unicesumar.chat.client.ModeloConversa;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
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
    private static final String PREFIXO_ASSISTENTE = "Assistente técnico: ";
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
        JScrollBar barra = rolagem.getVerticalScrollBar();
        barra.setUnitIncrement(18);
        barra.setOpaque(false);
        barra.setBorder(null);
        barra.setPreferredSize(new java.awt.Dimension(14, 0));
        barra.getAccessibleContext().setAccessibleName("Rolagem da conversa");
        barra.setToolTipText("Arraste para navegar pelo histórico");
        barra.setUI(new BarraRolagemUI());
        add(rolagem, BorderLayout.CENTER);
        JPanel superior = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        superior.setOpaque(false);
        novasMensagens.setFont(Tema.fonte(11, Font.BOLD));
        novasMensagens.setForeground(Tema.TEXTO);
        novasMensagens.setBackground(new Color(0x2D454F));
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
        boolean respostaAssistente = item.linha().tipo() == LinhaServidor.Tipo.SISTEMA
                && item.linha().texto().startsWith(PREFIXO_ASSISTENTE);
        if ((item.linha().tipo() == LinhaServidor.Tipo.MENSAGEM || respostaAssistente)
                && vazio.getParent() == itens) itens.remove(vazio);
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
                if (filho instanceof RespostaAssistente resposta) resposta.setTemaClaro(claro);
            }
        }
        novasMensagens.setForeground(claro ? new Color(0x101828) : Tema.TEXTO);
        novasMensagens.setBackground(claro ? new Color(0xE7EFEA) : new Color(0x2D454F));
        rolagem.getVerticalScrollBar().repaint();
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
        } else if (linha.tipo() == LinhaServidor.Tipo.SISTEMA
                && linha.texto().startsWith(PREFIXO_ASSISTENTE)) {
            RespostaAssistente resposta = new RespostaAssistente(
                    linha.texto().substring(PREFIXO_ASSISTENTE.length()));
            resposta.setTemaClaro(temaClaro);
            linhaPainel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));
            linhaPainel.add(resposta);
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
                if (componente instanceof RespostaAssistente resposta) {
                    resposta.setMaximumSize(new java.awt.Dimension(limite, Integer.MAX_VALUE));
                    resposta.setLarguraMaxima(limite);
                }
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

    private final class BarraRolagemUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void paintTrack(java.awt.Graphics contexto, javax.swing.JComponent componente,
                                  java.awt.Rectangle limites) {
            java.awt.Graphics2D grafico = (java.awt.Graphics2D) contexto.create();
            Tema.preparar(grafico);
            grafico.setColor(temaClaro ? new Color(0xDCE5DF) : Tema.SUPERFICIE);
            int largura = 3;
            int x = limites.x + (limites.width - largura) / 2;
            grafico.fillRoundRect(x, limites.y + 3, largura, Math.max(0, limites.height - 6), largura, largura);
            grafico.dispose();
        }

        @Override
        protected void paintThumb(java.awt.Graphics contexto, javax.swing.JComponent componente,
                                  java.awt.Rectangle limites) {
            if (limites.isEmpty() || !scrollbar.isEnabled()) return;
            java.awt.Graphics2D grafico = (java.awt.Graphics2D) contexto.create();
            Tema.preparar(grafico);
            grafico.setColor(temaClaro
                    ? (isThumbRollover() || isDragging ? new Color(0x23856E) : new Color(0x82988F))
                    : (isThumbRollover() || isDragging ? Tema.CIANO : new Color(0x627A7D)));
            int largura = 7;
            int x = limites.x + (limites.width - largura) / 2;
            int altura = Math.max(18, limites.height - 4);
            grafico.fillRoundRect(x, limites.y + 2, largura, Math.min(altura, limites.height - 2), largura, largura);
            grafico.dispose();
        }

        @Override
        protected JButton createDecreaseButton(int orientacao) { return criarBotaoVazio(); }

        @Override
        protected JButton createIncreaseButton(int orientacao) { return criarBotaoVazio(); }

        @Override
        protected java.awt.Dimension getMinimumThumbSize() { return new java.awt.Dimension(8, 28); }

        private JButton criarBotaoVazio() {
            JButton botao = new JButton();
            botao.setPreferredSize(new java.awt.Dimension(0, 0));
            botao.setMinimumSize(new java.awt.Dimension(0, 0));
            botao.setMaximumSize(new java.awt.Dimension(0, 0));
            botao.setOpaque(false);
            botao.setBorder(null);
            botao.setFocusable(false);
            return botao;
        }
    }

    private static final class Orbe extends javax.swing.JComponent {
        private Orbe() { setPreferredSize(new java.awt.Dimension(96, 96)); }
        @Override protected void paintComponent(java.awt.Graphics contexto) {
            java.awt.Graphics2D grafico = (java.awt.Graphics2D) contexto.create();
            Tema.preparar(grafico);
            int centroX = getWidth() / 2;
            int centroY = getHeight() / 2;
            grafico.setColor(Tema.SUPERFICIE);
            grafico.fillRoundRect(centroX - 31, centroY - 20, 62, 40, 14, 14);
            grafico.setColor(Tema.CIANO);
            grafico.drawRoundRect(centroX - 31, centroY - 20, 62, 40, 14, 14);
            grafico.drawLine(centroX - 15, centroY + 20, centroX - 21, centroY + 27);
            grafico.drawLine(centroX - 21, centroY + 27, centroX - 4, centroY + 20);
            for (int ponto = -10; ponto <= 10; ponto += 10) {
                grafico.fillOval(centroX + ponto - 2, centroY - 2, 4, 4);
            }
            grafico.dispose();
        }
    }
}
