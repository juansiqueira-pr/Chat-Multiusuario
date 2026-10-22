package br.unicesumar.chat.server;

import java.util.Objects;

/** Classifica linhas recebidas conforme o protocolo textual do chat. */
public final class Protocolo {
    /** Tipos de linha aceitos pelo protocolo. */
    public enum Tipo {
        MENSAGEM,
        SAIR,
        RESUMIR_HISTORICO,
        CONSULTAR_HACKER,
        COMANDO_DESCONHECIDO
    }

    /** Resultado imutável da interpretação de uma linha. */
    public record Resultado(Tipo tipo, String conteudo) {
    }

    /** Interpreta texto comum, o comando /quit ou outro comando iniciado por barra. */
    public Resultado interpretar(String linha) {
        Objects.requireNonNull(linha, "linha");
        if ("/quit".equals(linha)) {
            return new Resultado(Tipo.SAIR, linha);
        }
        if ("/historico".equalsIgnoreCase(linha) || "/histórico".equalsIgnoreCase(linha)) {
            return new Resultado(Tipo.RESUMIR_HISTORICO, linha);
        }
        if (linha.equalsIgnoreCase("/hacker") || linha.regionMatches(true, 0, "/hacker ", 0, 8)) {
            String pergunta = linha.length() <= 8 ? "" : linha.substring(8).trim();
            return new Resultado(Tipo.CONSULTAR_HACKER, pergunta);
        }
        if (linha.startsWith("/")) {
            return new Resultado(Tipo.COMANDO_DESCONHECIDO, linha);
        }
        return new Resultado(Tipo.MENSAGEM, linha);
    }
}
