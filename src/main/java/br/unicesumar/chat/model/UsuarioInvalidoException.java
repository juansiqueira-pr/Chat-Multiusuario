package br.unicesumar.chat.model;

/** Indica que um nome de usuário não atende às regras do chat. */
public class UsuarioInvalidoException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Cria a exceção com o motivo da validação. */
    public UsuarioInvalidoException(String mensagem) {
        super(mensagem);
    }
}
