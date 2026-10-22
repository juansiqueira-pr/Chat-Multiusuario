package br.unicesumar.chat.infraestrutura.ia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Executa manualmente chamadas externas aos quatro serviços de IA via Groq. */
public final class ConectividadeManualGroq {
    private static final Pattern CONTEUDO = Pattern.compile("\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");

    private ConectividadeManualGroq() { }

    /** Chama os quatro usos previstos e imprime sucesso ou a falha recebida. */
    public static void main(String[] args) {
        ConfiguracaoIA configuracao;
        try {
            configuracao = new ConfiguracaoIA();
        } catch (IllegalStateException excecao) {
            System.err.println(excecao.getMessage());
            return;
        }

        ClienteGroq cliente = new ClienteGroq();
        testar(cliente, configuracao, "Moderação", configuracao.getModeloModeracao(),
                "Classifique conforme esta política: conteúdo com xingamento, ameaça, discurso de ódio ou assédio "
                        + "é ofensivo. Responda somente 'ofensiva' ou 'não ofensiva'.",
                "I appreciate your help with this project.", "não ofensiva", "ofensiva");
        testar(cliente, configuracao, "Resumo", configuracao.getModeloResumo(),
                "Resuma em português, em uma frase curta, as mensagens fornecidas.",
                "Ana: enviei o trabalho. Bruno: confirmei o recebimento.");
        testar(cliente, configuracao, "Termômetro", configuracao.getModeloClima(),
                "Classifique o clima da conversa com exatamente uma palavra: calmo, neutro ou tenso.",
                "Gostei da ideia. Vamos conversar com calma e resolver juntos.", "calmo", "neutro", "tenso");
        testar(cliente, configuracao, "Modo hacker", configuracao.getModeloHacker(),
                "Ajude somente em contexto educativo e autorizado. Nunca instrua ataques reais, invasão, "
                        + "roubo de credenciais ou dano. Se o pedido não for autorizado, recuse e ofereça "
                        + "uma alternativa segura.",
                "Explique em alto nível como proteger uma aplicação contra SQL injection.");
    }

    private static void testar(ClienteGroq cliente, ConfiguracaoIA configuracao, String servico,
                               String modelo, String sistema, String entrada, String... respostasPermitidas) {
        try {
            String resposta = cliente.completar(modelo, sistema, entrada, configuracao.getChaveGroq());
            String conteudo = extrairConteudo(resposta);
            if (conteudo == null || conteudo.isBlank()) {
                System.out.println("ERRO - " + servico + " (" + modelo + "): resposta sem conteúdo.");
            } else if (!permitida(conteudo, respostasPermitidas)) {
                System.out.println("ERRO - " + servico + " (" + modelo + "): resposta fora do formato esperado: "
                        + resumir(conteudo));
            } else {
                System.out.println("OK - " + servico + " (" + modelo + "): " + resumir(conteudo));
            }
        } catch (FalhaInferenciaException excecao) {
            String status = excecao.getStatusHttp() < 0 ? "sem resposta HTTP"
                    : "HTTP " + excecao.getStatusHttp();
            System.out.println("ERRO - " + servico + " (" + modelo + ", " + status + "): "
                    + excecao.getMessage());
        }
    }

    private static String extrairConteudo(String respostaJson) {
        Matcher matcher = CONTEUDO.matcher(respostaJson);
        if (!matcher.find()) return null;
        return matcher.group(1).replace("\\n", "\n").replace("\\r", "\r")
                .replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static boolean permitida(String conteudo, String[] permitidas) {
        if (permitidas.length == 0) return true;
        for (String permitida : permitidas) {
            if (permitida.equalsIgnoreCase(conteudo.trim())) return true;
        }
        return false;
    }

    private static String resumir(String resposta) {
        String umaLinha = resposta.replaceAll("\\s+", " ").trim();
        return umaLinha.length() <= 500 ? umaLinha : umaLinha.substring(0, 500) + "…";
    }
}
