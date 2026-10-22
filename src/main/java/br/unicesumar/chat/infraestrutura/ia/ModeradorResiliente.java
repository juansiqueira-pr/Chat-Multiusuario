package br.unicesumar.chat.infraestrutura.ia;

import br.unicesumar.chat.service.ModeradorDeMensagens;

import java.util.Objects;
import java.util.logging.Logger;

/** Converte indisponibilidade da inferência em liberação da mensagem. */
public final class ModeradorResiliente implements ModeradorDeMensagens {
    private static final Logger REGISTRADOR = Logger.getLogger(ModeradorResiliente.class.getName());
    private final ModeradorDeMensagens moderador;

    /** Cria adaptador fail-open para o moderador de infraestrutura. */
    public ModeradorResiliente(ModeradorDeMensagens moderador) {
        this.moderador = Objects.requireNonNull(moderador);
    }

    /** Libera o texto se a inferência falhar e registra um aviso sem detalhes sensíveis. */
    @Override
    public boolean ehOfensiva(String texto) {
        try {
            return moderador.ehOfensiva(texto);
        } catch (FalhaInferenciaException excecao) {
            REGISTRADOR.warning("Moderação indisponível neste momento; mensagem liberada.");
            return false;
        }
    }
}
