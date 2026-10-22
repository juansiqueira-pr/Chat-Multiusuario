package br.unicesumar.chat.infraestrutura.ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/** Reutiliza um cliente HTTP para chamadas chat completions à API Groq. */
public final class ClienteGroq {
    private static final URI URL_CHAT_COMPLETIONS =
            URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final int LIMITE_RESPOSTA_ERRO = 1200;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final URI endpoint;

    /** Cria cliente conectado ao endpoint oficial do Groq. */
    public ClienteGroq() {
        this(URL_CHAT_COMPLETIONS);
    }

    ClienteGroq(URI endpoint) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint não pode ser null");
    }

    /** Envia uma conversa de sistema e usuário no padrão OpenAI chat completions. */
    public String completar(String modelo, String instrucaoSistema, String mensagemUsuario, String chaveApi) {
        Objects.requireNonNull(modelo, "modelo não pode ser null");
        Objects.requireNonNull(instrucaoSistema, "instrucaoSistema não pode ser null");
        Objects.requireNonNull(mensagemUsuario, "mensagemUsuario não pode ser null");
        if (chaveApi == null || chaveApi.isBlank()) {
            throw new FalhaInferenciaException("Chave GROQ_API_KEY não configurada.", null);
        }

        String corpoJson = montarCorpo(modelo, instrucaoSistema, mensagemUsuario);
        HttpRequest requisicao = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + chaveApi)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpoJson, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resposta = cliente.send(requisicao,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
                String corpoErro = limitar(resposta.body() == null ? "" : resposta.body()
                        .replace(chaveApi, "[REDACTED]"));
                String detalhe = resposta.statusCode() == 429
                        ? mensagemLimite(modelo, resposta)
                        : "Resposta HTTP " + resposta.statusCode() + " da API Groq.";
                throw new FalhaInferenciaException(resposta.statusCode(), detalhe + " " + corpoErro);
            }
            return resposta.body();
        } catch (FalhaInferenciaException excecao) {
            throw excecao;
        } catch (java.net.http.HttpTimeoutException excecao) {
            throw new FalhaInferenciaException("Tempo limite de 90 s excedido na API Groq.", excecao);
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new FalhaInferenciaException("Chamada à API Groq interrompida.", excecao);
        } catch (IOException excecao) {
            throw new FalhaInferenciaException("Falha de rede ao chamar a API Groq: "
                    + excecao.getMessage(), excecao);
        }
    }

    private static String montarCorpo(String modelo, String instrucaoSistema, String mensagemUsuario) {
        return "{\"model\":\"" + escapar(modelo) + "\",\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapar(instrucaoSistema) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapar(mensagemUsuario) + "\"}],"
                + "\"max_completion_tokens\":256,\"temperature\":0,\"stream\":false}";
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String mensagemLimite(String modelo, HttpResponse<?> resposta) {
        String limite = "openai/gpt-oss-safeguard-20b".equals(modelo)
                ? "O modelo de moderação tem limite documentado de 3 requisições por minuto"
                : "O limite de requisições ou tokens do modelo/organização foi atingido";
        String aguardar = resposta.headers().firstValue("retry-after")
                .map(valor -> "; Retry-After=" + valor + " s")
                .orElse("");
        return "HTTP 429: " + limite + aguardar + ". A chamada não será repetida automaticamente.";
    }

    private static String limitar(String corpo) {
        return corpo.length() <= LIMITE_RESPOSTA_ERRO
                ? corpo : corpo.substring(0, LIMITE_RESPOSTA_ERRO) + "…";
    }
}
