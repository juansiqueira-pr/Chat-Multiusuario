package br.unicesumar.chat.infraestrutura.ia;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Carrega a chave Groq e os modelos definidos em chat.properties. */
public final class ConfiguracaoIA {
    private static final String MENSAGEM_CHAVE_AUSENTE = "Configuração da IA ausente: defina GROQ_API_KEY no arquivo "
            + ".env na raiz do projeto (ex.: GROQ_API_KEY=sua_chave).";

    private final String chaveGroq;
    private final String modeloModeracao;
    private final String modeloResumo;
    private final String modeloClima;
    private final String modeloHacker;

    /** Carrega GROQ_API_KEY pelo Dotenv e os modelos pelo recurso chat.properties. */
    public ConfiguracaoIA() {
        this.chaveGroq = validarChaveGroq(carregarChaveGroq());
        Properties propriedades = carregarPropriedades();
        modeloModeracao = propriedade(propriedades, "ia.modelo.moderacao");
        modeloResumo = propriedade(propriedades, "ia.modelo.resumo");
        modeloClima = propriedade(propriedades, "ia.modelo.clima");
        modeloHacker = propriedade(propriedades, "ia.modelo.hacker");
    }

    /** Retorna a chave Groq sem registrá-la ou formatá-la em logs. */
    public String getChaveGroq() { return chaveGroq; }

    /** Retorna o identificador do modelo de moderação. */
    public String getModeloModeracao() { return modeloModeracao; }

    /** Retorna o identificador do modelo de resumo. */
    public String getModeloResumo() { return modeloResumo; }

    /** Retorna o identificador do modelo de sentimento. */
    public String getModeloClima() { return modeloClima; }

    /** Retorna o identificador do modelo hacker. */
    public String getModeloHacker() { return modeloHacker; }

    static String validarChaveGroq(String valor) {
        if (valor == null || valor.isBlank()) throw new IllegalStateException(MENSAGEM_CHAVE_AUSENTE);
        return valor.trim();
    }

    private static String carregarChaveGroq() {
        try {
            return Dotenv.load().get("GROQ_API_KEY");
        } catch (RuntimeException excecao) {
            throw new IllegalStateException(MENSAGEM_CHAVE_AUSENTE, excecao);
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
