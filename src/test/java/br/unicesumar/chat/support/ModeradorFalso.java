package br.unicesumar.chat.support;

import br.unicesumar.chat.service.ModeradorDeMensagens;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Moderador configurável para testes, sem acesso à rede. */
public final class ModeradorFalso implements ModeradorDeMensagens {
    private final boolean ofensiva;
    private final CopyOnWriteArrayList<String> mensagensAnalisadas = new CopyOnWriteArrayList<>();

    /** Cria um moderador que classifica todas as mensagens com o resultado definido. */
    public ModeradorFalso(boolean ofensiva) {
        this.ofensiva = ofensiva;
    }

    /** Registra o texto recebido e devolve a classificação configurada. */
    @Override
    public boolean ehOfensiva(String texto) {
        mensagensAnalisadas.add(texto);
        return ofensiva;
    }

    /** Retorna os textos analisados durante o teste. */
    public List<String> mensagensAnalisadas() {
        return List.copyOf(mensagensAnalisadas);
    }
}
