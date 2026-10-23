package br.unicesumar.chat.server;

import br.unicesumar.chat.config.Config;
import br.unicesumar.chat.infraestrutura.ia.ClienteGroq;
import br.unicesumar.chat.infraestrutura.ia.ConfiguracaoIA;
import br.unicesumar.chat.infraestrutura.ia.ModeradorGroq;
import br.unicesumar.chat.infraestrutura.ia.ModeradorResiliente;
import br.unicesumar.chat.infraestrutura.ia.ResumidorGroq;
import br.unicesumar.chat.infraestrutura.ia.AssistenteTecnicoGroq;
import br.unicesumar.chat.infraestrutura.ia.AnalisadorDeClimaGroq;
import br.unicesumar.chat.infraestrutura.ia.FalhaInferenciaException;
import br.unicesumar.chat.persistence.jdbc.ConnectionFactory;
import br.unicesumar.chat.persistence.jdbc.InicializadorSchema;
import br.unicesumar.chat.persistence.jdbc.JdbcMensagemRepository;
import br.unicesumar.chat.service.ChatService;
import br.unicesumar.chat.service.ClientRegistry;
import br.unicesumar.chat.service.ModeradorDeMensagens;
import br.unicesumar.chat.service.ResumidorDeConversa;
import br.unicesumar.chat.service.ResumoIndisponivelException;
import br.unicesumar.chat.service.AssistenteTecnico;
import br.unicesumar.chat.service.AssistenteIndisponivelException;
import br.unicesumar.chat.service.AnalisadorDeClima;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Aceita conexões TCP e cria uma thread nomeada para cada cliente. */
public final class Servidor {
    private final int porta;
    private final ChatService servico;
    private final Set<ClienteHandler> handlersAtivos = ConcurrentHashMap.newKeySet();
    private final AtomicLong sequenciaClientes = new AtomicLong();
    private final Object cicloLock = new Object();
    private final Logger registrador = Logger.getLogger(Servidor.class.getName());
    private volatile ServerSocket socketServidor;
    private volatile boolean encerrando;

    /** Cria o servidor com porta fixa ou zero para receber porta dinâmica. */
    public Servidor(int porta, ChatService servico) {
        this.porta = porta;
        this.servico = servico;
    }

    /** Inicia o socket e retorna sem bloquear a thread que chamou o método. */
    public void iniciar() {
        synchronized (cicloLock) {
            if (socketServidor != null && !socketServidor.isClosed()) {
                throw new IllegalStateException("Servidor já está iniciado.");
            }
            try {
                socketServidor = new ServerSocket(porta);
            } catch (IOException excecao) {
                throw new IllegalStateException("Não foi possível abrir a porta " + porta + ".", excecao);
            }
            encerrando = false;
            ServerSocket socketAtual = socketServidor;
            Thread threadAceitacao = new Thread(() -> aceitarConexoes(socketAtual), "servidor-accept");
            threadAceitacao.start();
        }
    }

    /** Retorna a porta local depois que o socket servidor é iniciado. */
    public int getPortaLocal() {
        ServerSocket atual = socketServidor;
        return atual == null ? -1 : atual.getLocalPort();
    }

    /** Fecha o socket servidor e todos os handlers ainda ativos. */
    public void parar() {
        ServerSocket atual;
        synchronized (cicloLock) {
            encerrando = true;
            atual = socketServidor;
        }
        if (atual != null && !atual.isClosed()) {
            try {
                atual.close();
            } catch (IOException excecao) {
                registrador.log(Level.WARNING, "Falha ao fechar o socket servidor.", excecao);
            }
        }
        for (ClienteHandler handler : handlersAtivos.toArray(ClienteHandler[]::new)) {
            handler.fechar();
        }
    }

    private void aceitarConexoes(ServerSocket socketAtual) {
        while (!encerrando) {
            Socket socketCliente;
            try {
                socketCliente = socketAtual.accept();
            } catch (IOException excecao) {
                if (!encerrando) {
                    registrador.log(Level.WARNING, "Falha ao aceitar conexão de cliente.", excecao);
                }
                return;
            }

            synchronized (cicloLock) {
                if (encerrando || socketAtual.isClosed()) {
                    fecharSocketRecemAceito(socketCliente);
                    return;
                }
                ClienteHandler handler = new ClienteHandler(socketCliente, servico, handlersAtivos::remove);
                handlersAtivos.add(handler);
                Thread threadCliente = new Thread(handler,
                        "cliente-" + sequenciaClientes.incrementAndGet());
                try {
                    threadCliente.start();
                } catch (RuntimeException excecao) {
                    handlersAtivos.remove(handler);
                    handler.fechar();
                    registrador.log(Level.SEVERE, "Não foi possível iniciar a thread do cliente.", excecao);
                }
            }
        }
    }

    private void fecharSocketRecemAceito(Socket socket) {
        try {
            socket.close();
        } catch (IOException excecao) {
            registrador.log(Level.WARNING, "Falha ao fechar conexão aceita durante a parada.", excecao);
        }
    }

    /** Monta as dependências e inicia o servidor conforme chat.properties. */
    public static void main(String[] args) {
        Config config = Config.carregar();
        ConnectionFactory conexoes = new ConnectionFactory(
                config.getBancoUrl(), config.getBancoUsuario(), config.getBancoSenha());
        new InicializadorSchema(conexoes).inicializar();
        JdbcMensagemRepository repositorio = new JdbcMensagemRepository(conexoes);
        ClientRegistry registro = new ClientRegistry();
        Logger registrador = Logger.getLogger(Servidor.class.getName());
        ModeradorDeMensagens moderador = texto -> false;
        ResumidorDeConversa resumidor = mensagens -> {
            throw new ResumoIndisponivelException(
                    "Resumo indisponível: configure GROQ_API_KEY no arquivo .env.", null);
        };
        AssistenteTecnico assistente = pergunta -> {
            throw new AssistenteIndisponivelException(
                    "Assistente técnico indisponível: configure GROQ_API_KEY no arquivo .env.", null);
        };
        AnalisadorDeClima analisadorClima = mensagens -> {
            throw new FalhaInferenciaException(
                    "Análise de clima indisponível: configure GROQ_API_KEY no arquivo .env.", null);
        };
        try {
            ConfiguracaoIA configuracaoIA = new ConfiguracaoIA();
            ClienteGroq clienteGroq = new ClienteGroq();
            moderador = new ModeradorResiliente(new ModeradorGroq(clienteGroq, configuracaoIA));
            resumidor = new ResumidorGroq(clienteGroq, configuracaoIA);
            assistente = new AssistenteTecnicoGroq(clienteGroq, configuracaoIA);
            analisadorClima = new AnalisadorDeClimaGroq(clienteGroq, configuracaoIA);
        } catch (IllegalStateException excecao) {
            registrador.warning("IA Groq indisponível por falta de configuração; mensagens serão liberadas e "
                    + "resumo/assistente técnico informarão indisponibilidade.");
        }
        ChatService servico = new ChatService(repositorio, registro, java.time.Clock.systemDefaultZone(),
                config.getHistoricoTamanho(), moderador, resumidor, assistente,
                analisadorClima, config.getClimaIntervaloMensagens());
        Servidor servidor = new Servidor(config.getPorta(), servico);
        servidor.iniciar();
        registrador.info("Servidor iniciado na porta " + servidor.getPortaLocal() + ".");
    }
}
