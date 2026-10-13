package br.unicesumar.chat.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Mantém a coleção concorrente de conexões ativas e seus nomes únicos. */
public final class ClientRegistry {
    private final CopyOnWriteArrayList<ConexaoCliente> clientes = new CopyOnWriteArrayList<>();

    /** Adiciona a conexão se ainda não houver usuário com o mesmo nome. */
    public synchronized boolean adicionar(ConexaoCliente cliente) {
        boolean nomeOcupado = clientes.stream()
                .anyMatch(ativo -> ativo.getUsuario().equals(cliente.getUsuario()));
        if (nomeOcupado) {
            return false;
        }
        return clientes.addIfAbsent(cliente);
    }

    /** Remove uma conexão ativa, retornando se ela estava registrada. */
    public synchronized boolean remover(ConexaoCliente cliente) {
        return clientes.remove(cliente);
    }

    /** Retorna uma cópia imutável da lista de conexões atuais. */
    public List<ConexaoCliente> snapshot() {
        return List.copyOf(clientes);
    }

    /** Retorna o número atual de conexões registradas. */
    public int quantidade() {
        return clientes.size();
    }

    /** Retorna os nomes de usuário atualmente conectados. */
    public List<String> usuariosOnline() {
        return snapshot().stream().map(cliente -> cliente.getUsuario().getNome()).toList();
    }
}
