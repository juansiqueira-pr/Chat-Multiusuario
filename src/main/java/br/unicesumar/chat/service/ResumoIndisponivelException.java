package br.unicesumar.chat.service;

/** Indica que o resumo solicitado não pôde ser produzido. */
public final class ResumoIndisponivelException extends RuntimeException {
    /** Cria falha de resumo com uma descrição segura para o fluxo do chat. */
    public ResumoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
