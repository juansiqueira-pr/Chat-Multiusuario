package br.unicesumar.chat.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioTest {
    @Test
    void aceitaNomeValidoEExponeNome() throws Exception {
        Usuario usuario = new Usuario("ana");

        assertEquals("ana", usuario.getNome());
    }

    @Test
    void rejeitaNomeVazioOuComEspacos() {
        assertThrows(UsuarioInvalidoException.class, () -> new Usuario(""));
        assertThrows(UsuarioInvalidoException.class, () -> new Usuario("  "));
        assertThrows(UsuarioInvalidoException.class, () -> new Usuario("ana maria"));
    }

    @Test
    void rejeitaNomeAcimaDeVinteCaracteres() {
        assertThrows(UsuarioInvalidoException.class, () -> new Usuario("abcdefghijklmnopqrstu"));
    }

    @Test
    void comparaNomeSemDiferenciarMaiusculas() throws Exception {
        assertEquals(new Usuario("Ana"), new Usuario("aNA"));
        assertEquals(new Usuario("Ana").hashCode(), new Usuario("aNA").hashCode());
    }

    @Test
    void comparaComLocaleRaiz() throws Exception {
        assertEquals(new Usuario("I"), new Usuario("i"));
        assertFalse(new Usuario("I").equals(new Usuario("ı")));
        assertTrue(new Usuario("I").equals(new Usuario("i")));
    }
}
