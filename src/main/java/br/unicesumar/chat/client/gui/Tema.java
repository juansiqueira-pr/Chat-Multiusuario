package br.unicesumar.chat.client.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Reúne paleta, fonte e configuração de desenho comuns às telas Swing. */
public final class Tema {
    public static final Color FUNDO_INICIO = new Color(0x101E2B);
    public static final Color SUPERFICIE = new Color(0x1D3441);
    public static final Color BORDA = new Color(0x344955);
    public static final Color TEXTO = new Color(0xEAF1EE);
    public static final Color SECUNDARIO = new Color(0xA5B7B5);
    public static final Color CIANO = new Color(0x4FC3A1);
    public static final Color CORAL = new Color(0xE98B68);
    public static final Color SUCESSO = new Color(0x4FC3A1);
    public static final Color ERRO = new Color(0xE47773);
    private static final List<String> FONTES = List.of(
            "SF Pro Display", "SF Pro Text", "Segoe UI Variable", "Segoe UI", "Helvetica Neue", "Inter"
    );
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
        return FONTES.stream().filter(instaladas::contains).findFirst().orElse("Dialog");
    }
}
