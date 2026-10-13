package br.unicesumar.chat.integracao;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.model.MensagemInvalidaException;
import br.unicesumar.chat.model.Usuario;
import br.unicesumar.chat.model.UsuarioInvalidoException;
import br.unicesumar.chat.persistence.jdbc.ConnectionFactory;
import br.unicesumar.chat.persistence.jdbc.InicializadorSchema;
import br.unicesumar.chat.persistence.jdbc.JdbcMensagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcMensagemRepositoryIntegracaoTest {
    private ConnectionFactory conexoes;
    private JdbcMensagemRepository repositorio;

    @BeforeEach
    void prepararBanco() throws Exception {
        String nomeUnico = "chat_" + UUID.randomUUID().toString().replace("-", "");
        conexoes = new ConnectionFactory("jdbc:h2:mem:" + nomeUnico + ";DB_CLOSE_DELAY=-1", "sa", "");
        InicializadorSchema inicializador = new InicializadorSchema(conexoes);
        inicializador.inicializar();
        inicializador.inicializar();
        try (Connection conexao = conexoes.abrir(); Statement comando = conexao.createStatement()) {
            comando.executeUpdate("DELETE FROM mensagem");
        }
        repositorio = new JdbcMensagemRepository(conexoes);
    }

    @Test
    void salvaERecuperaAsMaisRecentesEmOrdemCronologica() throws Exception {
        Mensagem primeira = repositorio.salvar(mensagem("primeira", 1));
        repositorio.salvar(mensagem("segunda", 2));
        repositorio.salvar(mensagem("terceira", 3));

        List<Mensagem> ultimas = repositorio.buscarUltimas(2);

        assertTrue(primeira.getId() > 0);
        assertEquals(List.of("segunda", "terceira"),
                ultimas.stream().map(Mensagem::getConteudo).toList());
        assertTrue(ultimas.get(0).getId() < ultimas.get(1).getId());
    }

    @Test
    void bancoVazioOuLimiteNaoPositivoRetornaListaVazia() {
        assertTrue(repositorio.buscarUltimas(20).isEmpty());
        assertTrue(repositorio.buscarUltimas(0).isEmpty());
        assertTrue(repositorio.buscarUltimas(-1).isEmpty());
    }

    private static Mensagem mensagem(String texto, int minuto)
            throws UsuarioInvalidoException, MensagemInvalidaException {
        return new Mensagem(null, new Usuario("ana"), texto,
                LocalDateTime.of(2026, 10, 13, 9, minuto));
    }
}
