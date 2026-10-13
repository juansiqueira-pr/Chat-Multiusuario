# ESTADO DO PROJETO

> Mapa compacto para o agente **não precisar varrer o repositório**. **Sobrescreva** a cada tarefa (não acumule histórico aqui; o histórico vai em `RELATORIO_DESENVOLVIMENTO.md`). Mantenha em até ~100 linhas.

**Atualizado em:** 2026-10-13 · **Fase atual:** 7 concluída (cliente, integração e documentação)
**Marco "núcleo obrigatório completo":** ✅ atingido
**Testes:** unitários 31 · integração 7 · `mvn -q test` verde em 3 execuções seguidas.
**Próximo passo:** gravar o vídeo de demonstração (atividade do aluno); nenhum código pendente.

Legenda: ⬜ pendente · 🟡 em andamento · ✅ pronto e testado

## Requisitos obrigatórios

| # | Requisito | Evidência (classe/teste) | Status |
|---|---|---|---|
| 1 | `Servidor` com `ServerSocket` e `accept()` | `Servidor`; `ServidorIntegracaoTest` | ✅ |
| 2 | Uma thread por conexão | `Servidor` (`cliente-N`); integração TCP | ✅ |
| 3 | `ClienteHandler implements Runnable` | `ClienteHandler`; integração TCP | ✅ |
| 4 | Coleção sincronizada de clientes | `ClientRegistry`; `ClientRegistryTest` | ✅ |
| 5 | Broadcast para todos | `ChatService`; `ChatServiceTest`, `ServidorIntegracaoTest` | ✅ |
| 6 | `Mensagem` (remetente, conteúdo, horário) | `Mensagem`; `MensagemTest` | ✅ |
| 7 | Persistência JDBC | `JdbcMensagemRepository`; `JdbcMensagemRepositoryIntegracaoTest` | ✅ |
| 8 | Repository isola o SQL | `MensagemRepository`; `ArquiteturaTest` | ✅ |
| 9 | Histórico ao conectar | `ChatService.conectar`; unitário e integração TCP | ✅ |
| 10 | Desconexão segura | `ClienteHandler.finally`; cenário abrupto TCP | ✅ |
| 11 | ≥ 5 testes JUnit sem rede real | 31 testes unitários/arquitetura | ✅ |
| 12 | Código organizado em pacotes | pacotes `br.unicesumar.chat`; `ArquiteturaTest` | ✅ |

Entregáveis: código ✅ · `schema.sql` ✅ · README ✅ · relatório final ✅ · vídeo ⬜ (feito pelo aluno)

## Rubrica de 5,0 pontos — evidências

| Critério | Pontos | Evidência |
|---|---:|---|
| Múltiplos clientes simultâneos | 1,3 | `Servidor`, `ClienteHandler`, integração TCP |
| Broadcast ordenado e seguro | 1,2 | `ChatServiceTest`, teste concorrente e integração de 3 clientes |
| Persistência JDBC | 1,0 | `JdbcMensagemRepositoryIntegracaoTest` |
| Pacotes e responsabilidades | 0,5 | `ArquiteturaTest` |
| Testes unitários de lógica | 0,5 | 31 unitários/arquitetura; sem rede real |
| Organização e relatório | 0,5 | README, estado e relatório final |

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
| server/Protocolo | Parsing das linhas de entrada | ✅ |
| server/ClienteHandler | Uma conexão; `Runnable` + `ConexaoCliente` | ✅ |
| server/Servidor | `ServerSocket`, `accept()`, `main` (montagem), `iniciar/parar` | ✅ |
| client/ClienteChat | Cliente de console com duas threads (teclado e socket) | ⬜ |
| config/Config | Lê `chat.properties` com valores padrão | ✅ |
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
