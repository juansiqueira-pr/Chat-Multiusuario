package br.unicesumar.chat.support;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.model.NivelClima;
import br.unicesumar.chat.service.AnalisadorDeClima;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Analisador configurável sem rede usado pelos testes de serviço. */
public final class AnalisadorDeClimaFalso implements AnalisadorDeClima {
    private final AtomicInteger chamadas = new AtomicInteger();
    private volatile NivelClima nivel;
    private volatile RuntimeException falha;
    private volatile List<Mensagem> mensagensRecebidas = List.of();

    public AnalisadorDeClimaFalso(NivelClima nivel) { this.nivel = nivel; }

    public void definirNivel(NivelClima novoNivel) { nivel = novoNivel; }

    public void falharCom(RuntimeException excecao) { falha = excecao; }

    public int chamadas() { return chamadas.get(); }

    public List<Mensagem> mensagensRecebidas() { return mensagensRecebidas; }

    @Override
    public NivelClima analisar(List<Mensagem> mensagens) {
        chamadas.incrementAndGet();
        mensagensRecebidas = List.copyOf(mensagens);
        RuntimeException erro = falha;
        if (erro != null) throw erro;
        return nivel;
    }
}
