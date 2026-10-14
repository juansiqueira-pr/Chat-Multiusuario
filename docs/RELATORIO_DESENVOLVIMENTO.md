# Relatório de Desenvolvimento — Chat Multiusuário com Histórico

> Documento vivo. A **Parte 1** é um painel curto, editável. A **Parte 2** é um log *append-only*: só acrescente entradas **ao final**; nunca reescreva entradas antigas. O agente **não deve ler o arquivo inteiro**: use `tail -n 40` para ver o último registro. Este material alimenta o relatório final (`docs/RELATORIO_FINAL.md`).

---

## Parte 1 — Painel fixo

### 1.1 Declaração de uso de IA

- Ferramentas: Codex (VS Code) para implementação; Claude para planejamento da arquitetura. *(aluno: confirmar ou ajustar)*
- Como foi validado: testes JUnit a cada mudança, revisão de concorrência (Fase 6) e leitura do código pelo aluno.
- Partes que o aluno revisou e sabe explicar: *(preencher ao final)*

### 1.2 Objetos compartilhados entre threads e proteção

| Objeto | Quem acessa | Risco | Proteção | Situação |
|---|---|---|---|---|
| `ClientRegistry` (lista de conexões) | todas as threads de cliente | modificação durante o broadcast; nome duplicado | `CopyOnWriteArrayList` + `adicionar`/`remover` atômicos + `snapshot()` | implementado; `ClientRegistryTest` |
| `Servidor` (socket e ciclo de vida) | thread accept e thread que chama `parar()` | aceitar conexão durante ou após a parada | estado `volatile`; transições protegidas por `cicloLock` | implementado; integração TCP |
| `Servidor.handlersAtivos` | accept, parada e handlers | coleção alterada durante encerramento | conjunto `ConcurrentHashMap.newKeySet()` | implementado; `ServidorIntegracaoTest` |
| `ClienteHandler.usuario` / `saida` / `enviar()` | handler, serviço e broadcasts | referência não publicada ou linhas intercaladas | referências `volatile`; envio `synchronized` e UTF-8 | implementado; integração TCP |
| `ClienteChat.socket` | thread do teclado e thread receptora | encerramento enquanto a outra direção está em uso | entrada e saída têm um único escritor cada; fechar socket interrompe ambas; falhas são tratadas | implementado; servidor real validado |
| `ChatService` (salvar+broadcast; conectar+histórico) | todas as threads de cliente | ordem do banco diferente da exibida; perder mensagem ao entrar | `ordemLock` | implementado; `ChatServiceTest` |
| `Connection` JDBC | threads que salvam/consultam | uso concorrente de uma conexão | uma `Connection` por operação | implementado em `JdbcMensagemRepository` |
| `Mensagem`, `Usuario` | todas | nenhum | imutáveis | implementado; testes do model |

### 1.3 Race conditions identificadas

| # | Situação | Como ocorreria | Como foi evitada | Teste que comprova |
|---|---|---|---|---|
| 1 | Duas conexões tentam registrar o mesmo nome ao mesmo tempo | ambas poderiam passar na verificação e aparecer online com o mesmo nome | `adicionar` e `remover` são atômicos; comparação usa `Usuario.equals` | `ClientRegistryTest.adicaoConcorrenteDoMesmoNomeEAtomica` |
| 2 | Alterações do registry coincidem com broadcast | iteração direta da lista poderia lançar exceção ou pular uma conexão estável | `CopyOnWriteArrayList`; broadcast percorre `snapshot()` | `ChatServiceTest.concorrenciaEntreRegistroRemocaoEBroadcastNaoPerdeLinhasNoClienteEstavel` |

### 1.4 Extras implementados e fora do escopo

Somente o núcleo obrigatório. Extras implementados: nenhum até o momento.

---

## Parte 2 — Log de alterações (append-only)

Formato de cada entrada (copie e preencha; mantenha curto):

```
### AAAA-MM-DD · Fase N — título curto
- **Adicionado:** ...
- **Alterado:** ...
- **Removido:** ...
- **Arquivos:** ...
- **Testes:** X passando / Y falhando (unit: a · integração: b)
- **Decisão / concorrência:** ...
- **Próximo passo:** ...
```

---

### 2026-10-12 · Fase 0 — Documentação inicial

- **Adicionado:** `AGENTS.md` (regras permanentes do agente), `docs/ESTADO.md` (mapa compacto), `docs/RELATORIO_DESENVOLVIMENTO.md` (este arquivo), `PROMPT_INICIAL_CODEX.md` (prompt de partida).
- **Alterado:** nada (projeto ainda sem código).
- **Removido:** nada.
- **Arquivos:** os quatro acima.
- **Testes:** não se aplica.
- **Decisão / concorrência:** arquitetura em camadas definida (model, persistence, service, server, client, config), com inversão de dependência via `MensagemRepository` e `ConexaoCliente`.
- **Próximo passo:** Fase 1 (esqueleto Maven e configuração).

### 2026-10-13 · Fase 1 — Esqueleto Maven e recursos

- **Adicionado:** estrutura dos pacotes de produção e teste, `pom.xml`, `.gitignore`, `schema.sql` e `chat.properties`.
- **Alterado:** `docs/ESTADO.md` para registrar a Fase 1 concluída e a Fase 2 como próximo passo.
- **Removido:** nada nesta fase.
- **Arquivos:** `pom.xml`, `.gitignore`, `src/main/resources/schema.sql`, `src/main/resources/chat.properties`, diretórios `src/main/java` e `src/test/java`, `docs/ESTADO.md`.
- **Testes:** `mvn -q test` concluído com sucesso; 0 unitários e 0 integração, pois ainda não há classes de teste.
- **Decisão / concorrência:** JDK 21 define `maven.compiler.release=21`; JUnit Jupiter 5.14.2 e H2 2.5.252 foram resolvidos pelo Maven Central. O painel 1.2 mantém os mecanismos de concorrência como planejados até as respectivas classes e testes existirem.
- **Próximo passo:** Fase 2 — `UsuarioTest` antes de `Usuario`.

### 2026-10-13 · Fase 2 — Modelo Usuario

- **Adicionado:** testes de validação, igualdade e hash; `Usuario` imutável e `UsuarioInvalidoException`.
- **Alterado:** configuração dos plugins Maven Compiler 3.16.0 e Surefire 3.6.0 para compilar com release 21 e descobrir testes JUnit 5.
- **Removido:** nada.
- **Arquivos:** `pom.xml`, `src/main/java/br/unicesumar/chat/model/Usuario.java`, `UsuarioInvalidoException.java`, `src/test/java/br/unicesumar/chat/model/UsuarioTest.java`, `docs/ESTADO.md`.
- **Testes:** 5 unitários passando; 0 integração. A primeira execução falhou como esperado porque `Usuario` ainda não existia.
- **Decisão / concorrência:** nomes comparam-se usando `Locale.ROOT`, evitando diferenças dependentes da localidade. Ainda não há estado compartilhado implementado.
- **Próximo passo:** Fase 3 — modelar e testar `Mensagem`.

### 2026-10-13 · Fase 3 — Modelo Mensagem

- **Adicionado:** testes para validação, formatação, conteúdo aparado, cópia com ID e imutabilidade; `Mensagem`, `MensagemInvalidaException`.
- **Alterado:** `docs/ESTADO.md` e painel de objetos imutáveis.
- **Removido:** nada.
- **Arquivos:** `src/main/java/br/unicesumar/chat/model/Mensagem.java`, `MensagemInvalidaException.java`, `src/test/java/br/unicesumar/chat/model/MensagemTest.java`, `docs/ESTADO.md`, `docs/RELATORIO_DESENVOLVIMENTO.md`.
- **Testes:** 10 unitários passando; 0 integração. A primeira execução falhou como esperado antes das classes de mensagem.
- **Decisão / concorrência:** `Mensagem` e `Usuario` são imutáveis, portanto podem ser compartilhados sem sincronização. Horário é recebido pelo construtor e não gerado internamente.
- **Próximo passo:** Fase 4 — interfaces de persistência e implementação JDBC.

### 2026-10-13 · Fase 4 — Persistência JDBC

- **Adicionado:** contrato `MensagemRepository`, `PersistenciaException`, repositório de teste em memória com `AtomicLong`, fábrica de conexões, inicializador do schema e repository JDBC; testes unitários e integração H2.
- **Alterado:** `docs/ESTADO.md` marca requisitos 6, 7, 8 e 11 como concluídos.
- **Removido:** nada.
- **Arquivos:** `src/main/java/br/unicesumar/chat/persistence/**`, `src/main/java/br/unicesumar/chat/persistence/jdbc/**`, `src/test/java/br/unicesumar/chat/support/**`, `src/test/java/br/unicesumar/chat/integracao/JdbcMensagemRepositoryIntegracaoTest.java`, `docs/ESTADO.md`, `docs/RELATORIO_DESENVOLVIMENTO.md`.
- **Testes:** 13 unitários e 2 de integração H2 passando; o teste JDBC usa URL com UUID, inicializa o schema de modo idempotente e limpa a tabela em cada preparação. `rg -l "java.sql" src/main` lista só `persistence/jdbc`.
- **Decisão / concorrência:** cada operação abre e fecha sua própria `Connection` com try-with-resources; SQL fica em `persistence.jdbc`. O repositório em memória sincroniza suas operações e gera IDs com `AtomicLong`.
- **Próximo passo:** Fase 5 — registry, abstração de conexão e regras concorrentes do serviço.

### 2026-10-13 · Fase 5 — Registry e serviço de chat

- **Adicionado:** `ConexaoCliente`, exceções de serviço, `ClientRegistry`, `ChatService`, `FakeConexao`, testes do registry e serviço com concorrência.
- **Alterado:** painel 1.2/1.3 registra objetos protegidos e duas races com seus testes; `docs/ESTADO.md` atualiza requisitos 4, 5, 9 e serviço.
- **Removido:** nada.
- **Arquivos:** `src/main/java/br/unicesumar/chat/service/**`, `src/test/java/br/unicesumar/chat/service/**`, `src/test/java/br/unicesumar/chat/support/FakeConexao.java`, `docs/ESTADO.md`, `docs/RELATORIO_DESENVOLVIMENTO.md`.
- **Testes:** 24 unitários e 2 de integração passando. O teste concorrente usa 6 workers, `ExecutorService` e `CountDownLatch`, sem `Thread.sleep`; cada linha chega ao cliente estável.
- **Decisão / concorrência:** `ordemLock` protege persistir+broadcast, conectar+histórico e desconectar+aviso. O broadcast usa snapshot; falhas removem a conexão e são registradas com `Logger`.
- **Próximo passo:** Fase 6 — protocolo, configuração e conexão TCP do servidor.

### 2026-10-13 · Fase 6 — Protocolo e servidor TCP

- **Adicionado:** `Protocolo`, `Config`, `ClienteHandler` e `Servidor` com accept não bloqueante, handlers concorrentes e montagem JDBC no `main`.
- **Alterado:** prefixos do protocolo como constantes em `ChatService`; `docs/ESTADO.md` marca requisitos 1, 2, 3 e 10 como concluídos.
- **Removido:** nada.
- **Arquivos:** `src/main/java/br/unicesumar/chat/config/Config.java`, `src/main/java/br/unicesumar/chat/server/**`, `src/main/java/br/unicesumar/chat/service/ChatService.java`, `src/test/java/br/unicesumar/chat/config/ConfigTest.java`, `src/test/java/br/unicesumar/chat/server/ProtocoloTest.java`, `docs/ESTADO.md`, `docs/RELATORIO_DESENVOLVIMENTO.md`.
- **Testes:** 30 unitários e 2 de integração JDBC passando; cobertura TCP real fica para a Fase 7.
- **Decisão / concorrência:** servidor guarda handlers em `ConcurrentHashMap.newKeySet()`, fecha socket e handlers ao parar e cria `Thread(handler, "cliente-N")`; a escrita de cada handler é sincronizada.
- **Próximo passo:** Fase 7 — cliente console, cenários com sockets reais, README e relatório final.

### 2026-10-13 · Fase 7 — Cliente, integração e entregáveis

- **Adicionado:** `ClienteChat` com duas threads UTF-8, cinco cenários TCP reais, `ArquiteturaTest`, README, launch configurations e relatório final com diagramas.
- **Alterado:** `docs/ESTADO.md` marca núcleo completo, requisitos e rubrica com evidências; painel 1.2 inclui estado compartilhado do cliente de console.
- **Removido:** nada.
- **Arquivos:** `src/main/java/br/unicesumar/chat/client/ClienteChat.java`, `src/test/java/br/unicesumar/chat/integracao/ServidorIntegracaoTest.java`, `src/test/java/br/unicesumar/chat/support/ArquiteturaTest.java`, `README.md`, `.vscode/launch.json`, `docs/ESTADO.md`, `docs/RELATORIO_FINAL.md`, `docs/RELATORIO_DESENVOLVIMENTO.md`.
- **Testes:** 31 unitários/arquitetura e 7 integrações (2 JDBC + 5 TCP), todos passando; `mvn -q test` passou três vezes seguidas. `mvn -q package` compilou e passou; `mvn -q exec:java` iniciou servidor em segundo plano, porta 5555 confirmada e processo encerrado.
- **Decisão / concorrência:** nenhum plugin exec foi declarado no `pom.xml`, pois o comando de execução funcionou. `mvn -q clean package` não removeu `target/classes/client` no Windows por arquivo em uso; o README documenta `mvn -q package`, que foi validado.
- **Próximo passo:** núcleo concluído; gravação do vídeo de demonstração pelo aluno.

### 2026-10-13 · Fase 8A — Rede e lógica de cliente sem Swing

- **Adicionado:** `LinhaServidor`, `OuvinteChat`, `ConexaoChat` e `ModeloConversa`; o cliente de console agora reutiliza a conexão compartilhada.
- **Alterado:** `ChatService.conectar()` envia `* Conectado como <nome>` antes do histórico, sob `ordemLock`; testes unitário e TCP validam a ordem. O console mantém stdin/stdout em UTF-8 e a leitura de rede numa thread nomeada.
- **Arquivos:** `src/main/java/br/unicesumar/chat/client/**`, `src/main/java/br/unicesumar/chat/service/ChatService.java`, testes em `src/test/java/br/unicesumar/chat/client/**`, `src/test/java/br/unicesumar/chat/service/ChatServiceTest.java`, `src/test/java/br/unicesumar/chat/integracao/ServidorIntegracaoTest.java`, `docs/ESTADO.md`, este relatório.
- **Testes:** baseline 38/38; após a fase, 49/49 (40 unitários/arquitetura e 9 integrações, incluindo 2 novos testes TCP de `ConexaoChat`). `mvn -q test` passou.
- **Concorrência:** `ConexaoChat` separa leitura em `chat-rede-leitura`, sincroniza escrita no `PrintWriter` e publica eventos sem conhecer Swing; `ModeloConversa` sincroniza acesso à lista e devolve cópias imutáveis. A linha de aceite é enviada no mesmo `ordemLock` que protege o histórico, mantendo o cliente informado antes das mensagens antigas.
- **Próximo passo:** Fase 8B — componentes visuais Java2D e geração de capturas em modo headless.
