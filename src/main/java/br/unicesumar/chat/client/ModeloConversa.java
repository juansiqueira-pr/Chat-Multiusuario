package br.unicesumar.chat.client;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Mantém itens prontos para apresentação com autoria, agrupamento e limite de memória. */
public final class ModeloConversa {
    private static final int LIMITE = 500;
    private static final Duration JANELA_GRUPO = Duration.ofMinutes(2);
    private final String nick;
    private final List<Item> itens = new ArrayList<>();

    /** Cria o modelo de conversa para o participante local. */
    public ModeloConversa(String nick) {
        this.nick = Objects.requireNonNull(nick).toLowerCase(Locale.ROOT);
    }

    /** Adiciona uma linha e descarta o item mais antigo quando o limite é atingido. */
    public synchronized void adicionar(LinhaServidor linha) {
        boolean propria = linha.tipo() == LinhaServidor.Tipo.MENSAGEM
                && linha.remetente().toLowerCase(Locale.ROOT).equals(nick);
        boolean agrupada = agruparComAnterior(linha);
        itens.add(new Item(linha, propria, agrupada));
        if (itens.size() > LIMITE) itens.removeFirst();
    }

    /** Retorna uma cópia imutável dos itens atuais. */
    public synchronized List<Item> itens() {
        return List.copyOf(itens);
    }

    private boolean agruparComAnterior(LinhaServidor linha) {
        if (linha.tipo() != LinhaServidor.Tipo.MENSAGEM || itens.isEmpty()) return false;
        Item anterior = itens.getLast();
        LinhaServidor linhaAnterior = anterior.linha();
        if (linhaAnterior.tipo() != LinhaServidor.Tipo.MENSAGEM
                || !linha.remetente().equalsIgnoreCase(linhaAnterior.remetente())
                || linha.horario() == null || linhaAnterior.horario() == null) return false;
        LocalTime atual = linha.horario();
        LocalTime passado = linhaAnterior.horario();
        long segundos = Duration.between(passado, atual).getSeconds();
        return segundos >= 0 && segundos <= JANELA_GRUPO.getSeconds();
    }

    /** Item imutável pronto para a camada visual. */
    public record Item(LinhaServidor linha, boolean propria, boolean agrupada) { }
}
