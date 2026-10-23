package br.unicesumar.chat.client.gui;

import br.unicesumar.chat.client.LinhaServidor;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BarraClimaVisualTest {
    @Test
    void mostraNivelRecebidoEAtualizaNaEdt() throws Exception {
        AtomicReference<TelaChat> tela = new AtomicReference<>();
        AtomicReference<JLabel> nivel = new AtomicReference<>();
        Animacao animacao = new Animacao();
        SwingUtilities.invokeAndWait(() -> tela.set(new TelaChat(animacao)));

        CountDownLatch eventoEnfileirado = new CountDownLatch(1);
        Thread atualizacaoRede = new Thread(() -> {
            tela.get().adicionar(LinhaServidor.interpretar("* clima: TENSO"));
            eventoEnfileirado.countDown();
        }, "teste-evento-clima");
        atualizacaoRede.start();
        org.junit.jupiter.api.Assertions.assertTrue(eventoEnfileirado.await(2, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> localizarNivel(tela.get(), nivel));

        assertNotNull(nivel.get());
        assertEquals("Tenso", nivel.get().getText());
        SwingUtilities.invokeAndWait(animacao::parar);
    }

    private void localizarNivel(Component componente, AtomicReference<JLabel> nivel) {
        if ("barra-clima-nivel".equals(componente.getName()) && componente instanceof JLabel rotulo) {
            nivel.set(rotulo);
        }
        if (componente instanceof Container recipiente) {
            for (Component filho : recipiente.getComponents()) localizarNivel(filho, nivel);
        }
    }
}
