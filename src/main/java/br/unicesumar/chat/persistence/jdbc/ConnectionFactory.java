package br.unicesumar.chat.persistence.jdbc;

import br.unicesumar.chat.persistence.PersistenciaException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Abre conexões JDBC independentes para cada operação de persistência. */
public final class ConnectionFactory {
    private final String url;
    private final String usuario;
    private final String senha;

    /** Cria a fábrica com a URL e as credenciais do banco. */
    public ConnectionFactory(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    /** Abre uma conexão nova com o banco configurado. */
    public Connection abrir() {
        try {
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException excecao) {
            throw new PersistenciaException("Não foi possível abrir conexão com o banco.", excecao);
        }
    }
}
