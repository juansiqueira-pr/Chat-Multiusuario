package br.unicesumar.chat.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Representa uma mensagem imutável enviada por um participante. */
public final class Mensagem {
    private static final DateTimeFormatter FORMATO_HORARIO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Long id;
    private final Usuario remetente;
    private final String conteudo;
    private final LocalDateTime horario;

    /** Cria uma mensagem, removendo espaços externos e validando os campos obrigatórios. */
    public Mensagem(Long id, Usuario remetente, String conteudo, LocalDateTime horario)
            throws MensagemInvalidaException {
        String texto = conteudo == null ? null : conteudo.trim();
        if (remetente == null || horario == null || texto == null || texto.isEmpty() || texto.length() > 500) {
            throw new MensagemInvalidaException("Mensagem exige remetente, horário e texto de 1 a 500 caracteres.");
        }
        this.id = id;
        this.remetente = remetente;
        this.conteudo = texto;
        this.horario = horario;
    }

    /** Retorna o identificador gerado pela persistência, ou null antes de salvar. */
    public Long getId() {
        return id;
    }

    /** Retorna o remetente da mensagem. */
    public Usuario getRemetente() {
        return remetente;
    }

    /** Retorna o conteúdo validado da mensagem. */
    public String getConteudo() {
        return conteudo;
    }

    /** Retorna o horário recebido na criação. */
    public LocalDateTime getHorario() {
        return horario;
    }

    /** Devolve uma nova mensagem com o identificador gerado pelo repositório. */
    public Mensagem comId(Long novoId) throws MensagemInvalidaException {
        if (novoId == null || novoId <= 0) {
            throw new MensagemInvalidaException("Identificador da mensagem deve ser positivo.");
        }
        return new Mensagem(novoId, remetente, conteudo, horario);
    }

    /** Formata a mensagem como data, hora, nome do remetente e texto. */
    public String formatar() {
        return "[" + horario.format(FORMATO_HORARIO) + "] " + remetente.getNome() + ": " + conteudo;
    }

    /** Compara todos os valores da mensagem. */
    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Mensagem outra)) {
            return false;
        }
        return Objects.equals(id, outra.id)
                && remetente.equals(outra.remetente)
                && conteudo.equals(outra.conteudo)
                && horario.equals(outra.horario);
    }

    /** Retorna o hash dos valores da mensagem. */
    @Override
    public int hashCode() {
        return Objects.hash(id, remetente, conteudo, horario);
    }
}
