package br.unicesumar.chat.service;

import br.unicesumar.chat.model.Usuario;
import br.unicesumar.chat.model.UsuarioInvalidoException;
import br.unicesumar.chat.support.FakeConexao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientRegistryTest {
    @Test
    void rejeitaNomeDuplicadoSemDiferenciarMaiusculas() throws Exception {
        ClientRegistry registro = new ClientRegistry();

        assertTrue(registro.adicionar(new FakeConexao(new Usuario("Ana"))));
        assertFalse(registro.adicionar(new FakeConexao(new Usuario("aNA"))));
        assertEquals(1, registro.quantidade());
    }

    @Test
    void removeClienteERefleteQuantidade() throws Exception {
        ClientRegistry registro = new ClientRegistry();
        FakeConexao conexao = new FakeConexao(new Usuario("ana"));
        registro.adicionar(conexao);

        assertTrue(registro.remover(conexao));
        assertFalse(registro.remover(conexao));
        assertEquals(0, registro.quantidade());
    }

    @Test
    void snapshotEListaDeNomesSaoImutaveis() throws Exception {
        ClientRegistry registro = new ClientRegistry();
        registro.adicionar(new FakeConexao(new Usuario("ana")));
        List<ConexaoCliente> snapshot = registro.snapshot();

        assertEquals(List.of("ana"), registro.usuariosOnline());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
    }

    @Test
    void adicaoConcorrenteDoMesmoNomeEAtomica() throws Exception {
        ClientRegistry registro = new ClientRegistry();
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primeira = executor.submit(() -> {
                inicio.await();
                return registro.adicionar(new FakeConexao(usuario("ana")));
            });
            Future<Boolean> segunda = executor.submit(() -> {
                inicio.await();
                return registro.adicionar(new FakeConexao(usuario("ANA")));
            });
            inicio.countDown();

            assertNotEquals(primeira.get(), segunda.get());
            assertEquals(1, registro.quantidade());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Usuario usuario(String nome) {
        try {
            return new Usuario(nome);
        } catch (UsuarioInvalidoException excecao) {
            throw new AssertionError(excecao);
        }
    }
}
