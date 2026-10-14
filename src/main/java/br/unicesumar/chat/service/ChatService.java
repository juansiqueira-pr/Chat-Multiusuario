package br.unicesumar.chat.service;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.model.MensagemInvalidaException;
import br.unicesumar.chat.model.Usuario;
import br.unicesumar.chat.persistence.MensagemRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Coordena regras de conexão, histórico, persistência e distribuição das mensagens. */
public final class ChatService {
    public static final String PREFIXO_SISTEMA = "* ";
    public static final String PREFIXO_ERRO = "! ";

    private final MensagemRepository repositorio;
    private final ClientRegistry registro;
    private final Clock relogio;
    private final int tamanhoHistorico;
    private final Object ordemLock = new Object();
    private final Logger registrador = Logger.getLogger(ChatService.class.getName());

    /** Cria o serviço com repositório, registry, relógio e limite de histórico. */
    public ChatService(MensagemRepository repositorio, ClientRegistry registro, Clock relogio,
                       int tamanhoHistorico) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.registro = Objects.requireNonNull(registro);
        this.relogio = Objects.requireNonNull(relogio);
        this.tamanhoHistorico = tamanhoHistorico;
    }

    /** Registra a conexão, confirma o nick, envia o histórico e anuncia a entrada em ordem única. */
    public void conectar(ConexaoCliente cliente) throws NomeEmUsoException {
        synchronized (ordemLock) {
            if (!registro.adicionar(cliente)) {
                throw new NomeEmUsoException("Nome já está em uso: " + cliente.getUsuario().getNome());
            }
            try {
                cliente.enviar(PREFIXO_SISTEMA + "Conectado como " + cliente.getUsuario().getNome());
                for (Mensagem mensagem : buscarHistoricoSemLock()) {
                    cliente.enviar(mensagem.formatar());
                }
            } catch (FalhaEnvioException excecao) {
                registro.remover(cliente);
                registrador.log(Level.WARNING, "Cliente falhou ao receber o histórico.", excecao);
                return;
            }
            broadcast(PREFIXO_SISTEMA + cliente.getUsuario().getNome() + " entrou");
        }
    }

    /** Remove uma conexão uma única vez e anuncia a saída aos clientes restantes. */
    public void desconectar(ConexaoCliente cliente) {
        synchronized (ordemLock) {
            if (registro.remover(cliente)) {
                broadcast(PREFIXO_SISTEMA + cliente.getUsuario().getNome() + " saiu");
            }
        }
    }

    /** Valida, persiste e distribui uma mensagem sob o mesmo lock de ordenação. */
    public Mensagem enviarMensagem(Usuario remetente, String conteudo) throws MensagemInvalidaException {
        synchronized (ordemLock) {
            Mensagem mensagem = new Mensagem(null, remetente, conteudo, LocalDateTime.now(relogio));
            Mensagem salva = repositorio.salvar(mensagem);
            broadcast(salva.formatar());
            return salva;
        }
    }

    /** Envia uma linha a uma cópia dos clientes e remove conexões que falharem. */
    public void broadcast(String linha) {
        synchronized (ordemLock) {
            for (ConexaoCliente cliente : registro.snapshot()) {
                try {
                    cliente.enviar(linha);
                } catch (FalhaEnvioException excecao) {
                    registro.remover(cliente);
                    registrador.log(Level.WARNING,
                            "Cliente removido após falha de envio: " + cliente.getUsuario().getNome(), excecao);
                }
            }
        }
    }

    /** Retorna as últimas mensagens conforme o limite configurado. */
    public List<Mensagem> buscarHistorico() {
        synchronized (ordemLock) {
            return buscarHistoricoSemLock();
        }
    }

    private List<Mensagem> buscarHistoricoSemLock() {
        return repositorio.buscarUltimas(tamanhoHistorico);
    }
}
