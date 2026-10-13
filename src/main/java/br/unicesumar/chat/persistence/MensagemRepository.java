package br.unicesumar.chat.persistence;

import br.unicesumar.chat.model.Mensagem;

import java.util.List;

/** Define operações para salvar mensagens e consultar o histórico recente. */
public interface MensagemRepository {
    /** Persiste uma mensagem e a devolve com o identificador gerado. */
    Mensagem salvar(Mensagem mensagem);

    /** Retorna até n mensagens em ordem cronológica. */
    List<Mensagem> buscarUltimas(int n);
}
