package br.unicesumar.chat.persistence.jdbc;

import br.unicesumar.chat.persistence.PersistenciaException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Cria as tabelas necessárias a partir do recurso schema.sql. */
public final class InicializadorSchema {
    private final ConnectionFactory conexoes;

    /** Cria o inicializador com a fábrica de conexões do banco. */
    public InicializadorSchema(ConnectionFactory conexoes) {
        this.conexoes = conexoes;
    }

    /** Executa cada comando do recurso SQL de criação do esquema. */
    public void inicializar() {
        try (InputStream recurso = InicializadorSchema.class.getResourceAsStream("/schema.sql")) {
            if (recurso == null) {
                throw new PersistenciaException("Recurso schema.sql não encontrado.", null);
            }
            String script = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conexao = conexoes.abrir(); Statement comando = conexao.createStatement()) {
                for (String instrucao : script.split(";")) {
                    if (!instrucao.isBlank()) {
                        comando.execute(instrucao.trim());
                    }
                }
            }
        } catch (IOException | SQLException excecao) {
            throw new PersistenciaException("Não foi possível inicializar o esquema do banco.", excecao);
        }
    }
}
