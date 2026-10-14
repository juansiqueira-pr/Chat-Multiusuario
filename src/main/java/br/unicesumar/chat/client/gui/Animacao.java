package br.unicesumar.chat.client.gui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/** Executa transições ease-out em um único temporizador Swing por instância. */
public final class Animacao {
    private final List<Transicao> transicoes = new ArrayList<>();
    private final Timer timer = new Timer(16, evento -> atualizar());

    /** Anima o progresso de zero a um em uma duração limitada. */
    public void animar(JComponent componente, int duracaoMs, DoubleConsumer atualizacao) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            javax.swing.SwingUtilities.invokeLater(() -> animar(componente, duracaoMs, atualizacao));
            return;
        }
        transicoes.add(new Transicao(componente, Math.max(1, duracaoMs), atualizacao, System.nanoTime()));
        if (!timer.isRunning()) timer.start();
    }

    /** Indica se há uma transição ativa no temporizador. */
    public boolean estaAnimando() {
        return timer.isRunning();
    }

    /** Cancela as transições pendentes de um componente específico. */
    public void cancelar(JComponent componente) {
        transicoes.removeIf(transicao -> transicao.componente() == componente);
        if (transicoes.isEmpty()) timer.stop();
    }

    /** Cancela todas as transições ao encerrar uma janela. */
    public void parar() {
        transicoes.clear();
        timer.stop();
    }

    private void atualizar() {
        long agora = System.nanoTime();
        for (Transicao transicao : List.copyOf(transicoes)) {
            double bruto = Math.min(1.0, (agora - transicao.inicio()) / (transicao.duracaoMs() * 1_000_000.0));
            double easeOut = 1.0 - Math.pow(1.0 - bruto, 3.0);
            transicao.atualizacao().accept(easeOut);
            transicao.componente().repaint();
            if (bruto >= 1.0) transicoes.remove(transicao);
        }
        if (transicoes.isEmpty()) timer.stop();
    }

    private record Transicao(JComponent componente, int duracaoMs, DoubleConsumer atualizacao, long inicio) { }
}
