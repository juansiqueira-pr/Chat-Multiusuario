package br.unicesumar.chat.integracao;

import br.unicesumar.chat.persistence.jdbc.ConnectionFactory;
import br.unicesumar.chat.persistence.jdbc.InicializadorSchema;
import br.unicesumar.chat.persistence.jdbc.JdbcMensagemRepository;
import br.unicesumar.chat.server.Servidor;
import br.unicesumar.chat.service.ChatService;
import br.unicesumar.chat.service.ClientRegistry;
import br.unicesumar.chat.service.AssistenteTecnico;
import br.unicesumar.chat.service.AssistenteIndisponivelException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import br.unicesumar.chat.service.ResumoIndisponivelException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServidorIntegracaoTest {
    private final List<ClienteTeste> clientes = new ArrayList<>();
    private Servidor servidor;

    @BeforeEach
    void iniciarServidor() {
        iniciarServidor(pergunta -> "Resposta educativa para: " + pergunta);
    }

    private void iniciarServidor(AssistenteTecnico assistente) {
        String nomeBanco = "tcp_" + UUID.randomUUID().toString().replace("-", "");
        ConnectionFactory conexoes = new ConnectionFactory(
                "jdbc:h2:mem:" + nomeBanco + ";DB_CLOSE_DELAY=-1", "sa", "");
        new InicializadorSchema(conexoes).inicializar();
        ChatService servico = new ChatService(new JdbcMensagemRepository(conexoes), new ClientRegistry(),
                Clock.fixed(Instant.parse("2026-10-13T12:30:00Z"), ZoneOffset.UTC), 20,
                texto -> false, mensagens -> "Resumo: " + mensagens.getLast().getConteudo(), assistente);
        servidor = new Servidor(0, servico);
        servidor.iniciar();
    }

    @AfterEach
    void encerrarServidor() {
        for (ClienteTeste cliente : clientes) {
            cliente.close();
        }
        servidor.parar();
    }

    @Test
    void broadcastChegaAosTresClientes() throws Exception {
        ClienteTeste ana = conectar("ana");
        ClienteTeste bruno = conectar("bruno");
        ClienteTeste carlos = conectar("carlos");

        ana.enviar("broadcast-real");

        assertTrue(ana.aguardar(linha -> linha.contains("ana: broadcast-real")));
        assertTrue(bruno.aguardar(linha -> linha.contains("ana: broadcast-real")));
        assertTrue(carlos.aguardar(linha -> linha.contains("ana: broadcast-real")));
    }

    @Test
    void novoClienteRecebeHistoricoEmOrdemCronologica() throws Exception {
        ClienteTeste ana = conectar("ana");
        ClienteTeste bruno = conectar("bruno");
        ana.enviar("historico-primeira");
        assertTrue(ana.aguardar(linha -> linha.contains("ana: historico-primeira")));
        assertTrue(bruno.aguardar(linha -> linha.contains("ana: historico-primeira")));
        ana.enviar("historico-segunda");
        assertTrue(ana.aguardar(linha -> linha.contains("ana: historico-segunda")));
        assertTrue(bruno.aguardar(linha -> linha.contains("ana: historico-segunda")));

        ClienteTeste carlos = abrirCliente("carlos");
        assertTrue(carlos.aguardar(linha -> linha.equals("* Conectado como carlos")));
        String primeira = carlos.aguardarLinha(linha -> linha.contains("ana: historico-primeira"));
        String segunda = carlos.aguardarLinha(linha -> linha.contains("ana: historico-segunda"));
        assertTrue(carlos.aguardar(linha -> linha.equals("* carlos entrou")));

        assertTrue(primeira.contains("historico-primeira"));
        assertTrue(segunda.contains("historico-segunda"));
    }

    @Test
    void fechamentoAbruptoNaoInterrompeOsOutrosClientes() throws Exception {
        ClienteTeste ana = conectar("ana");
        ClienteTeste bruno = conectar("bruno");
        ClienteTeste carlos = conectar("carlos");

        carlos.fecharAbruptamente();

        assertTrue(ana.aguardar(linha -> linha.equals("* carlos saiu")));
        assertTrue(bruno.aguardar(linha -> linha.equals("* carlos saiu")));
        bruno.enviar("ainda-funciona");
        assertTrue(ana.aguardar(linha -> linha.contains("bruno: ainda-funciona")));
        assertTrue(bruno.aguardar(linha -> linha.contains("bruno: ainda-funciona")));
    }

    @Test
    void nomeDuplicadoERecusado() throws Exception {
        ClienteTeste ana = conectar("ana");
        ClienteTeste duplicada = abrirCliente("ANA");

        assertTrue(duplicada.aguardar(linha -> linha.startsWith("! Nome já está em uso")));
        duplicada.close();
        ana.enviar("depois-da-recusa");
        assertTrue(ana.aguardar(linha -> linha.contains("ana: depois-da-recusa")));
    }

    @Test
    void mensagemVaziaGeraErroSomenteParaRemetente() throws Exception {
        ClienteTeste ana = conectar("ana");
        ClienteTeste bruno = conectar("bruno");

        ana.enviar("");
        assertTrue(ana.aguardar(linha -> linha.startsWith("! ")));
        ana.enviar("marcador-vazio");
        List<String> linhasBruno = bruno.aguardarAte(linha -> linha.contains("ana: marcador-vazio"));

        assertFalse(linhasBruno.stream().anyMatch(linha -> linha.startsWith("! ")));
        assertTrue(ana.aguardar(linha -> linha.contains("ana: marcador-vazio")));
    }

    @Test
    void resumoDoHistoricoVaiSomenteAoClienteQueSolicitou() throws Exception {
        ClienteTeste ana = conectar("ana");
        ClienteTeste bruno = conectar("bruno");
        ana.enviar("mensagem para resumir");
        assertTrue(ana.aguardar(linha -> linha.contains("ana: mensagem para resumir")));
        assertTrue(bruno.aguardar(linha -> linha.contains("ana: mensagem para resumir")));

        ana.enviar("/historico");
        assertTrue(ana.aguardar(linha -> linha.equals("* Resumo: Resumo: mensagem para resumir")));
        ana.enviar("marcador-de-privacidade");
        List<String> linhasBruno = bruno.aguardarAte(linha -> linha.contains("marcador-de-privacidade"));

        assertFalse(linhasBruno.stream().anyMatch(linha -> linha.contains("Resumo:")));
    }

    @Test
    void falhaNoResumoMostraErroPrivadoEServidorContinua() throws Exception {
        servidor.parar();
        String nomeBanco = "tcp_falha_resumo_" + UUID.randomUUID().toString().replace("-", "");
        ConnectionFactory conexoes = new ConnectionFactory(
                "jdbc:h2:mem:" + nomeBanco + ";DB_CLOSE_DELAY=-1", "sa", "");
        new InicializadorSchema(conexoes).inicializar();
        ChatService servico = new ChatService(new JdbcMensagemRepository(conexoes), new ClientRegistry(),
                Clock.fixed(Instant.parse("2026-10-13T12:30:00Z"), ZoneOffset.UTC), 20,
                texto -> false, mensagens -> { throw new ResumoIndisponivelException("modelo indisponível", null); });
        servidor = new Servidor(0, servico);
        servidor.iniciar();
        ClienteTeste ana = conectar("ana");
        ana.enviar("mensagem existente");
        assertTrue(ana.aguardar(linha -> linha.contains("mensagem existente")));
        ana.enviar("/historico");
        assertTrue(ana.aguardar(linha -> linha.equals("! resumo indisponível neste momento")));
        ana.enviar("servidor-continua");
        assertTrue(ana.aguardar(linha -> linha.contains("servidor-continua")));
    }

    @Test
    void comandoHackerRespondeSomenteAoSolicitante() throws Exception {
        ClienteTeste ana = conectar("ana");
        ClienteTeste bruno = conectar("bruno");

        ana.enviar("/hacker como proteger meu servidor?");
        assertTrue(ana.aguardar(linha -> linha.equals(
                "* Assistente técnico: Resposta educativa para: como proteger meu servidor?")));
        ana.enviar("marcador-hacker-privado");
        List<String> linhasBruno = bruno.aguardarAte(linha -> linha.contains("marcador-hacker-privado"));
        assertFalse(linhasBruno.stream().anyMatch(linha -> linha.contains("Assistente técnico:")));
    }

    @Test
    void falhaDoAssistenteNaoEncerraClienteNemServidor() throws Exception {
        servidor.parar();
        iniciarServidor(pergunta -> {
            throw new AssistenteIndisponivelException("modelo indisponível", null);
        });
        ClienteTeste ana = conectar("ana");

        ana.enviar("/hacker explique TLS");
        assertTrue(ana.aguardar(linha -> linha.equals("! assistente técnico indisponível neste momento")));
        ana.enviar("servidor-segue-ativo");
        assertTrue(ana.aguardar(linha -> linha.contains("servidor-segue-ativo")));
    }

    @Test
    void comandoHackerSemPerguntaRecebeInstrucaoPrivada() throws Exception {
        ClienteTeste ana = conectar("ana");
        ana.enviar("/hacker");

        assertTrue(ana.aguardar(linha -> linha.equals("! use: /hacker <pergunta>")));
        ana.enviar("ainda-conectado");
        assertTrue(ana.aguardar(linha -> linha.contains("ainda-conectado")));
    }

    private ClienteTeste conectar(String nick) throws Exception {
        ClienteTeste cliente = abrirCliente(nick);
        assertTrue(cliente.aguardar(linha -> linha.equals("* Conectado como " + nick)));
        assertTrue(cliente.aguardar(linha -> linha.equals("* " + nick + " entrou")));
        return cliente;
    }

    private ClienteTeste abrirCliente(String nick) throws Exception {
        ClienteTeste cliente = new ClienteTeste(servidor.getPortaLocal());
        clientes.add(cliente);
        cliente.enviar(nick);
        return cliente;
    }

    private static final class ClienteTeste implements AutoCloseable {
        private static final Duration TEMPO_LIMITE = Duration.ofSeconds(8);
        private final Socket socket;
        private final BufferedReader entrada;
        private final PrintWriter saida;
        private final BlockingQueue<String> linhas = new LinkedBlockingQueue<>();
        private final CountDownLatch leitorEncerrado = new CountDownLatch(1);
        private final AtomicReference<IOException> falhaLeitura = new AtomicReference<>();

        private ClienteTeste(int porta) throws IOException {
            socket = new Socket("127.0.0.1", porta);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            saida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            Thread leitor = new Thread(this::lerLinhas, "teste-cliente-receptor");
            leitor.setDaemon(true);
            leitor.start();
        }

        private synchronized void enviar(String linha) throws IOException {
            saida.println(linha);
            saida.flush();
            if (saida.checkError()) {
                throw new IOException("Falha ao escrever no socket de teste.");
            }
        }

        private boolean aguardar(Predicate<String> condicao) throws InterruptedException {
            aguardarLinha(condicao);
            return true;
        }

        private String aguardarLinha(Predicate<String> condicao) throws InterruptedException {
            long prazo = System.nanoTime() + TEMPO_LIMITE.toNanos();
            List<String> recebidas = new ArrayList<>();
            while (System.nanoTime() < prazo) {
                long restante = prazo - System.nanoTime();
                String linha = linhas.poll(restante, TimeUnit.NANOSECONDS);
                if (linha == null) {
                    break;
                }
                recebidas.add(linha);
                if (condicao.test(linha)) {
                    return linha;
                }
            }
            IOException falha = falhaLeitura.get();
            throw new AssertionError("Timeout aguardando linha. Recebidas: " + recebidas, falha);
        }

        private List<String> aguardarAte(Predicate<String> condicao) throws InterruptedException {
            long prazo = System.nanoTime() + TEMPO_LIMITE.toNanos();
            List<String> recebidas = new ArrayList<>();
            while (System.nanoTime() < prazo) {
                long restante = prazo - System.nanoTime();
                String linha = linhas.poll(restante, TimeUnit.NANOSECONDS);
                if (linha == null) {
                    break;
                }
                recebidas.add(linha);
                if (condicao.test(linha)) {
                    return recebidas;
                }
            }
            IOException falha = falhaLeitura.get();
            throw new AssertionError("Timeout aguardando marcador. Recebidas: " + recebidas, falha);
        }

        private void fecharAbruptamente() throws IOException, InterruptedException {
            socket.close();
            leitorEncerrado.await(2, TimeUnit.SECONDS);
        }

        private void lerLinhas() {
            try {
                String linha;
                while ((linha = entrada.readLine()) != null) {
                    linhas.add(linha);
                }
            } catch (IOException excecao) {
                if (!socket.isClosed()) {
                    falhaLeitura.set(excecao);
                }
            } finally {
                leitorEncerrado.countDown();
            }
        }

        @Override
        public void close() {
            try {
                socket.close();
                leitorEncerrado.await(2, TimeUnit.SECONDS);
            } catch (IOException excecao) {
                throw new AssertionError("Falha ao fechar cliente de teste.", excecao);
            } catch (InterruptedException excecao) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrompido ao fechar cliente de teste.", excecao);
            }
        }
    }
}
