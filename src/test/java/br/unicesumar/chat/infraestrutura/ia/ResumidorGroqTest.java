package br.unicesumar.chat.infraestrutura.ia;

import br.unicesumar.chat.model.Mensagem;
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

class ResumidorGroqTest {
    @Test
    void extraiResumoDaRespostaChatCompletions() throws Exception {
        HttpServer servidor = criarServidor("A conversa cumprimentou a equipe.");
        try {
            assertEquals("A conversa cumprimentou a equipe.", resumidor(servidor).resumir(mensagens()));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void rejeitaRespostaSemContent() throws Exception {
        HttpServer servidor = criarServidor("");
        try {
            ResumidorGroq resumidor = resumidor(servidor);
            assertThrows(br.unicesumar.chat.service.ResumoIndisponivelException.class,
                    () -> resumidor.resumir(mensagens()));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void instruiModeloAResumirContextoGeralSemAtribuirFalas() throws Exception {
        AtomicReference<String> corpoRecebido = new AtomicReference<>();
        HttpServer servidor = criarServidor("A conversa tratou do trabalho.", corpoRecebido);
        try {
            resumidor(servidor).resumir(mensagens());

            assertTrue(corpoRecebido.get().contains("contexto geral da conversa"));
            assertTrue(corpoRecebido.get().contains("sem atribuir falas"));
            assertTrue(corpoRecebido.get().contains("Destaque os assuntos principais"));
        } finally {
            servidor.stop(0);
        }
    }

    private static List<Mensagem> mensagens() throws Exception {
        return List.of(new Mensagem(1L, new Usuario("ana"), "boa noite",
                LocalDateTime.of(2026, 10, 22, 20, 0)));
    }

    private static HttpServer criarServidor(String resumo) throws IOException {
        return criarServidor(resumo, new AtomicReference<>());
    }

    private static HttpServer criarServidor(String resumo, AtomicReference<String> corpoRecebido)
            throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String content = resumo.isEmpty() ? "" : "\"" + resumo + "\"";
        String resposta = "{\"choices\":[{\"message\":{" + (resumo.isEmpty() ? "" : "\"content\":" + content) + "}}]}";
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

    private static ResumidorGroq resumidor(HttpServer servidor) {
        URI endpoint = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/");
        return new ResumidorGroq(new ClienteGroq(endpoint), "openai/gpt-oss-20b", "gsk_teste");
    }
}
