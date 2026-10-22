package br.unicesumar.chat.service;

/** Define se uma mensagem deve ser bloqueada antes de ser publicada no chat. */
@FunctionalInterface
public interface ModeradorDeMensagens {
    /** Retorna true quando o texto é ofensivo. */
    boolean ehOfensiva(String texto);
}
