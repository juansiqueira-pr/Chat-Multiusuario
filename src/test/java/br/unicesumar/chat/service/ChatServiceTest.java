package br.unicesumar.chat.service;

import br.unicesumar.chat.model.Mensagem;
import br.unicesumar.chat.model.MensagemInvalidaException;
import br.unicesumar.chat.model.Usuario;
import br.unicesumar.chat.model.UsuarioInvalidoException;
import br.unicesumar.chat.infraestrutura.ia.FalhaInferenciaException;
import br.unicesumar.chat.infraestrutura.ia.ModeradorResiliente;
import br.unicesumar.chat.support.FakeConexao;
import br.unicesumar.chat.support.InMemoryMensagemRepository;
import br.unicesumar.chat.support.ModeradorFalso;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceTest {
    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-10-13T12:30:00Z"), ZoneOffset.UTC);

    @Test
    void salvaMensagemAntesDeEntregarParaTodosIncluindoRemetente() throws Exception {
        InMemoryMensagemRepository repositorio = new InMemoryMensagemRepository();
        ClientRegistry registro = new ClientRegistry();
        FakeConexao ana = conexao("ana");
        FakeConexao bruno = conexao("bruno");
        registro.adicionar(ana);
        registro.adicionar(bruno);
        ChatService servico = servico(repositorio, registro);

        Mensagem salva = servico.enviarMensagem(ana.getUsuario(), "  olá  ");

        assertEquals(1L, salva.getId());
        assertEquals(List.of("olá"), servico.buscarHistorico().stream().map(Mensagem::getConteudo).toList());
        assertTrue(ana.linhas().contains(salva.formatar()));
        assertTrue(bruno.linhas().contains(salva.formatar()));
    }

    @Test
    void mensagemNormalPassaPelaModeracaoEChegaAosClientes() throws Exception {
        InMemoryMensagemRepository repositorio = new InMemoryMensagemRepository();
        ClientRegistry registro = new ClientRegistry();
        FakeConexao ana = conexao("ana");
        FakeConexao bruno = conexao("bruno");
        registro.adicionar(ana);
        registro.adicionar(bruno);
        ModeradorFalso moderador = new ModeradorFalso(false);
        ChatService servico = servico(repositorio, registro, moderador);

        Mensagem salva = servico.enviarMensagem(ana.getUsuario(), "boa noite");

        assertEquals("boa noite", salva.getConteudo());
        assertEquals(List.of("boa noite"), repositorio.buscarUltimas(10).stream()
                .map(Mensagem::getConteudo).toList());
        assertTrue(ana.linhas().contains(salva.formatar()));
        assertTrue(bruno.linhas().contains(salva.formatar()));
        assertEquals(List.of("boa noite"), moderador.mensagensAnalisadas());
    }

    @Test
    void mensagemOfensivaNaoEGravadaNemEnviadaAosOutros() throws Exception {
        InMemoryMensagemRepository repositorio = new InMemoryMensagemRepository();
        ClientRegistry registro = new ClientRegistry();
        FakeConexao ana = conexao("ana");
        FakeConexao bruno = conexao("bruno");
        registro.adicionar(ana);
        registro.adicionar(bruno);
        ChatService servico = servico(repositorio, registro, new ModeradorFalso(true));

        Mensagem resultado = servico.enviarMensagem(ana.getUsuario(), "texto ofensivo");

        assertEquals(null, resultado);
        assertTrue(repositorio.buscarUltimas(10).isEmpty());
        assertTrue(ana.linhas().contains("! sua mensagem foi bloqueada por conter conteúdo ofensivo"));
        assertFalse(bruno.linhas().stream().anyMatch(linha -> linha.contains("texto ofensivo")));
    }

    @Test
    void falhaDeModeracaoLiberaMensagemSemInterromperChat() throws Exception {
        InMemoryMensagemRepository repositorio = new InMemoryMensagemRepository();
        ClientRegistry registro = new ClientRegistry();
        FakeConexao ana = conexao("ana");
        FakeConexao bruno = conexao("bruno");
        registro.adicionar(ana);
        registro.adicionar(bruno);
        ModeradorResiliente moderador = new ModeradorResiliente(texto -> {
            throw new FalhaInferenciaException(429, "limite do modelo atingido");
        });
        ChatService servico = servico(repositorio, registro, moderador);

        Mensagem salva = servico.enviarMensagem(ana.getUsuario(), "mensagem liberada");

        assertEquals("mensagem liberada", salva.getConteudo());
        assertTrue(ana.linhas().contains(salva.formatar()));
        assertTrue(bruno.linhas().contains(salva.formatar()));
    }

    @Test
    void resumoRecebeHistoricoRecenteSemAlterarAsMensagens() throws Exception {
        InMemoryMensagemRepository repositorio = new InMemoryMensagemRepository();
        repositorio.salvar(mensagem("assunto do teste", 1));
        ClientRegistry registro = new ClientRegistry();
        ChatService servico = new ChatService(repositorio, registro, RELOGIO, 20,
                new ModeradorFalso(false), mensagens -> {
                    assertEquals(List.of("assunto do teste"), mensagens.stream()
                            .map(Mensagem::getConteudo).toList());
                    return "Resumo do assunto.";
                });

        assertEquals("Resumo do assunto.", servico.resumirHistorico());
        assertEquals(1, servico.buscarHistorico().size());
    }

    @Test
    void moderacaoLentaNaoSeguraLockQueImpedeOutroBroadcast() throws Exception {
        ClientRegistry registro = new ClientRegistry();
        FakeConexao ana = conexao("ana");
        registro.adicionar(ana);
        CountDownLatch moderacaoIniciada = new CountDownLatch(1);
        CountDownLatch concluirModeracao = new CountDownLatch(1);
        ModeradorDeMensagens moderador = texto -> {
            moderacaoIniciada.countDown();
            try {
                if (!concluirModeracao.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("A liberação do moderador expirou.");
                }
            } catch (InterruptedException excecao) {
                Thread.currentThread().interrupt();
                throw new AssertionError(excecao);
            }
            return false;
        };
        ChatService servico = servico(new InMemoryMensagemRepository(), registro, moderador);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> envio = executor.submit(() -> {
                try {
                    servico.enviarMensagem(ana.getUsuario(), "mensagem");
                } catch (MensagemInvalidaException excecao) {
                    throw new AssertionError(excecao);
                }
            });
            assertTrue(moderacaoIniciada.await(2, TimeUnit.SECONDS));

            executor.submit(() -> servico.broadcast("chat continua"))
                    .get(2, TimeUnit.SECONDS);
            assertTrue(ana.linhas().contains("chat continua"));

            concluirModeracao.countDown();
            envio.get(2, TimeUnit.SECONDS);
        } finally {
            concluirModeracao.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void removidoNaoRecebeBroadcast() throws Exception {
        ClientRegistry registro = new ClientRegistry();
        FakeConexao ativa = conexao("ana");
        FakeConexao removida = conexao("bruno");
        registro.adicionar(ativa);
        registro.adicionar(removida);
        registro.remover(removida);

        servico(new InMemoryMensagemRepository(), registro).broadcast("aviso");

        assertEquals(List.of("aviso"), ativa.linhas());
        assertTrue(removida.linhas().isEmpty());
    }

    @Test
    void falhaDeUmClienteNaoInterrompeBroadcastAosDemais() throws Exception {
        ClientRegistry registro = new ClientRegistry();
        FakeConexao falha = new FakeConexao(usuario("falha"), true);
        FakeConexao saudavel = conexao("saudavel");
        registro.adicionar(falha);
        registro.adicionar(saudavel);

        servico(new InMemoryMensagemRepository(), registro).broadcast("aviso");

        assertEquals(List.of("aviso"), saudavel.linhas());
        assertEquals(List.of(saudavel), registro.snapshot());
    }

    @Test
    void conectarConfirmaNickAntesDoHistoricoEAnunciaEntradaDepois() throws Exception {
        InMemoryMensagemRepository repositorio = new InMemoryMensagemRepository();
        repositorio.salvar(mensagem("primeira", 1));
        repositorio.salvar(mensagem("segunda", 2));
        ClientRegistry registro = new ClientRegistry();
        ChatService servico = servico(repositorio, registro);
        FakeConexao novo = conexao("carlos");

        servico.conectar(novo);

        assertEquals("* Conectado como carlos", novo.linhas().getFirst());
        assertEquals(List.of("primeira", "segunda"), novo.linhas().subList(1, 3).stream()
                .map(linha -> linha.substring(linha.indexOf(": ") + 2)).toList());
        assertEquals("* carlos entrou", novo.linhas().get(3));
    }

    @Test
    void conectarRecusaNomeDuplicadoSemDiferenciarMaiusculas() throws Exception {
        ClientRegistry registro = new ClientRegistry();
        ChatService servico = servico(new InMemoryMensagemRepository(), registro);
        servico.conectar(conexao("ana"));

        assertThrows(NomeEmUsoException.class, () -> servico.conectar(conexao("ANA")));
    }

    @Test
    void desconectarEIdempotenteEAvisaSomenteNaPrimeiraRemocao() throws Exception {
        ClientRegistry registro = new ClientRegistry();
        FakeConexao ana = conexao("ana");
        FakeConexao bruno = conexao("bruno");
        registro.adicionar(ana);
        registro.adicionar(bruno);
        ChatService servico = servico(new InMemoryMensagemRepository(), registro);

        servico.desconectar(ana);
        int linhasDepoisDaPrimeiraSaida = bruno.linhas().size();
        servico.desconectar(ana);

        assertEquals(1, linhasDepoisDaPrimeiraSaida);
        assertEquals(linhasDepoisDaPrimeiraSaida, bruno.linhas().size());
    }

    @Test
    void concorrenciaEntreRegistroRemocaoEBroadcastNaoPerdeLinhasNoClienteEstavel() throws Exception {
        ClientRegistry registro = new ClientRegistry();
        FakeConexao estavel = conexao("estavel");
        registro.adicionar(estavel);
        ChatService servico = servico(new InMemoryMensagemRepository(), registro);
        int trabalhadores = 6;
        int repeticoes = 20;
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(trabalhadores);
        List<java.util.concurrent.Future<?>> tarefas = new ArrayList<>();
        try {
            for (int trabalhador = 0; trabalhador < trabalhadores; trabalhador++) {
                int id = trabalhador;
                tarefas.add(executor.submit(() -> {
                    inicio.await();
                    for (int passo = 0; passo < repeticoes; passo++) {
                        FakeConexao temporaria = conexao("c" + id + "x" + passo);
                        registro.adicionar(temporaria);
                        servico.broadcast("evento-" + id + "-" + passo);
                        registro.remover(temporaria);
                    }
                    return null;
                }));
            }
            inicio.countDown();
            for (java.util.concurrent.Future<?> tarefa : tarefas) {
                tarefa.get(10, TimeUnit.SECONDS);
            }

            assertEquals(trabalhadores * repeticoes, estavel.linhas().size());
            assertEquals(trabalhadores * repeticoes,
                    estavel.linhas().stream().distinct().count());
        } finally {
            executor.shutdownNow();
        }
    }

    private static ChatService servico(InMemoryMensagemRepository repositorio, ClientRegistry registro) {
        return servico(repositorio, registro, new ModeradorFalso(false));
    }

    private static ChatService servico(InMemoryMensagemRepository repositorio, ClientRegistry registro,
                                       ModeradorDeMensagens moderador) {
        return new ChatService(repositorio, registro, RELOGIO, 20, moderador);
    }

    private static FakeConexao conexao(String nome) {
        return new FakeConexao(usuario(nome));
    }

    private static Usuario usuario(String nome) {
        try {
            return new Usuario(nome);
        } catch (UsuarioInvalidoException excecao) {
            throw new AssertionError(excecao);
        }
    }

    private static Mensagem mensagem(String texto, int minuto) throws Exception {
        return new Mensagem(null, usuario("ana"), texto,
                java.time.LocalDateTime.of(2026, 10, 13, 9, minuto));
    }
}
