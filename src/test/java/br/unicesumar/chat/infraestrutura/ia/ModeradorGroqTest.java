package br.unicesumar.chat.infraestrutura.ia;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeradorGroqTest {
    @Test
    void classificaSomenteAsDuasRespostasPermitidas() throws Exception {
        HttpServer servidor = criarServidor("ofensiva");
        try {
            ModeradorGroq moderador = moderador(servidor);

            assertTrue(moderador.ehOfensiva("mensagem de teste"));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void respostaForaDoFormatoGeraFalhaDeInferencia() throws Exception {
        HttpServer servidor = criarServidor("ofensiva porque contém ameaça");
        try {
            ModeradorGroq moderador = moderador(servidor);

            assertThrows(FalhaInferenciaException.class,
                    () -> moderador.ehOfensiva("mensagem de teste"));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void classificaRespostaNaoOfensivaComoFalsa() throws Exception {
        HttpServer servidor = criarServidor("não ofensiva");
        try {
            assertFalse(moderador(servidor).ehOfensiva("mensagem de teste"));
        } finally {
            servidor.stop(0);
        }
    }

    private HttpServer criarServidor(String classificacao) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String resposta = "{\"choices\":[{\"message\":{\"content\":\"" + classificacao + "\"}}]}";
        servidor.createContext("/", requisicao -> responder(requisicao, resposta));
        servidor.start();
        return servidor;
    }

    private void responder(HttpExchange requisicao, String resposta) throws IOException {
        try (var entrada = requisicao.getRequestBody()) {
            entrada.readAllBytes();
        }
        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        requisicao.sendResponseHeaders(200, bytes.length);
        try (var saida = requisicao.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private ModeradorGroq moderador(HttpServer servidor) {
        URI endpoint = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/");
        return new ModeradorGroq(new ClienteGroq(endpoint), "openai/gpt-oss-safeguard-20b", "gsk_teste");
    }
}
