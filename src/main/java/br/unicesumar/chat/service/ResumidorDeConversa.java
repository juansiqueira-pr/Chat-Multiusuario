package br.unicesumar.chat.service;

import br.unicesumar.chat.model.Mensagem;

import java.util.List;

/** Resume uma lista de mensagens sem depender da implementação de IA. */
@FunctionalInterface
public interface ResumidorDeConversa {
    /** Produz um resumo textual das mensagens recebidas. */
    String resumir(List<Mensagem> mensagens);
}
