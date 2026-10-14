package br.unicesumar.chat.client;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinhaServidorTest {
    @Test
    void interpretaMensagemComMetadados() {
        LinhaServidor linha = LinhaServidor.interpretar("[13/10 09:30] ana: oi");

        assertEquals(LinhaServidor.Tipo.MENSAGEM, linha.tipo());
        assertEquals("ana", linha.remetente());
        assertEquals(LocalTime.of(9, 30), linha.horario());
        assertEquals("oi", linha.texto());
    }

    @Test
    void preservaDoisPontosEColchetesNoTextoDaMensagem() {
        LinhaServidor linha = LinhaServidor.interpretar("[13/10 09:30] ana: [nota]: conferir");

        assertEquals("[nota]: conferir", linha.texto());
    }

    @Test
    void interpretaAvisoDeSistema() {
        assertEquals(LinhaServidor.Tipo.SISTEMA,
                LinhaServidor.interpretar("* bruno entrou").tipo());
    }

    @Test
    void interpretaAceiteDoServidor() {
        LinhaServidor linha = LinhaServidor.interpretar("* Conectado como ana");

        assertEquals(LinhaServidor.Tipo.ACEITE, linha.tipo());
        assertEquals("ana", linha.remetente());
    }

    @Test
    void interpretaErro() {
        assertEquals("Esse nome já está em uso",
                LinhaServidor.interpretar("! Esse nome já está em uso").texto());
    }

    @Test
    void linhaInesperadaViraSistemaSemLancarExcecao() {
        assertEquals(LinhaServidor.Tipo.SISTEMA,
                LinhaServidor.interpretar("conteúdo [livre]: qualquer").tipo());
        assertEquals("conteúdo [livre]: qualquer",
                LinhaServidor.interpretar("conteúdo [livre]: qualquer").texto());
    }
}
