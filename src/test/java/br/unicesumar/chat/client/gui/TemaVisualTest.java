package br.unicesumar.chat.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemaVisualTest {
    @Test
    void usaPaletaDoChatDoCampus() {
        assertEquals(0x101E2B, rgb(Tema.FUNDO_INICIO));
        assertEquals(0x1D3441, rgb(Tema.SUPERFICIE));
        assertEquals(0xEAF1EE, rgb(Tema.TEXTO));
        assertEquals(0xA5B7B5, rgb(Tema.SECUNDARIO));
        assertEquals(0x4FC3A1, rgb(Tema.CIANO));
        assertEquals(0xE98B68, rgb(Tema.CORAL));
    }

    private int rgb(java.awt.Color cor) {
        return cor.getRGB() & 0xFFFFFF;
    }
}
