package br.unicesumar.chat.infraestrutura.ia;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Carrega o token Hugging Face e os modelos definidos em chat.properties. */
public final class ConfiguracaoIA {
    private static final String MENSAGEM_TOKEN_AUSENTE = "Configuração da IA ausente: defina HF_TOKEN no arquivo "
            + ".env na raiz do projeto (ex.: HF_TOKEN=seu_token).";

    private final String token;
    private final String urlClassificacao;
    private final String urlChat;
    private final String modeloModeracao;
    private final String modeloResumo;
    private final String modeloClima;
    private final String modeloHacker;

    /** Carrega HF_TOKEN pelo Dotenv e os modelos pelo recurso chat.properties. */
    public ConfiguracaoIA() {
        this.token = validarToken(carregarToken());
        Properties propriedades = carregarPropriedades();
        urlClassificacao = propriedade(propriedades, "ia.url.classificacao");
        urlChat = propriedade(propriedades, "ia.url.chat");
        modeloModeracao = propriedade(propriedades, "ia.modelo.moderacao");
        modeloResumo = propriedade(propriedades, "ia.modelo.resumo");
        modeloClima = propriedade(propriedades, "ia.modelo.clima");
        modeloHacker = propriedade(propriedades, "ia.modelo.hacker");
    }

    /** Retorna o token carregado sem registrá-lo ou formatá-lo em logs. */
    public String getToken() { return token; }

    /** Retorna a URL de classificação para o modelo de moderação. */
    public String getUrlModeracao() { return urlClassificacao + modeloModeracao; }

    /** Retorna a URL de chat completions usada pelo modelo de resumo. */
    public String getUrlResumo() { return urlChat; }

    /** Retorna a URL de classificação para o modelo de sentimento. */
    public String getUrlClima() { return urlClassificacao + modeloClima; }

    /** Retorna a URL de chat completions usada pelo modelo hacker. */
    public String getUrlHacker() { return urlChat; }

    /** Retorna o identificador do modelo de moderação. */
    public String getModeloModeracao() { return modeloModeracao; }

    /** Retorna o identificador do modelo de resumo. */
    public String getModeloResumo() { return modeloResumo; }

    /** Retorna o identificador do modelo de sentimento. */
    public String getModeloClima() { return modeloClima; }

    /** Retorna o identificador do modelo hacker. */
    public String getModeloHacker() { return modeloHacker; }

    static String validarToken(String valor) {
        if (valor == null || valor.isBlank()) throw new IllegalStateException(MENSAGEM_TOKEN_AUSENTE);
        return valor.trim();
    }

    private static String carregarToken() {
        try {
            return Dotenv.load().get("HF_TOKEN");
        } catch (RuntimeException excecao) {
            throw new IllegalStateException(MENSAGEM_TOKEN_AUSENTE, excecao);
        }
    }

    private static Properties carregarPropriedades() {
        InputStream recurso = ConfiguracaoIA.class.getResourceAsStream("/chat.properties");
        if (recurso == null) throw new IllegalStateException("Não foi encontrado src/main/resources/chat.properties.");
        Properties propriedades = new Properties();
        try (InputStream entrada = recurso) {
            propriedades.load(entrada);
            return propriedades;
        } catch (IOException excecao) {
            throw new IllegalStateException("Não foi possível ler chat.properties.", excecao);
        }
    }

    private static String propriedade(Properties propriedades, String chave) {
        String valor = propriedades.getProperty(chave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Propriedade obrigatória ausente em chat.properties: " + chave);
        }
        return valor.trim();
    }
}
