package br.unicesumar.chat.integracao;

import br.unicesumar.chat.infraestrutura.ia.ClienteGroq;
import br.unicesumar.chat.infraestrutura.ia.ConfiguracaoIA;
import br.unicesumar.chat.infraestrutura.ia.ModeradorGroq;
import br.unicesumar.chat.infraestrutura.ia.ModeradorResiliente;
import br.unicesumar.chat.persistence.jdbc.ConnectionFactory;
import br.unicesumar.chat.persistence.jdbc.InicializadorSchema;
import br.unicesumar.chat.persistence.jdbc.JdbcMensagemRepository;
import br.unicesumar.chat.server.Servidor;
import br.unicesumar.chat.service.ChatService;
import br.unicesumar.chat.service.ClientRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;

/** Verifica manualmente a moderação real pelo servidor TCP e pela API Groq. */
public final class ModeracaoChatManual {
    private ModeracaoChatManual() { }

    /** Abre dois clientes, envia conteúdo seguro e ofensivo e imprime os resultados. */
    public static void main(String[] args) throws Exception {
        ConfiguracaoIA configuracao = new ConfiguracaoIA();
        String banco = "jdbc:h2:mem:moderacao_manual_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
        ConnectionFactory conexoes = new ConnectionFactory(banco, "sa", "");
        new InicializadorSchema(conexoes).inicializar();
        ChatService servico = new ChatService(new JdbcMensagemRepository(conexoes), new ClientRegistry(),
                Clock.systemDefaultZone(), 20, new ModeradorResiliente(
                new ModeradorGroq(new ClienteGroq(), configuracao)));
        Servidor servidor = new Servidor(0, servico);
        servidor.iniciar();
        try (ClienteManual ana = ClienteManual.conectar(servidor.getPortaLocal(), "ana");
             ClienteManual bruno = ClienteManual.conectar(servidor.getPortaLocal(), "bruno")) {
            ana.aguardar(linha -> linha.equals("* bruno entrou"));
            bruno.aguardar(linha -> linha.equals("* bruno entrou"));

            String segura = "Boa noite! Obrigado pela ajuda de hoje.";
            ana.enviar(segura);
            ana.aguardar(linha -> linha.contains("ana: " + segura));
            bruno.aguardar(linha -> linha.contains("ana: " + segura));
            System.out.println("OK - mensagem permitida chegou aos dois clientes.");

            String ofensiva = "Seu idiota, vou te agredir.";
            ana.enviar(ofensiva);
            ana.aguardar(linha -> linha.equals("! sua mensagem foi bloqueada por conter conteúdo ofensivo"));
            bruno.confirmarSemLinha(Duration.ofSeconds(3));
            boolean persistida = servico.buscarHistorico().stream()
                    .anyMatch(mensagem -> mensagem.getConteudo().equals(ofensiva));
            if (persistida) throw new IllegalStateException("Mensagem bloqueada apareceu no histórico.");
            System.out.println("OK - mensagem ofensiva bloqueada; aviso só para ana; sem broadcast nem persistência.");
        } finally {
            servidor.parar();
        }
    }

    private static final class ClienteManual implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader leitor;
        private final PrintWriter escritor;

        private ClienteManual(Socket socket) throws IOException {
            this.socket = socket;
            leitor = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            escritor = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private static ClienteManual conectar(int porta, String nick) throws IOException {
            Socket socket = new Socket("127.0.0.1", porta);
            socket.setSoTimeout(95_000);
            ClienteManual cliente = new ClienteManual(socket);
            cliente.enviar(nick);
            cliente.aguardar(linha -> linha.equals("* Conectado como " + nick));
            return cliente;
        }

        private void enviar(String linha) throws IOException {
            escritor.println(linha);
            escritor.flush();
            if (escritor.checkError()) throw new IOException("Falha ao enviar linha ao servidor.");
        }

        private String aguardar(Predicate<String> criterio) throws IOException {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (criterio.test(linha)) return linha;
            }
            throw new IOException("Servidor fechou a conexão antes da resposta esperada.");
        }

        private void confirmarSemLinha(Duration espera) throws IOException {
            socket.setSoTimeout(Math.toIntExact(espera.toMillis()));
            try {
                String linha = leitor.readLine();
                if (linha != null) throw new IllegalStateException("O outro cliente recebeu: " + linha);
                throw new IOException("Servidor encerrou a conexão durante a verificação.");
            } catch (SocketTimeoutException esperado) {
                // O silêncio confirma que não houve broadcast para este cliente.
            } finally {
                socket.setSoTimeout(95_000);
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
