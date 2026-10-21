package br.unicesumar.chat.infraestrutura.ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/** Reutiliza um cliente HTTP para chamadas à API de inferência do Hugging Face. */
public final class ClienteHuggingFace {
    private static final int LIMITE_RESPOSTA_ERRO = 1200;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Envia JSON para uma URL de modelo usando o token fornecido. */
    public String inferir(String urlModelo, String corpoJson, String token) {
        Objects.requireNonNull(urlModelo, "urlModelo não pode ser null");
        Objects.requireNonNull(corpoJson, "corpoJson não pode ser null");
        if (token == null || token.isBlank()) {
            throw new FalhaInferenciaException("Token de inferência não configurado.", null);
        }

        HttpRequest requisicao;
        try {
            requisicao = HttpRequest.newBuilder(URI.create(urlModelo))
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoJson, StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException excecao) {
            throw new FalhaInferenciaException("URL de inferência inválida.", excecao);
        }

        try {
            HttpResponse<String> resposta = cliente.send(requisicao,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
                String corpoErro = resposta.body() == null ? "" : resposta.body().replace(token, "[REDACTED]");
                if (corpoErro.length() > LIMITE_RESPOSTA_ERRO) {
                    corpoErro = corpoErro.substring(0, LIMITE_RESPOSTA_ERRO) + "…";
                }
                throw new FalhaInferenciaException(resposta.statusCode(),
                        "Resposta HTTP " + resposta.statusCode() + " da API Hugging Face: " + corpoErro);
            }
            return resposta.body();
        } catch (FalhaInferenciaException excecao) {
            throw excecao;
        } catch (java.net.http.HttpTimeoutException excecao) {
            throw new FalhaInferenciaException("Tempo limite de 90 s excedido na API Hugging Face.", excecao);
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new FalhaInferenciaException("Chamada à API Hugging Face interrompida.", excecao);
        } catch (IOException excecao) {
            throw new FalhaInferenciaException("Falha de rede ao chamar a API Hugging Face: "
                    + excecao.getMessage(), excecao);
        }
    }
}
