package br.unicesumar.chat.client.gui;

import br.unicesumar.chat.client.LinhaServidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Consumer;

/** Tela de conversa com status, histórico, campo de envio e opção de reconexão manual. */
public final class TelaChat extends JPanel {
    private final Animacao animacao;
    private final FundoFuturista fundo = new FundoFuturista();
    private final ListaMensagens mensagens;
    private final CampoMensagem campo;
    private final JLabel titulo = new JLabel("Chat ao vivo");
    private final JLabel identidade = new JLabel("Conectando…");
    private final JLabel status = new JLabel("Desconectado");
    private final PontoOnline pontoOnline = new PontoOnline();
    private final JLabel aviso = new JLabel("Conexão perdida");
    private final JButton reconectar = new JButton("Reconectar");
    private final BotaoTema botaoTema;
    private Consumer<String> aoEnviar = ignorar -> { };
    private Runnable aoReconectar = () -> { };
    private String nick = "";
    private boolean temaClaro;
    private boolean historicoSeparado;

    /** Cria a interface de conversa e compartilha o temporizador da janela. */
    public TelaChat(Animacao animacao) {
        super(new BorderLayout());
        this.animacao = animacao;
        mensagens = new ListaMensagens("", animacao);
        campo = new CampoMensagem(animacao);
        botaoTema = new BotaoTema(() -> setTemaClaro(!temaClaro));
        setOpaque(false);
        JPanel conteudo = new JPanel(new BorderLayout(0, 12));
        conteudo.setOpaque(false);
        conteudo.setBorder(BorderFactory.createEmptyBorder(18, 26, 18, 26));
        conteudo.add(criarCabecalho(), BorderLayout.NORTH);
        conteudo.add(mensagens, BorderLayout.CENTER);
        conteudo.add(campo, BorderLayout.SOUTH);
        fundo.setLayout(new BorderLayout());
        fundo.add(conteudo, BorderLayout.CENTER);
        add(fundo, BorderLayout.CENTER);
        campo.setAoEnviar(texto -> aoEnviar.accept(texto));
        aviso.setFont(Tema.fonte(12, Font.PLAIN));
        aviso.setForeground(Tema.ERRO);
        reconectar.setFont(Tema.fonte(12, Font.BOLD));
        reconectar.setForeground(Tema.TEXTO);
        reconectar.setOpaque(false);
        reconectar.setToolTipText("Voltar aos dados de conexão e tentar novamente");
        reconectar.addActionListener(evento -> aoReconectar.run());
        JPanel faixa = new JPanel(new BorderLayout(10, 0));
        faixa.setOpaque(true);
        faixa.setBackground(new Color(255, 107, 129, 20));
        faixa.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        faixa.add(aviso, BorderLayout.CENTER);
        faixa.add(reconectar, BorderLayout.EAST);
        faixa.setVisible(false);
        add(faixa, BorderLayout.SOUTH);
        aviso.setName("aviso-conexao");
        reconectar.setName("acao-reconectar");
        this.addPropertyChangeListener("conexaoPerdida", evento -> faixa.setVisible(Boolean.TRUE.equals(evento.getNewValue())));
        setMinimumSize(new java.awt.Dimension(720, 520));
    }

    /** Instala o envio, que será executado fora da EDT pela janela principal. */
    public void setAoEnviar(Consumer<String> acao) {
        aoEnviar = acao == null ? ignorar -> { } : acao;
    }

    /** Define a navegação de reconexão manual para a tela de entrada. */
    public void setAoReconectar(Runnable acao) {
        aoReconectar = acao == null ? () -> { } : acao;
    }

    /** Registra o nick aceito e atualiza o cabeçalho de conexão. */
    public void setConectado(String nome) {
        nick = nome;
        mensagens.setNick(nome);
        historicoSeparado = false;
        mensagens.setVisible(true);
        identidade.setText("Conectado como " + nome);
        status.setText("Online");
        status.setForeground(Tema.SUCESSO);
        campo.setHabilitado(true);
        botaoTema.setOnline(true);
        pontoOnline.setOnline(true);
        putClientProperty("conexaoPerdida", false);
        repaint();
    }

    /** Atualiza o estado de queda sem descartar o conteúdo em tela. */
    public void setDesconectado(String motivo) {
        status.setText("Desconectado");
        status.setForeground(Tema.ERRO);
        campo.setHabilitado(false);
        aviso.setText(motivo == null || motivo.isBlank() ? "Conexão perdida" : "Conexão perdida");
        putClientProperty("conexaoPerdida", true);
        botaoTema.setOnline(false);
        pontoOnline.setOnline(false);
    }

    /** Insere uma linha recebida e separa o histórico das mensagens ao vivo. */
    public void adicionar(LinhaServidor linha) {
        if (!historicoSeparado && linha.tipo() == LinhaServidor.Tipo.SISTEMA
                && linha.texto().equalsIgnoreCase(nick + " entrou")) {
            mensagens.adicionarSeparadorHistorico();
            historicoSeparado = true;
        }
        mensagens.adicionar(linha);
    }

    /** Atualiza fundo, textos e componentes de mensagem para o modo claro/escuro. */
    public void setTemaClaro(boolean claro) {
        temaClaro = claro;
        fundo.setTemaClaro(claro);
        Color texto = claro ? new Color(0x101828) : Tema.TEXTO;
        titulo.setForeground(texto);
        identidade.setForeground(claro ? new Color(0x667085) : Tema.SECUNDARIO);
        mensagens.setTemaClaro(claro);
        campo.setTemaClaro(claro);
        reconectar.setForeground(texto);
        botaoTema.repaint();
    }

    private JPanel criarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout(16, 0));
        cabecalho.setOpaque(false);
        JPanel tituloComStatus = new JPanel(new BorderLayout(10, 2));
        tituloComStatus.setOpaque(false);
        titulo.setFont(Tema.fonte(22, Font.BOLD));
        titulo.setForeground(Tema.TEXTO);
        identidade.setFont(Tema.fonte(12, Font.PLAIN));
        identidade.setForeground(Tema.SECUNDARIO);
        JPanel statusLinha = new JPanel(new BorderLayout(7, 0));
        statusLinha.setOpaque(false);
        status.setFont(Tema.fonte(11, Font.BOLD));
        status.setForeground(Tema.SUCESSO);
        statusLinha.add(pontoOnline, BorderLayout.WEST);
        statusLinha.add(status, BorderLayout.CENTER);
        JPanel textos = new JPanel(new BorderLayout(0, 3));
        textos.setOpaque(false);
        textos.add(titulo, BorderLayout.NORTH);
        textos.add(identidade, BorderLayout.CENTER);
        tituloComStatus.add(textos, BorderLayout.CENTER);
        tituloComStatus.add(statusLinha, BorderLayout.EAST);
        cabecalho.add(tituloComStatus, BorderLayout.CENTER);
        cabecalho.add(botaoTema, BorderLayout.EAST);
        return cabecalho;
    }

    private final class PontoOnline extends javax.swing.JComponent {
        private double pulso;
        private boolean online;

        private PontoOnline() { setPreferredSize(new java.awt.Dimension(10, 24)); }

        private void setOnline(boolean ativo) {
            online = ativo;
            if (!ativo) {
                animacao.cancelar(this);
                pulso = 0;
            } else animarPulso();
        }

        private void animarPulso() {
            animacao.animar(this, 1100, progresso -> {
                pulso = Math.sin(progresso * Math.PI);
                if (progresso >= 1 && online) animarPulso();
            });
        }

        @Override protected void paintComponent(Graphics contexto) {
            Graphics2D grafico = (Graphics2D) contexto.create();
            int alpha = 180 + (int) (75 * pulso);
            grafico.setColor(new Color(52, 211, 153, Math.min(255, alpha)));
            int d = 7;
            grafico.fillOval(1, (getHeight() - d) / 2, d, d);
            grafico.dispose();
        }
    }

    private final class BotaoTema extends JButton {
        private final Runnable alternar;
        private boolean online;

        private BotaoTema(Runnable alternar) {
            this.alternar = alternar;
            setPreferredSize(new java.awt.Dimension(42, 42));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setToolTipText("Alternar tema claro e escuro");
            getAccessibleContext().setAccessibleName("Alternar tema");
            addActionListener(evento -> this.alternar.run());
        }

        private void setOnline(boolean valor) { online = valor; }

        @Override protected void paintComponent(Graphics contexto) {
            Graphics2D grafico = (Graphics2D) contexto.create();
            Tema.preparar(grafico);
            grafico.setColor(new Color(255, 255, 255, temaClaro ? 210 : 18));
            grafico.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
            grafico.setColor(temaClaro ? new Color(0x475467) : Tema.TEXTO);
            int x = getWidth() / 2;
            int y = getHeight() / 2;
            if (temaClaro) {
                grafico.drawOval(x - 6, y - 6, 12, 12);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    grafico.drawLine(x + (int) (9 * Math.cos(a)), y + (int) (9 * Math.sin(a)),
                            x + (int) (12 * Math.cos(a)), y + (int) (12 * Math.sin(a)));
                }
            } else {
                grafico.fillOval(x - 7, y - 8, 15, 15);
                grafico.setColor(new Color(18, 26, 46));
                grafico.fillOval(x - 2, y - 11, 14, 14);
            }
            if (isFocusOwner()) {
                grafico.setColor(Tema.CIANO);
                grafico.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
            }
            grafico.dispose();
        }
    }
}
