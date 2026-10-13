# ESTADO DO PROJETO

> Mapa compacto para o agente **não precisar varrer o repositório**. **Sobrescreva** a cada tarefa (não acumule histórico aqui; o histórico vai em `RELATORIO_DESENVOLVIMENTO.md`). Mantenha em até ~100 linhas.

**Atualizado em:** 2026-10-13 · **Fase atual:** 5 concluída (registry e serviço)
**Marco "núcleo obrigatório completo":** ⬜ não atingido (previsto no cronograma para 02/11)
**Testes:** unitários 24 · integração 2 · último resultado: `mvn -q test` verde.
**Próximo passo:** Fase 6 — protocolo de linha, configuração e handler do cliente.

Legenda: ⬜ pendente · 🟡 em andamento · ✅ pronto e testado

## Requisitos obrigatórios

| # | Requisito | Onde | Status |
|---|---|---|---|
| 1 | `Servidor` com `ServerSocket` e laço `accept()` | server/Servidor | ⬜ |
| 2 | Uma thread por conexão | server/Servidor | ⬜ |
| 3 | `ClienteHandler implements Runnable` | server/ClienteHandler | ⬜ |
| 4 | Coleção sincronizada de clientes | service/ClientRegistry | ✅ |
| 5 | Broadcast para todos | service/ChatService | ✅ |
| 6 | `Mensagem` (remetente, conteúdo, horário) | model/Mensagem | ✅ |
| 7 | Persistência JDBC | persistence/jdbc/JdbcMensagemRepository | ✅ |
| 8 | Repository isolando o SQL | persistence/MensagemRepository | ✅ |
| 9 | Histórico ao conectar | service/ChatService.conectar | ✅ |
| 10 | Desconexão segura | server/ClienteHandler (finally) + ChatService.desconectar | 🟡 |
| 11 | ≥ 5 testes JUnit sem rede real | src/test/... | ✅ |
| 12 | Código em pacotes | todos | ⬜ |

Entregáveis: código ⬜ · `schema.sql` ⬜ · README ⬜ · relatório final ⬜ · vídeo ⬜ (feito pelo aluno)

## Mapa de classes (`src/main/java/br/unicesumar/chat/...`)

| Arquivo | Responsabilidade | Status |
|---|---|---|
| model/Usuario | Nome validado; imutável; equals/hashCode sem diferenciar maiúsculas | ✅ |
| model/Mensagem | Remetente, conteúdo, horário; valida; `formatar()`; `comId(Long)` | ✅ |
| model/MensagemInvalidaException, UsuarioInvalidoException | Erros de negócio (checked) | ✅ |
| persistence/MensagemRepository | Contrato: `salvar`, `buscarUltimas(n)` | ✅ |
| persistence/PersistenciaException | Falha de persistência (runtime) | ✅ |
| persistence/jdbc/ConnectionFactory | Abre uma `Connection` por operação | ✅ |
| persistence/jdbc/InicializadorSchema | Executa `schema.sql` na inicialização | ✅ |
| persistence/jdbc/JdbcMensagemRepository | Único lugar com SQL | ✅ |
| service/ConexaoCliente | Abstração de quem recebe texto: `getUsuario`, `enviar` | ✅ |
| service/FalhaEnvioException, NomeEmUsoException | Falha de envio (runtime) / nome em uso (checked) | ✅ |
| service/ClientRegistry | Coleção compartilhada e thread-safe de conexões | ✅ |
| service/ChatService | Regras do chat: conectar, desconectar, enviar, broadcast, histórico | ✅ |
| server/Protocolo | Parsing das linhas de entrada | ⬜ |
| server/ClienteHandler | Uma conexão; `Runnable` + `ConexaoCliente` | ⬜ |
| server/Servidor | `ServerSocket`, `accept()`, `main` (montagem), `iniciar/parar` | ⬜ |
| client/ClienteChat | Cliente de console com duas threads (teclado e socket) | ⬜ |
| config/Config | Lê `chat.properties` com valores padrão | ⬜ |
| resources/schema.sql, chat.properties | Script da tabela; configuração | ✅ |
| test/support/InMemoryMensagemRepository, FakeConexao | Dublês de teste | ✅ |

## Decisões fixas

- Pacote base `br.unicesumar.chat` · H2 (arquivo `./data/chat`; memória nos testes) · porta padrão **5555** · histórico padrão **20** mensagens.
- Nome de usuário: não vazio, sem espaços, até 20 caracteres, **único sem diferenciar maiúsculas**. Conteúdo: até 500 caracteres, com `trim`.
- Mensagem de chat vai a **todos, inclusive o remetente**. Avisos de entrada/saída não são persistidos.
- `ChatService.ordemLock`: salvar+broadcast e registrar+histórico são seções críticas (ordem de locks: `ordemLock` -> registry).
- Núcleo só tem o comando `/quit`. Extras (F1, M4, D10...) ficam para depois do marco.

## Comandos

- Testar: `mvn -q test`
- Servidor: `mvn -q exec:java -Dexec.mainClass=br.unicesumar.chat.server.Servidor` *(validar na Fase 7)*
- Cliente: `mvn -q exec:java -Dexec.mainClass=br.unicesumar.chat.client.ClienteChat -Dexec.args="localhost 5555 ana"` *(validar na Fase 7)*

## Pendências / bugs conhecidos

Nenhum.
