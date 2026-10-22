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
    void reconheceComandoHistoricoComOuSemAcento() {
        assertEquals(new Protocolo.Resultado(Protocolo.Tipo.RESUMIR_HISTORICO, "/historico"),
                protocolo.interpretar("/historico"));
        assertEquals(new Protocolo.Resultado(Protocolo.Tipo.RESUMIR_HISTORICO, "/histórico"),
                protocolo.interpretar("/histórico"));
    }

    @Test
    void reconheceHackerESeparaPerguntaDoComando() {
        assertEquals(new Protocolo.Resultado(Protocolo.Tipo.CONSULTAR_HACKER,
                        "como proteger meu servidor de senhas fracas?"),
                protocolo.interpretar("/hacker como proteger meu servidor de senhas fracas?"));
        assertEquals(new Protocolo.Resultado(Protocolo.Tipo.CONSULTAR_HACKER, ""),
                protocolo.interpretar("/hacker"));
    }

    @Test
    void classificaOutrosComandosComoDesconhecidos() {
        assertEquals(new Protocolo.Resultado(Protocolo.Tipo.COMANDO_DESCONHECIDO, "/privado ana oi"),
                protocolo.interpretar("/privado ana oi"));
    }
}
