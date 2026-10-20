package br.unicesumar.chat.client.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListaMensagensVisualTest {
    @Test
    void usaBarraDeRolagemDaConversaCompacta() throws Exception {
        AtomicReference<JScrollBar> barra = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> localizarBarra(
                new ListaMensagens("ana", new Animacao()), barra));

        assertEquals("BarraRolagemUI", barra.get().getUI().getClass().getSimpleName());
        assertEquals(14, barra.get().getPreferredSize().width);
        assertEquals("Rolagem da conversa", barra.get().getAccessibleContext().getAccessibleName());
    }

    private void localizarBarra(Component componente, AtomicReference<JScrollBar> barra) {
        if (componente instanceof JScrollPane painel) barra.set(painel.getVerticalScrollBar());
        if (componente instanceof Container recipiente) {
            for (Component filho : recipiente.getComponents()) localizarBarra(filho, barra);
        }
    }
}
