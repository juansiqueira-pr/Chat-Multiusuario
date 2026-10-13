package br.unicesumar.chat.model;

import java.util.Locale;
import java.util.Objects;

/** Representa um participante com nome validado e imutável. */
public final class Usuario {
    private final String nome;

    /** Cria um usuário com nome não vazio, sem espaços e de até vinte caracteres. */
    public Usuario(String nome) throws UsuarioInvalidoException {
        if (nome == null || nome.isEmpty() || nome.length() > 20
                || nome.codePoints().anyMatch(Character::isWhitespace)) {
            throw new UsuarioInvalidoException("Nome deve ter de 1 a 20 caracteres e não conter espaços.");
        }
        this.nome = nome;
    }

    /** Retorna o nome apresentado no chat. */
    public String getNome() {
        return nome;
    }

    /** Compara usuários sem diferenciar maiúsculas e minúsculas. */
    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Usuario outro)) {
            return false;
        }
        return nome.toLowerCase(Locale.ROOT).equals(outro.nome.toLowerCase(Locale.ROOT));
    }

    /** Retorna hash compatível com a igualdade sem diferenciar maiúsculas. */
    @Override
    public int hashCode() {
        return Objects.hash(nome.toLowerCase(Locale.ROOT));
    }
}
