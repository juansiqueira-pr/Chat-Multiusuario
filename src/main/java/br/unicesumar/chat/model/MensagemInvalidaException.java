package br.unicesumar.chat.model;

/** Indica que uma mensagem não atende às regras de validação do chat. */
public class MensagemInvalidaException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Cria a exceção com o motivo da validação. */
    public MensagemInvalidaException(String mensagem) {
        super(mensagem);
    }
}
