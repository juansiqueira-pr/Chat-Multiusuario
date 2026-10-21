package br.unicesumar.chat.infraestrutura.ia;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteHuggingFaceTest {
    @Test
    void enviaJsonComAutorizacaoEDevolveResposta() throws Exception {
        AtomicReference<String> autorizacao = new AtomicReference<>();
        AtomicReference<String> corpo = new AtomicReference<>();
        HttpServer servidor = criarServidor(200, "{\"resultado\":\"ok\"}", autorizacao, corpo);
        try {
            String resposta = new ClienteHuggingFace().inferir(url(servidor), "{\"texto\":\"olá\"}", "hf_teste");

            assertEquals("Bearer hf_teste", autorizacao.get());
            assertEquals("{\"texto\":\"olá\"}", corpo.get());
            assertEquals("{\"resultado\":\"ok\"}", resposta);
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void transformaRespostaHttpDeErroEmFalhaEspecifica() throws Exception {
        HttpServer servidor = criarServidor(401, "{\"error\":\"hf_teste inválido\"}",
                new AtomicReference<>(), new AtomicReference<>());
        try {
            FalhaInferenciaException excecao = assertThrows(FalhaInferenciaException.class,
                    () -> new ClienteHuggingFace().inferir(url(servidor), "{}", "hf_teste"));

            assertEquals(401, excecao.getStatusHttp());
            assertTrue(excecao.getMessage().contains("{\"error\":\"[REDACTED] inválido\"}"));
            assertFalse(excecao.getMessage().contains("hf_teste"));
        } finally {
            servidor.stop(0);
        }
    }

    private HttpServer criarServidor(int status, String resposta, AtomicReference<String> autorizacao,
                                     AtomicReference<String> corpo) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/inferir", requisicao -> responder(requisicao, status, resposta, autorizacao, corpo));
        servidor.start();
        return servidor;
    }

    private void responder(HttpExchange requisicao, int status, String resposta,
                           AtomicReference<String> autorizacao, AtomicReference<String> corpo) throws IOException {
        autorizacao.set(requisicao.getRequestHeaders().getFirst("Authorization"));
        try (var entrada = requisicao.getRequestBody()) {
            corpo.set(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        requisicao.sendResponseHeaders(status, bytes.length);
        try (var saida = requisicao.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private String url(HttpServer servidor) {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/inferir";
    }
}
