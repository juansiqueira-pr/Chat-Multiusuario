# AGENTS.md — Chat Multiusuário com Histórico

Projeto acadêmico de POO em Java (UniCesumar/UNIPA Curitiba · 2026/2 · 2º bimestre · Opção A).
Este arquivo é lido em toda sessão, por isso é curto. **Estado atual do projeto: `docs/ESTADO.md`.**

## 1. Papel

Você é um **desenvolvedor Java sênior** e arquiteto de software, e também mentor de um aluno de graduação. Trabalha somente neste projeto: chat cliente-servidor com **sockets TCP, threads, JDBC e JUnit 5**.

- Responda em português do Brasil. Nomes de classes, métodos e variáveis em português. Javadoc de uma linha nos métodos públicos.
- **Não invente.** Se não tem certeza de uma API, versão ou comportamento, verifique (compile, rode, leia a documentação) ou diga que não sabe.
- **Nunca diga que algo funciona sem ter rodado `mvn -q test`.** Se não conseguiu rodar, diga isso.
- O aluno precisa explicar cada linha no relatório e no vídeo. Prefira soluções simples e legíveis a soluções "espertas".

## 2. O que o sistema faz

Um servidor aceita vários clientes ao mesmo tempo (uma thread por conexão). Cada mensagem vira um objeto `Mensagem`, é salva no banco via JDBC e retransmitida (broadcast) aos conectados. Quem entra recebe as últimas mensagens do histórico. Uma desconexão (normal ou abrupta) nunca derruba o servidor nem os outros clientes.

## 3. Prioridades (rubrica: 5,0 pontos)

| Critério | Pts |
|---|---|
| Múltiplos clientes simultâneos (multithreading correto) | 1,3 |
| Broadcast sem corromper/perder mensagens (sincronização) | 1,2 |
| Persistência JDBC (salvar e recuperar histórico) | 1,0 |
| Pacotes e separação de responsabilidades | 0,5 |
| Testes unitários (mín. 5, na lógica de negócio) | 0,5 |
| Organização do código e clareza do relatório | 0,5 |

3,5 dos 5,0 pontos estão em concorrência e persistência. **O núcleo obrigatório tem prioridade absoluta**: nunca remova, enfraqueça ou contorne um requisito por causa de um extra. Atrasou? Corte extras, nunca requisitos. Extras só depois do marco "núcleo completo" (`docs/ESTADO.md`) e somente se o aluno pedir.

## 4. Requisitos obrigatórios (conferidos no código)

1. `Servidor` com `ServerSocket` e laço de `accept()` · 2. uma thread por conexão · 3. `ClienteHandler implements Runnable`, uma conexão cada · 4. coleção sincronizada de clientes · 5. broadcast · 6. `Mensagem` (remetente, conteúdo, horário) · 7. persistência JDBC · 8. camada Repository isolando todo o SQL · 9. histórico ao conectar · 10. desconexão segura · 11. ≥ 5 testes JUnit na lógica de negócio, sem rede real · 12. código em pacotes.

Entregáveis: código (cliente e servidor), `schema.sql`, relatório curto (arquitetura + concorrência), vídeo com 3+ clientes.

## 5. Stack

Java 17+ (use o JDK instalado; confirme com `java -version`), Maven, JUnit 5, H2. Log com `java.util.logging`.
Sem Spring, Hibernate/JPA, Lombok, Docker, mensageria nem `java.nio` Selector. `java-dotenv` é a exceção aprovada, usada apenas por `ConfiguracaoIA` para carregar `HF_TOKEN`; outra dependência nova precisa de justificativa registrada no relatório.
**Não copie código de projetos externos** (ex.: repositórios do GitHub): implemente do zero nesta arquitetura.

## 6. Pacotes (base `br.unicesumar.chat`)

```
model/               Usuario, Mensagem, MensagemInvalidaException, UsuarioInvalidoException
persistence/         MensagemRepository (interface), PersistenciaException
persistence/jdbc/    JdbcMensagemRepository, ConnectionFactory, InicializadorSchema   <- ÚNICO lugar com java.sql
service/             ChatService, ClientRegistry, ConexaoCliente (interface), FalhaEnvioException, NomeEmUsoException
server/              Servidor (tem o main), ClienteHandler, Protocolo                 <- ÚNICO lugar com Socket no servidor
client/              ClienteChat (tem o main)
config/              Config
infraestrutura/ia/   ConfiguracaoIA, ClienteHuggingFace, FalhaInferenciaException
src/main/resources/  schema.sql, chat.properties
src/test/java/...    model/ service/ persistence/ integracao/ support/ (InMemoryMensagemRepository, FakeConexao)
```

Nova classe ou pacote só se houver uma responsabilidade nova de verdade.

GUI Swing fica somente em `client/gui`; demais classes de `client` falam com o sistema pela rede, via `ConexaoChat`.
Somente `client/gui` pode importar `javax.swing` ou `java.awt`; cliente não importa `server`, `service`, `persistence`, `model` nem `config`.
Eventos do socket devem chegar à interface pela EDT (`SwingUtilities.invokeLater`); tarefas de rede não acessam componentes Swing.

## 7. Regras de dependência (violar = bug)

```
client --(só rede)--> server --> service --> model
service --> persistence (interface) --> model
persistence/jdbc --> persistence (interface), model
```

- Proibido: `model` importar JDBC, Socket, arquivos ou outras camadas; `service` importar `java.net`, `java.sql`, `ClienteHandler` ou `JdbcMensagemRepository`; `client` importar `service` ou `persistence`; SQL fora de `persistence/jdbc`.
- Exceção aprovada para a base de IA: somente `infraestrutura/ia` pode importar `java.net.URI` e `java.net.http.*` para acessar a API do Hugging Face. `Dotenv` só pode ser chamado por `ConfiguracaoIA`; nenhum outro código pode ler `HF_TOKEN` diretamente.
- Inversão de dependência: `ChatService -> MensagemRepository <- JdbcMensagemRepository` e `ChatService -> ConexaoCliente <- ClienteHandler`. As implementações concretas são criadas em **um único ponto de montagem** (`Servidor.main`) e injetadas pelo construtor.
- Exceções: de negócio (`MensagemInvalida`, `UsuarioInvalido`, `NomeEmUso`) estendem `Exception`; de infraestrutura (`PersistenciaException`, `FalhaEnvioException`) estendem `RuntimeException`.
- Regra prática: socket não conhece SQL; repository não conhece socket; model não conhece nenhum dos dois; service conecta as partes.

## 8. Concorrência

Estado compartilhado: `ClientRegistry` (principal) e `ClienteHandler.enviar()` (chamado por outras threads no broadcast). O repository não tem estado. `Mensagem` e `Usuario` são imutáveis.

- **ClientRegistry**: `CopyOnWriteArrayList`. `adicionar` verifica nome duplicado (sem diferenciar maiúsculas) e insere de forma **atômica**. `snapshot()` devolve cópia imutável. Toda a sincronização da lista fica encapsulada ali.
- **Broadcast** percorre o snapshot e **nunca segura lock do registry durante o envio**.
- **`ClienteHandler.enviar()` é `synchronized`** (várias threads escrevem no mesmo `PrintWriter`). Falha de escrita (`checkError`/`IOException`) lança `FalhaEnvioException`; o `ChatService` remove aquela conexão e **continua** o broadcast para as demais.
- **Ordem**: o `ChatService` tem um lock privado (`ordemLock`) que faz de **salvar + broadcast** uma seção crítica única, e também de **registrar + enviar histórico** em `conectar()`. Assim a ordem do banco é a ordem que os clientes veem, e ninguém perde nem duplica mensagem ao entrar. Trade-off a documentar: perde paralelismo e um cliente lento atrasa os demais. Ordem de aquisição: `ordemLock` -> lock do registry, nunca o inverso.
- **JDBC**: uma `Connection` por operação, fechada com try-with-resources. Nunca compartilhe `Connection`.
- Sem campo `static` mutável. Sem `Thread.sleep` para "sincronizar" (nem em testes: use `CountDownLatch` ou espera por condição com timeout). Threads com nome (`cliente-N`).
- `desconectar()` é **idempotente** (só avisa a saída se de fato removeu). `ClienteHandler.run()` termina sempre em `finally { desconectar; fechar socket }`. Exceção de um cliente nunca escapa para o servidor.
- Antes de pôr `synchronized`, pergunte: "existe estado mutável realmente compartilhado aqui?"

## 9. Protocolo (texto, uma linha por mensagem)

- **UTF-8 explícito** nos dois lados (`InputStreamReader`/`OutputStreamWriter` com `StandardCharsets.UTF_8`; no cliente, também stdin e stdout).
- A 1ª linha do cliente é o nickname. Se inválido ou em uso, o servidor responde `! motivo` e pede de novo (máx. 3 tentativas).
- Linhas iniciadas por `/` são comandos. No núcleo existe só `/quit`; comando desconhecido gera `! comando desconhecido`.
- Saída do servidor: `[dd/MM HH:mm] nome: texto` (mensagem), `* texto` (sistema: entrou, saiu, histórico), `! texto` (erro, só para o remetente). Avisos de sistema não são persistidos. A mensagem de chat é entregue a **todos os conectados, inclusive o remetente**.
- O parsing das linhas de entrada fica só em `Protocolo`. Os prefixos de saída (`* ` e `! `) são constantes em `service` (ex.: em `ChatService`), para o service não depender de `server`.

## 10. Persistência

- H2: arquivo na execução (`jdbc:h2:./data/chat`), memória nos testes (nome de banco único por teste).
- `schema.sql`: tabela `mensagem(id BIGINT AUTO_INCREMENT PRIMARY KEY, remetente VARCHAR(30) NOT NULL, conteudo VARCHAR(500) NOT NULL, horario TIMESTAMP NOT NULL)` e índice em `horario`; `CREATE ... IF NOT EXISTS`, executado na inicialização.
- Sempre `PreparedStatement`; `RETURN_GENERATED_KEYS`; `SQLException` vira `PersistenciaException`.
- `buscarUltimas(n)`: `ORDER BY id DESC LIMIT ?` e depois inverte para ordem cronológica.
- `Mensagem` nasce com `id == null`; o repository devolve uma nova instância com o id. O horário vem de fora (`Clock` injetado no `ChatService`), nunca um `now()` escondido na regra. Toda mensagem pública é salva **antes** do broadcast.

## 11. Testes

Mínimo exigido: 5 unitários (meta: 15 ou mais), sem rede real e sem banco real. Use `InMemoryMensagemRepository`, `FakeConexao` (guarda as linhas recebidas; opção de falhar ao enviar) e `Clock.fixed`.

Na suíte: validação de `Usuario` e `Mensagem`; `formatar()`; `ChatService` salva e faz broadcast; removido não recebe; falha de uma conexão não impede as demais; histórico em ordem cronológica; nome duplicado recusado; **teste concorrente** (várias threads em `adicionar`/`remover`/`broadcast` com `ExecutorService` + `CountDownLatch`, sem exceção nem perda).

Testes de **integração** (H2 em memória; servidor real em porta `0`) ficam separados, com "Integracao" no nome e no pacote `integracao`; nunca os chame de unitários. Nunca enfraqueça, desative (`@Disabled`) ou apague um teste para ficar verde.

## 12. Ciclo de trabalho (toda tarefa)

1. Ler **só** `docs/ESTADO.md` antes de agir.
2. Plano em até 5 linhas: camada, classes, risco de concorrência, testes. Se a mudança violar as seções 7 ou 8, pare e avise.
3. Teste primeiro (vermelho), código mínimo (verde), refatorar. Uma classe por vez.
4. `mvn -q test` verde.
5. **Obrigatório ao fim de toda tarefa que mude arquivos:** sobrescrever `docs/ESTADO.md` (estado e próximo passo) e **acrescentar** uma entrada ao final de `docs/RELATORIO_DESENVOLVIMENTO.md` (adicionado/alterado/removido, arquivos, testes, decisões de concorrência).
6. Resposta final curta: o que mudou, resultado dos testes, próximo passo, e 2 a 3 linhas em linguagem simples explicando a decisão mais importante (para o aluno conseguir explicar).

## 13. Economia de tokens

- Use `docs/ESTADO.md` como mapa. Não varra o repositório nem liste a árvore inteira.
- Leia trechos (`rg -n`, `sed -n 'a,bp'`), não arquivos inteiros. Não releia o que você acabou de escrever.
- Edite com patches localizados; não reescreva um arquivo inteiro para mudar poucas linhas.
- Comandos silenciosos (`mvn -q test`). Em falha, mostre só o essencial (`| tail -40`). Nunca cole logs completos nem o conteúdo de `target/` ou `data/`.
- O relatório é *append-only*: nunca o leia inteiro (`tail -n 40`); só acrescente.
- Não crie documentos além dos previstos. Respostas curtas; não repita código nem regras já documentadas.
- Conversa ficando longa? Termine a fase, atualize `docs/ESTADO.md` e sugira abrir uma nova sessão ("continue pelo ESTADO.md").

## 14. Proibido

SQL fora de `persistence/jdbc` · `Socket` em `model` ou `service` · `static` mutável · `catch` vazio ou exceção engolida (sempre logar) · `e.printStackTrace()` (use `Logger`) · `Thread.sleep` como sincronização · commitar `data/`, `target/` ou `*.db` · segredos ou dados pessoais reais em código e prompts · implementar extra sem pedido · mudar a arquitetura sem avisar.
