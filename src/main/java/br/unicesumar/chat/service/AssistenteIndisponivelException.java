package br.unicesumar.chat.service;

/** Indica que o assistente técnico não conseguiu responder à consulta. */
public final class AssistenteIndisponivelException extends RuntimeException {
    /** Cria falha do assistente com a causa técnica original. */
    public AssistenteIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
