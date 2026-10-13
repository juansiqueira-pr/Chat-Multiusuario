package br.unicesumar.chat.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MensagemTest {
    private final Usuario remetente = criarUsuario();
    private final LocalDateTime horario = LocalDateTime.of(2026, 10, 13, 9, 5);

    @Test
    void criaMensagemSemIdERemoveEspacosDasPontas() throws Exception {
        Mensagem mensagem = new Mensagem(null, remetente, "  Olá  ", horario);

        assertNull(mensagem.getId());
        assertEquals("Olá", mensagem.getConteudo());
    }

    @Test
    void rejeitaConteudoVazioOuAcimaDeQuinhentosCaracteres() {
        assertThrows(MensagemInvalidaException.class,
                () -> new Mensagem(null, remetente, "  ", horario));
        assertThrows(MensagemInvalidaException.class,
                () -> new Mensagem(null, remetente, "x".repeat(501), horario));
    }

    @Test
    void rejeitaRemetenteOuHorarioNulo() {
        assertThrows(MensagemInvalidaException.class,
                () -> new Mensagem(null, null, "texto", horario));
        assertThrows(MensagemInvalidaException.class,
                () -> new Mensagem(null, remetente, "texto", null));
    }

    @Test
    void formataMensagemComDiaMesHorarioNomeETexto() throws Exception {
        Mensagem mensagem = new Mensagem(null, remetente, "Olá", horario);

        assertEquals("[13/10 09:05] ana: Olá", mensagem.formatar());
    }

    @Test
    void comIdDevolveNovaMensagemSemAlterarOriginal() throws Exception {
        Mensagem original = new Mensagem(null, remetente, "texto", horario);
        Mensagem persistida = original.comId(42L);

        assertNotSame(original, persistida);
        assertNull(original.getId());
        assertEquals(42L, persistida.getId());
        assertEquals(original.getConteudo(), persistida.getConteudo());
    }

    private static Usuario criarUsuario() {
        try {
            return new Usuario("ana");
        } catch (UsuarioInvalidoException excecao) {
            throw new AssertionError(excecao);
        }
    }
}
