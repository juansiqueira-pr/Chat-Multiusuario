package br.unicesumar.chat.server;

import java.util.Objects;

/** Classifica linhas recebidas conforme o protocolo textual do chat. */
public final class Protocolo {
    /** Tipos de linha aceitos pelo protocolo. */
    public enum Tipo {
        MENSAGEM,
        SAIR,
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
        if (linha.startsWith("/")) {
            return new Resultado(Tipo.COMANDO_DESCONHECIDO, linha);
        }
        return new Resultado(Tipo.MENSAGEM, linha);
    }
}
