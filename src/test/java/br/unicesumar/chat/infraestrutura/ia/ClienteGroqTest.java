package br.unicesumar.chat.infraestrutura.ia;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteGroqTest {
    @Test
    void enviaChatCompletionsComModeloMensagensEAutorizacao() throws Exception {
        AtomicReference<String> autorizacao = new AtomicReference<>();
        AtomicReference<String> caminho = new AtomicReference<>();
        AtomicReference<String> corpo = new AtomicReference<>();
        HttpServer servidor = criarServidor(200, "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}",
                autorizacao, caminho, corpo);
        try {
            String resposta = cliente(servidor).completar("openai/gpt-oss-20b", "seja breve",
                    "resuma isto", "gsk_teste");

            assertEquals("Bearer gsk_teste", autorizacao.get());
            assertEquals("/openai/v1/chat/completions", caminho.get());
            assertTrue(corpo.get().contains("\"model\":\"openai/gpt-oss-20b\""));
            assertTrue(corpo.get().contains("\"role\":\"system\""));
            assertTrue(corpo.get().contains("\"role\":\"user\""));
            assertEquals("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}", resposta);
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void transformaLimite429EmFalhaSemExporChave() throws Exception {
        HttpServer servidor = criarServidor(429, "gsk_teste: rate limit", new AtomicReference<>(),
                new AtomicReference<>(), new AtomicReference<>());
        try {
            FalhaInferenciaException excecao = assertThrows(FalhaInferenciaException.class,
                    () -> cliente(servidor).completar("openai/gpt-oss-safeguard-20b", "política", "texto",
                            "gsk_teste"));

            assertEquals(429, excecao.getStatusHttp());
            assertTrue(excecao.getMessage().contains("limite"));
            assertFalse(excecao.getMessage().contains("gsk_teste"));
        } finally {
            servidor.stop(0);
        }
    }

    private HttpServer criarServidor(int status, String resposta, AtomicReference<String> autorizacao,
                                     AtomicReference<String> caminho, AtomicReference<String> corpo)
            throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", requisicao -> responder(requisicao, status, resposta, autorizacao,
                caminho, corpo));
        servidor.start();
        return servidor;
    }

    private void responder(HttpExchange requisicao, int status, String resposta,
                           AtomicReference<String> autorizacao, AtomicReference<String> caminho,
                           AtomicReference<String> corpo) throws IOException {
        autorizacao.set(requisicao.getRequestHeaders().getFirst("Authorization"));
        caminho.set(requisicao.getRequestURI().getPath());
        try (var entrada = requisicao.getRequestBody()) {
            corpo.set(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        requisicao.sendResponseHeaders(status, bytes.length);
        try (var saida = requisicao.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private ClienteGroq cliente(HttpServer servidor) {
        return new ClienteGroq(URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()
                + "/openai/v1/chat/completions"));
    }
}
