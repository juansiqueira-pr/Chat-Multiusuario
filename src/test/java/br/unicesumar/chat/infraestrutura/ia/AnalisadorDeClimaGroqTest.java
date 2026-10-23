package br.unicesumar.chat.infraestrutura.ia;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.model.NivelClima;
import br.unicesumar.chat.model.Usuario;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalisadorDeClimaGroqTest {
    @Test
    void classificaClimaPelaCategoriaRetornadaPeloModelo() throws Exception {
        HttpServer servidor = criarServidor("CALMO", new AtomicReference<>());
        try {
            assertEquals(NivelClima.CALMO, analisador(servidor).analisar(mensagens()));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void rejeitaFormatoQueNaoSejaUmNomeDeCategoriaExato() throws Exception {
        HttpServer servidor = criarServidor("O clima parece calmo.", new AtomicReference<>());
        try {
            assertThrows(FalhaInferenciaException.class,
                    () -> analisador(servidor).analisar(mensagens()));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void promptPedeSomenteUmaCategoriaDefinida() throws Exception {
        AtomicReference<String> corpo = new AtomicReference<>();
        HttpServer servidor = criarServidor("NEUTRO", corpo);
        try {
            analisador(servidor).analisar(mensagens());
            assertTrue(corpo.get().contains("CALMO, NEUTRO ou TENSO"));
            assertTrue(corpo.get().toLowerCase(java.util.Locale.ROOT)
                    .contains("responda apenas o nome da categoria"));
        } finally {
            servidor.stop(0);
        }
    }

    private static List<Mensagem> mensagens() throws Exception {
        return List.of(new Mensagem(1L, new Usuario("ana"), "Boa noite, pessoal.",
                LocalDateTime.of(2026, 10, 22, 20, 0)));
    }

    private static HttpServer criarServidor(String clima, AtomicReference<String> corpo) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String resposta = "{\"choices\":[{\"message\":{\"content\":\"" + clima + "\"}}]}";
        servidor.createContext("/", requisicao -> responder(requisicao, resposta, corpo));
        servidor.start();
        return servidor;
    }

    private static void responder(HttpExchange requisicao, String resposta,
                                  AtomicReference<String> corpo) throws IOException {
        try (var entrada = requisicao.getRequestBody()) {
            corpo.set(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        requisicao.sendResponseHeaders(200, bytes.length);
        try (var saida = requisicao.getResponseBody()) { saida.write(bytes); }
    }

    private static AnalisadorDeClimaGroq analisador(HttpServer servidor) {
        URI endpoint = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/");
        return new AnalisadorDeClimaGroq(new ClienteGroq(endpoint), "openai/gpt-oss-20b", "gsk_teste");
    }
}
