package br.unicesumar.chat.client.integracao;

import br.unicesumar.chat.client.ConexaoChat;
import br.unicesumar.chat.client.LinhaServidor;
import br.unicesumar.chat.client.OuvinteChat;
import br.unicesumar.chat.persistence.jdbc.ConnectionFactory;
import br.unicesumar.chat.persistence.jdbc.InicializadorSchema;
import br.unicesumar.chat.persistence.jdbc.JdbcMensagemRepository;
import br.unicesumar.chat.server.Servidor;
import br.unicesumar.chat.service.ChatService;
import br.unicesumar.chat.service.ClientRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConexaoChatIntegracaoTest {
    private Servidor servidor;
    private final BlockingQueue<Evento> eventos = new LinkedBlockingQueue<>();
    private final OuvinteChat ouvinte = new OuvinteChat() {
        @Override public void aoAceitar(String nome) { eventos.add(new Evento("aceite", nome)); }
        @Override public void aoReceber(LinhaServidor linha) { eventos.add(new Evento("linha", linha.original())); }
        @Override public void aoRejeitar(String motivo) { eventos.add(new Evento("rejeicao", motivo)); }
        @Override public void aoDesconectar(String motivo) { eventos.add(new Evento("desconexao", motivo)); }
    };

    @BeforeEach
    void iniciarServidor() {
        ConnectionFactory conexoes = new ConnectionFactory(
                "jdbc:h2:mem:gui_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1", "sa", "");
        new InicializadorSchema(conexoes).inicializar();
        ChatService servico = new ChatService(new JdbcMensagemRepository(conexoes), new ClientRegistry(),
                Clock.fixed(Instant.parse("2026-10-13T12:30:00Z"), ZoneOffset.UTC), 20);
        servidor = new Servidor(0, servico);
        servidor.iniciar();
    }

    @AfterEach
    void pararServidor() {
        servidor.parar();
    }

    @Test
    void recebeAceiteEBroadcastPelaConexaoTcp() throws Exception {
        try (ConexaoChat conexao = new ConexaoChat("127.0.0.1", servidor.getPortaLocal(), "ana", ouvinte)) {
            conexao.conectar();

            assertEquals(new Evento("aceite", "ana"), aguardar("aceite"));
            conexao.enviar("olá");
            assertTrue(aguardarLinha("ana: olá").valor().contains("ana: olá"));
        }
    }

    @Test
    void nomeEmUsoERecusadoEFechamentoDoServidorNotificaDesconexao() throws Exception {
        try (ConexaoChat primeira = new ConexaoChat("127.0.0.1", servidor.getPortaLocal(), "ana", ouvinte)) {
            primeira.conectar();
            assertEquals(new Evento("aceite", "ana"), aguardar("aceite"));
            OuvinteChat recusada = novoOuvinte();
            try (ConexaoChat segunda = new ConexaoChat("127.0.0.1", servidor.getPortaLocal(), "ANA", recusada)) {
                segunda.conectar();
                assertEquals("rejeicao", aguardar("rejeicao").tipo());
            }
            servidor.parar();
            assertEquals("desconexao", aguardar("desconexao").tipo());
        }
    }

    @Test
    void cancelarAntesDeConectarImpedeAberturaDoSocket() {
        ConexaoChat conexao = new ConexaoChat("127.0.0.1", servidor.getPortaLocal(), "ana", ouvinte);

        conexao.desconectar();

        assertThrows(java.io.IOException.class, conexao::conectar);
        conexao.desconectar();
    }

    @Test
    void desconexaoLocalFechaLeituraENotificaUmaVez() throws Exception {
        try (ConexaoChat conexao = new ConexaoChat("127.0.0.1", servidor.getPortaLocal(), "ana", ouvinte)) {
            conexao.conectar();
            assertEquals(new Evento("aceite", "ana"), aguardar("aceite"));

            conexao.desconectar();

            assertEquals("desconexao", aguardar("desconexao").tipo());
        }
    }

    private OuvinteChat novoOuvinte() {
        return new OuvinteChat() {
            @Override public void aoAceitar(String nome) { eventos.add(new Evento("aceite", nome)); }
            @Override public void aoReceber(LinhaServidor linha) { eventos.add(new Evento("linha", linha.original())); }
            @Override public void aoRejeitar(String motivo) { eventos.add(new Evento("rejeicao", motivo)); }
            @Override public void aoDesconectar(String motivo) { eventos.add(new Evento("desconexao", motivo)); }
        };
    }

    private Evento aguardar(String tipo) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < limite) {
            Evento evento = eventos.poll(100, TimeUnit.MILLISECONDS);
            if (evento != null && evento.tipo().equals(tipo)) return evento;
        }
        throw new AssertionError("Tempo esgotado aguardando evento: " + tipo);
    }

    private Evento aguardarLinha(String trecho) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < limite) {
            Evento evento = eventos.poll(100, TimeUnit.MILLISECONDS);
            if (evento != null && evento.tipo().equals("linha") && evento.valor().contains(trecho)) return evento;
        }
        throw new AssertionError("Tempo esgotado aguardando linha com: " + trecho);
    }

    private record Evento(String tipo, String valor) { }
}
