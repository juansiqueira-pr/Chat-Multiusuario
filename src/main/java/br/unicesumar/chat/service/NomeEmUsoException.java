package br.unicesumar.chat.service;

/** Indica que outro cliente já está conectado com o mesmo nome. */
public class NomeEmUsoException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Cria a exceção com a mensagem de recusa da conexão. */
    public NomeEmUsoException(String mensagem) {
        super(mensagem);
    }
}
