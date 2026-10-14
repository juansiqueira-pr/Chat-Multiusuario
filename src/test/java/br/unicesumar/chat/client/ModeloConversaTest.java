package br.unicesumar.chat.client;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeloConversaTest {
    @Test
    void identificaMensagemPropriaSemDiferenciarMaiusculas() {
        ModeloConversa modelo = new ModeloConversa("Ana");
        modelo.adicionar(LinhaServidor.interpretar("[13/10 09:30] ana: oi"));

        assertTrue(modelo.itens().getFirst().propria());
    }

    @Test
    void agrupaMensagensSeguidasDoMesmoRemetenteDentroDeDoisMinutos() {
        ModeloConversa modelo = new ModeloConversa("eu");
        modelo.adicionar(LinhaServidor.interpretar("[13/10 09:30] ana: um"));
        modelo.adicionar(LinhaServidor.interpretar("[13/10 09:31] ANA: dois"));

        assertTrue(modelo.itens().getLast().agrupada());
    }

    @Test
    void agrupaMensagensQueAtravessamMeiaNoite() {
        ModeloConversa modelo = new ModeloConversa("eu");
        modelo.adicionar(LinhaServidor.interpretar("[31/12 23:59] ana: quase meia-noite"));
        modelo.adicionar(LinhaServidor.interpretar("[01/01 00:01] ana: virou o dia"));

        assertTrue(modelo.itens().getLast().agrupada());
    }

    @Test
    void mantemNoMaximoQuinhentosItens() {
        ModeloConversa modelo = new ModeloConversa("eu");
        IntStream.range(0, 510).forEach(i -> modelo.adicionar(
                LinhaServidor.interpretar("[13/10 09:30] pessoa: texto-" + i)));

        assertEquals(500, modelo.itens().size());
        assertEquals("texto-10", modelo.itens().getFirst().linha().texto());
    }
}
