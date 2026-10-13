package br.unicesumar.chat.support;

import br.unicesumar.chat.model.Usuario;
import br.unicesumar.chat.service.ConexaoCliente;
import br.unicesumar.chat.service.FalhaEnvioException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Conexão falsa que guarda linhas para testes e pode simular falha de escrita. */
public final class FakeConexao implements ConexaoCliente {
    private final Usuario usuario;
    private final boolean falharAoEnviar;
    private final CopyOnWriteArrayList<String> linhas = new CopyOnWriteArrayList<>();

    /** Cria uma conexão falsa que aceita envios. */
    public FakeConexao(Usuario usuario) {
        this(usuario, false);
    }

    /** Cria uma conexão falsa com opção para falhar em todos os envios. */
    public FakeConexao(Usuario usuario, boolean falharAoEnviar) {
        this.usuario = usuario;
        this.falharAoEnviar = falharAoEnviar;
    }

    /** Retorna o usuário desta conexão falsa. */
    @Override
    public Usuario getUsuario() {
        return usuario;
    }

    /** Guarda a linha ou lança a falha configurada no teste. */
    @Override
    public void enviar(String linha) {
        if (falharAoEnviar) {
            throw new FalhaEnvioException("Falha simulada de escrita.");
        }
        linhas.add(linha);
    }

    /** Retorna uma cópia das linhas enviadas à conexão falsa. */
    public List<String> linhas() {
        return List.copyOf(linhas);
    }
}
