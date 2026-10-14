package br.unicesumar.chat.client.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.Set;

/** Reúne paleta, fonte e configuração de desenho comuns às telas Swing. */
public final class Tema {
    public static final Color FUNDO_INICIO = new Color(0x0A0E1A);
    public static final Color FUNDO_FIM = new Color(0x121A2E);
    public static final Color VIDRO = new Color(255, 255, 255, 18);
    public static final Color BORDA_VIDRO = new Color(255, 255, 255, 27);
    public static final Color TEXTO = new Color(0xE8ECF5);
    public static final Color SECUNDARIO = new Color(0xAAB4C9);
    public static final Color CIANO = new Color(0x4CC9F0);
    public static final Color VIOLETA = new Color(0x7B61FF);
    public static final Color SUCESSO = new Color(0x34D399);
    public static final Color ERRO = new Color(0xFF6B81);
    private static final String[] FONTES = {
            "SF Pro Display", "SF Pro Text", "Segoe UI Variable", "Segoe UI", "Helvetica Neue", "Inter"
    };
    private static final String FAMILIA = escolherFonte();

    private Tema() { }

    /** Retorna a família tipográfica preferida que está instalada. */
    public static String familiaFonte() {
        return FAMILIA;
    }

    /** Cria uma fonte legível para o tamanho e peso pedidos. */
    public static Font fonte(float tamanho, int peso) {
        return new Font(FAMILIA, peso, Math.round(tamanho)).deriveFont(tamanho);
    }

    /** Liga antialiasing e qualidade de renderização em um contexto Java2D. */
    public static void preparar(Graphics2D grafico) {
        grafico.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        grafico.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        grafico.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        grafico.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static String escolherFonte() {
        Set<String> instaladas = Set.copyOf(Arrays.asList(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        return Arrays.stream(FONTES).filter(instaladas::contains).findFirst().orElse("Dialog");
    }
}
