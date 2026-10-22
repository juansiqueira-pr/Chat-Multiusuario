package br.unicesumar.chat.infraestrutura.ia;

import br.unicesumar.chat.service.ModeradorDeMensagens;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Classifica mensagens pela política configurada usando Groq Safeguard. */
public final class ModeradorGroq implements ModeradorDeMensagens {
    private static final String INSTRUCAO_SISTEMA = "Você é um moderador de mensagens. Marque como ofensiva "
            + "qualquer mensagem que contenha xingamento, ameaça, discurso de ódio, assédio ou conteúdo sexual "
            + "explícito. Caso contrário, marque como não ofensiva. Responda com exatamente uma das expressões "
            + "'ofensiva' ou 'não ofensiva', sem explicações, pontuação ou formatação.";
    private static final Pattern CONTEUDO = Pattern.compile(
            "\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");

    private final ClienteGroq cliente;
    private final String modelo;
    private final String chaveApi;

    /** Cria o moderador com o modelo e a chave carregados pela configuração central. */
    public ModeradorGroq(ClienteGroq cliente, ConfiguracaoIA configuracao) {
        this(cliente, configuracao.getModeloModeracao(), configuracao.getChaveGroq());
    }

    ModeradorGroq(ClienteGroq cliente, String modelo, String chaveApi) {
        this.cliente = Objects.requireNonNull(cliente);
        this.modelo = Objects.requireNonNull(modelo);
        this.chaveApi = Objects.requireNonNull(chaveApi);
    }

    /** Consulta Safeguard e aceita somente a classificação prevista pela política. */
    @Override
    public boolean ehOfensiva(String texto) {
        String resposta = cliente.completar(modelo, INSTRUCAO_SISTEMA, texto, chaveApi);
        String classificacao = extrairClassificacao(resposta);
        return switch (classificacao.toLowerCase(Locale.ROOT)) {
            case "ofensiva" -> true;
            case "não ofensiva" -> false;
            default -> throw new FalhaInferenciaException(
                    "Resposta de moderação fora do formato esperado.", null);
        };
    }

    private static String extrairClassificacao(String respostaJson) {
        if (respostaJson == null || respostaJson.isBlank()) {
            throw new FalhaInferenciaException("Resposta de moderação vazia.", null);
        }
        Matcher matcher = CONTEUDO.matcher(respostaJson);
        if (!matcher.find()) throw new FalhaInferenciaException(
                "Resposta de moderação sem o campo content.", null);
        return matcher.group(1).replace("\\n", "\n").replace("\\r", "\r")
                .replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\").trim();
    }
}
