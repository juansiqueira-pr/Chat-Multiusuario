package br.unicesumar.chat.service;

/** Contrato para responder dúvidas técnicas sem acoplar o chat à IA concreta. */
@FunctionalInterface
public interface AssistenteTecnico {
    /** Responde a uma pergunta técnica dentro da política de uso configurada. */
    String responder(String pergunta);
}
