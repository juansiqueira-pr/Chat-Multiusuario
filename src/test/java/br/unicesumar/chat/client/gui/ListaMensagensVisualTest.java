package br.unicesumar.chat.client.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void respostaDoAssistenteTemTextoSelecionavelComQuebraAutomatica() throws Exception {
        String resposta = "Use consultas preparadas com parâmetros para separar os dados do comando SQL."
                + " Não concatene o conteúdo recebido diretamente na consulta.";
        AtomicReference<JTextArea> corpo = new AtomicReference<>();
        AtomicReference<JLabel> vazio = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            ListaMensagens lista = new ListaMensagens("ana", new Animacao());
            lista.adicionar(br.unicesumar.chat.client.LinhaServidor.interpretar(
                    "* Assistente técnico: " + resposta));
            localizarTexto(lista, corpo);
            localizarRotulo(lista, "Diga olá", vazio);
        });

        assertNotNull(corpo.get(), "a resposta longa deve ser renderizada em um componente que quebra linhas");
        assertEquals(resposta, corpo.get().getText());
        assertTrue(corpo.get().getLineWrap());
        assertTrue(corpo.get().getWrapStyleWord());
        assertFalse(corpo.get().isEditable());
        org.junit.jupiter.api.Assertions.assertNull(vazio.get(),
                "a resposta deve ocupar o estado vazio da conversa");
    }

    @Test
    void resumoDoHistoricoTemCartaoProprioComTextoSelecionavelEQuebrado() throws Exception {
        String resumo = "A conversa apresentou os principais pontos do projeto, incluindo "
                + "persistência, organização das tarefas e próximos passos para a equipe.";
        AtomicReference<JTextArea> corpo = new AtomicReference<>();
        AtomicReference<JLabel> vazio = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            ListaMensagens lista = new ListaMensagens("ana", new Animacao());
            lista.adicionar(br.unicesumar.chat.client.LinhaServidor.interpretar("* Resumo: " + resumo));
            localizarTextoIniciandoCom(lista, resumo, corpo);
            localizarRotulo(lista, "Diga olá", vazio);
        });

        assertNotNull(corpo.get(), "o resumo deve aparecer em um cartão de texto longo");
        assertEquals(resumo, corpo.get().getText());
        assertEquals("Resumo da conversa", corpo.get().getAccessibleContext().getAccessibleName());
        assertTrue(corpo.get().getLineWrap());
        assertTrue(corpo.get().getWrapStyleWord());
        assertFalse(corpo.get().isEditable());
        org.junit.jupiter.api.Assertions.assertNull(vazio.get(),
                "o resumo deve substituir o estado vazio da conversa");
    }

    private void localizarBarra(Component componente, AtomicReference<JScrollBar> barra) {
        if (componente instanceof JScrollPane painel) barra.set(painel.getVerticalScrollBar());
        if (componente instanceof Container recipiente) {
            for (Component filho : recipiente.getComponents()) localizarBarra(filho, barra);
        }
    }

    private void localizarTexto(Component componente, AtomicReference<JTextArea> texto) {
        if (componente instanceof JTextArea area && area.getText().startsWith("Use consultas preparadas")) {
            texto.set(area);
        }
        if (componente instanceof Container recipiente) {
            for (Component filho : recipiente.getComponents()) localizarTexto(filho, texto);
        }
    }

    private void localizarTextoIniciandoCom(Component componente, String inicio,
                                             AtomicReference<JTextArea> texto) {
        if (componente instanceof JTextArea area && area.getText().startsWith(inicio)) texto.set(area);
        if (componente instanceof Container recipiente) {
            for (Component filho : recipiente.getComponents()) localizarTextoIniciandoCom(filho, inicio, texto);
        }
    }

    private void localizarRotulo(Component componente, String valor, AtomicReference<JLabel> rotulo) {
        if (componente instanceof JLabel etiqueta && valor.equals(etiqueta.getText())) rotulo.set(etiqueta);
        if (componente instanceof Container recipiente) {
            for (Component filho : recipiente.getComponents()) localizarRotulo(filho, valor, rotulo);
        }
    }
}
