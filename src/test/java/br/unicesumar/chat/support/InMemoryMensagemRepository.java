package br.unicesumar.chat.support;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.model.MensagemInvalidaException;
import br.unicesumar.chat.persistence.MensagemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Repositório em memória para testes sem banco de dados. */
public final class InMemoryMensagemRepository implements MensagemRepository {
    private final AtomicLong proximoId = new AtomicLong();
    private final List<Mensagem> mensagens = new ArrayList<>();

    /** Persiste em memória com ID crescente e devolve uma cópia identificada. */
    @Override
    public synchronized Mensagem salvar(Mensagem mensagem) {
        try {
            Mensagem salva = mensagem.comId(proximoId.incrementAndGet());
            mensagens.add(salva);
            return salva;
        } catch (MensagemInvalidaException excecao) {
            throw new IllegalStateException("O repositório gerou um identificador inválido.", excecao);
        }
    }

    /** Retorna as últimas mensagens em ordem cronológica. */
    @Override
    public synchronized List<Mensagem> buscarUltimas(int n) {
        if (n <= 0) {
            return List.of();
        }
        int inicio = Math.max(0, mensagens.size() - n);
        return List.copyOf(mensagens.subList(inicio, mensagens.size()));
    }
}
