package br.unicesumar.chat.service;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.model.NivelClima;

import java.util.List;

/** Classifica o clima de uma amostra recente da conversa. */
public interface AnalisadorDeClima {
    /** Analisa mensagens e retorna uma das categorias conhecidas. */
    NivelClima analisar(List<Mensagem> mensagens);
}
