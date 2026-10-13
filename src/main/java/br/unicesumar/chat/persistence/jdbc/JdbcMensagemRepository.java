package br.unicesumar.chat.persistence.jdbc;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.model.MensagemInvalidaException;
import br.unicesumar.chat.model.Usuario;
import br.unicesumar.chat.model.UsuarioInvalidoException;
import br.unicesumar.chat.persistence.MensagemRepository;
import br.unicesumar.chat.persistence.PersistenciaException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Implementa o repositório de mensagens usando JDBC e comandos preparados. */
public final class JdbcMensagemRepository implements MensagemRepository {
    private final ConnectionFactory conexoes;

    /** Cria o repositório com uma fábrica que abre conexões por operação. */
    public JdbcMensagemRepository(ConnectionFactory conexoes) {
        this.conexoes = conexoes;
    }

    /** Insere uma mensagem e devolve uma nova instância com o ID gerado. */
    @Override
    public Mensagem salvar(Mensagem mensagem) {
        String sql = "INSERT INTO mensagem (remetente, conteudo, horario) VALUES (?, ?, ?)";
        try (Connection conexao = conexoes.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            comando.setString(1, mensagem.getRemetente().getNome());
            comando.setString(2, mensagem.getConteudo());
            comando.setTimestamp(3, Timestamp.valueOf(mensagem.getHorario()));
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("O banco não retornou o identificador da mensagem.");
                }
                return mensagem.comId(chaves.getLong(1));
            }
        } catch (SQLException | MensagemInvalidaException excecao) {
            throw new PersistenciaException("Não foi possível salvar a mensagem.", excecao);
        }
    }

    /** Busca as mensagens mais recentes e as devolve em ordem cronológica. */
    @Override
    public List<Mensagem> buscarUltimas(int n) {
        if (n <= 0) {
            return List.of();
        }
        String sql = "SELECT id, remetente, conteudo, horario FROM mensagem ORDER BY id DESC LIMIT ?";
        List<Mensagem> mensagens = new ArrayList<>();
        try (Connection conexao = conexoes.abrir(); PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, n);
            try (ResultSet resultados = comando.executeQuery()) {
                while (resultados.next()) {
                    Usuario remetente = new Usuario(resultados.getString("remetente"));
                    Mensagem mensagem = new Mensagem(
                            resultados.getLong("id"),
                            remetente,
                            resultados.getString("conteudo"),
                            resultados.getTimestamp("horario").toLocalDateTime());
                    mensagens.add(mensagem);
                }
            }
            Collections.reverse(mensagens);
            return List.copyOf(mensagens);
        } catch (SQLException | UsuarioInvalidoException | MensagemInvalidaException excecao) {
            throw new PersistenciaException("Não foi possível consultar o histórico de mensagens.", excecao);
        }
    }
}
