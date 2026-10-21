package br.unicesumar.chat.infraestrutura.ia;

/** Executa manualmente chamadas externas aos quatro modelos configurados. */
public final class ConectividadeManualHuggingFace {
    private ConectividadeManualHuggingFace() { }

    /** Chama os modelos configurados e imprime sucesso ou a falha recebida. */
    public static void main(String[] args) {
        ConfiguracaoIA configuracao;
        try {
            configuracao = new ConfiguracaoIA();
        } catch (IllegalStateException excecao) {
            System.err.println(excecao.getMessage());
            return;
        }

        ClienteHuggingFace cliente = new ClienteHuggingFace();
        testar(cliente, configuracao, "Moderação", configuracao.getModeloModeracao(),
                configuracao.getUrlModeracao(), "{\"inputs\":\"I appreciate your help with this project.\"}");
        testar(cliente, configuracao, "Resumo", configuracao.getModeloResumo(), configuracao.getUrlResumo(),
                corpoChat(configuracao.getModeloResumo(), "Resuma em uma frase: Ana envia o trabalho; Bruno confirma o recebimento."));
        testar(cliente, configuracao, "Termômetro", configuracao.getModeloClima(), configuracao.getUrlClima(),
                "{\"inputs\":\"I am delighted with this wonderful result.\"}");
        testar(cliente, configuracao, "Modo hacker", configuracao.getModeloHacker(), configuracao.getUrlHacker(),
                corpoChat(configuracao.getModeloHacker(), "Responda apenas: conexão aprovada."));
    }

    private static void testar(ClienteHuggingFace cliente, ConfiguracaoIA configuracao,
                               String recurso, String modelo, String url, String corpo) {
        try {
            String resposta = cliente.inferir(url, corpo, configuracao.getToken());
            if (resposta == null || resposta.isBlank()) {
                System.out.println("ERRO - " + recurso + " (" + modelo + "): resposta vazia.");
                return;
            }
            System.out.println("OK - " + recurso + " (" + modelo + "): " + resumir(resposta));
        } catch (FalhaInferenciaException excecao) {
            String status = excecao.getStatusHttp() < 0 ? "sem resposta HTTP"
                    : "HTTP " + excecao.getStatusHttp();
            System.out.println("ERRO - " + recurso + " (" + modelo + ", " + status + "): "
                    + excecao.getMessage());
        }
    }

    private static String corpoChat(String modelo, String texto) {
        return "{\"model\":\"" + escapar(modelo) + "\",\"messages\":[{\"role\":\"user\",\"content\":\""
                + escapar(texto) + "\"}],\"max_tokens\":80,\"stream\":false}";
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String resumir(String resposta) {
        String umaLinha = resposta.replaceAll("\\s+", " ").trim();
        return umaLinha.length() <= 500 ? umaLinha : umaLinha.substring(0, 500) + "…";
    }
}
