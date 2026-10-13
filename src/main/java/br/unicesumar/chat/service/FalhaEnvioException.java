package br.unicesumar.chat.service;

/** Indica que uma linha não pôde ser entregue a uma conexão cliente. */
public class FalhaEnvioException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Cria a exceção com a causa da falha de envio. */
    public FalhaEnvioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    /** Cria a exceção com o motivo da falha de envio. */
    public FalhaEnvioException(String mensagem) {
        super(mensagem);
    }
}
