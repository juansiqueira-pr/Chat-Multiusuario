package br.unicesumar.chat.persistence;

/** Indica uma falha ao acessar ou atualizar a persistência do chat. */
public class PersistenciaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Cria a exceção com uma mensagem e a causa original. */
    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
