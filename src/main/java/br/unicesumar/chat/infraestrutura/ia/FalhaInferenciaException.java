package br.unicesumar.chat.infraestrutura.ia;

/** Indica falha de rede, timeout ou resposta HTTP inválida na inferência. */
public final class FalhaInferenciaException extends RuntimeException {
    private final int statusHttp;

    /** Cria falha sem status HTTP, como erro de conexão ou timeout. */
    public FalhaInferenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
        statusHttp = -1;
    }

    /** Cria falha para uma resposta HTTP fora da faixa de sucesso. */
    public FalhaInferenciaException(int statusHttp, String mensagem) {
        super(mensagem);
        this.statusHttp = statusHttp;
    }

    /** Retorna o código HTTP ou -1 quando a chamada não recebeu resposta. */
    public int getStatusHttp() { return statusHttp; }
}
