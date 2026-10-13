package br.unicesumar.chat.support;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.model.MensagemInvalidaException;
import br.unicesumar.chat.model.Usuario;
import br.unicesumar.chat.model.UsuarioInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryMensagemRepositoryTest {
    @Test
    void salvarAtribuiIdsCrescentesSemAlterarMensagemOriginal() throws Exception {
        InMemoryMensagemRepository repositorio = new InMemoryMensagemRepository();

        Mensagem primeira = mensagem("primeira", 1);
        Mensagem segunda = mensagem("segunda", 2);
        Mensagem salva1 = repositorio.salvar(primeira);
        Mensagem salva2 = repositorio.salvar(segunda);

        assertNull(primeira.getId());
        assertEquals(1L, salva1.getId());
        assertEquals(2L, salva2.getId());
    }

    @Test
    void buscaAsUltimasEmOrdemCronologica() {
        InMemoryMensagemRepository repositorio = new InMemoryMensagemRepository();
        repositorio.salvar(mensagem("uma", 1));
        repositorio.salvar(mensagem("duas", 2));
        repositorio.salvar(mensagem("tres", 3));

        List<Mensagem> ultimas = repositorio.buscarUltimas(2);

        assertEquals(List.of("duas", "tres"), ultimas.stream().map(Mensagem::getConteudo).toList());
        assertEquals(List.of(2L, 3L), ultimas.stream().map(Mensagem::getId).toList());
    }

    @Test
    void limiteNaoPositivoRetornaListaVazia() {
        InMemoryMensagemRepository repositorio = new InMemoryMensagemRepository();

        assertTrue(repositorio.buscarUltimas(0).isEmpty());
        assertTrue(repositorio.buscarUltimas(-1).isEmpty());
    }

    private static Mensagem mensagem(String texto, int minuto) {
        try {
            return new Mensagem(null, new Usuario("ana"), texto,
                    LocalDateTime.of(2026, 10, 13, 9, minuto));
        } catch (MensagemInvalidaException | UsuarioInvalidoException excecao) {
            throw new AssertionError(excecao);
        }
    }
}
