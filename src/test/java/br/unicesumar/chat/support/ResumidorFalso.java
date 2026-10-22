package br.unicesumar.chat.support;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.service.ResumidorDeConversa;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Resumidor configurável para testes sem chamadas de rede. */
public final class ResumidorFalso implements ResumidorDeConversa {
    private final String resposta;
    private final AtomicInteger chamadas = new AtomicInteger();
    private volatile List<Mensagem> mensagensRecebidas = List.of();

    /** Cria um resumidor de teste com a resposta que deve devolver. */
    public ResumidorFalso(String resposta) {
        this.resposta = resposta;
    }

    /** Conta a chamada, guarda uma cópia das mensagens e devolve a resposta configurada. */
    @Override
    public String resumir(List<Mensagem> mensagens) {
        chamadas.incrementAndGet();
        mensagensRecebidas = List.copyOf(mensagens);
        return resposta;
    }

    /** Retorna quantas vezes o modelo falso foi consultado. */
    public int chamadas() {
        return chamadas.get();
    }

    /** Retorna a última cópia recebida pelo resumidor falso. */
    public List<Mensagem> mensagensRecebidas() {
        return mensagensRecebidas;
    }
}
