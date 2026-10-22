package br.unicesumar.chat.infraestrutura.ia;

import br.unicesumar.chat.service.AssistenteIndisponivelException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistenteTecnicoGroqTest {
    @Test
    void enviaPerguntaComPoliticaEticaEExtraiResposta() throws Exception {
        AtomicReference<String> corpoRecebido = new AtomicReference<>();
        HttpServer servidor = criarServidor("Use autenticação forte e atualize o servidor.", corpoRecebido);
        try {
            AssistenteTecnicoGroq assistente = assistente(servidor);

            assertEquals("Use autenticação forte e atualize o servidor.",
                    assistente.responder("como proteger meu servidor?"));
            assertTrue(corpoRecebido.get().contains("openai/gpt-oss-20b"));
            assertTrue(corpoRecebido.get().contains("contexto educativo e autorizado"));
            assertTrue(corpoRecebido.get().contains("como proteger meu servidor?"));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void respostaSemConteudoViraFalhaControlada() throws Exception {
        HttpServer servidor = criarServidor("", new AtomicReference<>());
        try {
            assertThrows(AssistenteIndisponivelException.class,
                    () -> assistente(servidor).responder("pergunta de teste"));
        } finally {
            servidor.stop(0);
        }
    }

    private static HttpServer criarServidor(String texto, AtomicReference<String> corpoRecebido) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String content = texto.isEmpty() ? "" : "\"content\":\"" + texto + "\"";
        String resposta = "{\"choices\":[{\"message\":{" + content + "}}]}";
        servidor.createContext("/", requisicao -> responder(requisicao, resposta, corpoRecebido));
        servidor.start();
        return servidor;
    }

    private static void responder(HttpExchange requisicao, String resposta,
                                  AtomicReference<String> corpoRecebido) throws IOException {
        try (var entrada = requisicao.getRequestBody()) {
            corpoRecebido.set(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        requisicao.sendResponseHeaders(200, bytes.length);
        try (var saida = requisicao.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private static AssistenteTecnicoGroq assistente(HttpServer servidor) {
        URI endpoint = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/");
        return new AssistenteTecnicoGroq(new ClienteGroq(endpoint), "openai/gpt-oss-20b", "gsk_teste");
    }
}
