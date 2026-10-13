package br.unicesumar.chat.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocoloTest {
    private final Protocolo protocolo = new Protocolo();

    @Test
    void classificaTextoComumComoMensagem() {
        assertEquals(new Protocolo.Resultado(Protocolo.Tipo.MENSAGEM, "Olá"),
                protocolo.interpretar("Olá"));
    }

    @Test
    void reconheceComandoQuit() {
        assertEquals(new Protocolo.Resultado(Protocolo.Tipo.SAIR, "/quit"),
                protocolo.interpretar("/quit"));
    }

    @Test
    void classificaOutrosComandosComoDesconhecidos() {
        assertEquals(new Protocolo.Resultado(Protocolo.Tipo.COMANDO_DESCONHECIDO, "/privado ana oi"),
                protocolo.interpretar("/privado ana oi"));
    }
}
