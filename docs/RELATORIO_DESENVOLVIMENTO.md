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
| `ConexaoChat.socket` / `saida` / `encerrada` | executor de conexão/envio, leitor de rede e thread que solicita fechamento | envio simultâneo ou cancelamento durante connect/leitura | `AtomicBoolean`, escrita e fechamento sob o monitor do escritor, socket fechado para interromper leitura e connect limitado a 5 s | `ConexaoChatIntegracaoTest` (cancelamento, fechamento local e fim do servidor) |
| `ClienteChatGui.conexao` / `envio` | EDT e executor serial | fechar a janela enquanto conexão ou envio está enfileirado | referência `volatile`; operações ordenadas pelo `ExecutorService`; no fechamento, fecha a conexão e encerra o executor | `ConexaoChatIntegracaoTest.cancelarAntesDeConectarImpedeAberturaDoSocket`; inicialização headless |
| componentes Swing e `Animacao.transicoes` | EDT e callbacks do leitor | mutação de componente por thread de rede | eventos publicados com `SwingUtilities.invokeLater`; a lista do temporizador e a árvore Swing ficam na EDT | `ArquiteturaTest`; capturas `BufferedImage` headless |
| `ModeloConversa.itens` | callbacks da conversa e camada de apresentação | leitura durante adição ou descarte do item mais antigo | métodos sincronizados e `List.copyOf` para snapshots | `ModeloConversaTest` |
| `ChatService` (salvar+broadcast; conectar+histórico) | todas as threads de cliente | ordem do banco diferente da exibida; perder mensagem ao entrar | `ordemLock` | implementado; `ChatServiceTest` |
| `Connection` JDBC | threads que salvam/consultam | uso concorrente de uma conexão | uma `Connection` por operação | implementado em `JdbcMensagemRepository` |
| `Mensagem`, `Usuario` | todas | nenhum | imutáveis | implementado; testes do model |

### 1.3 Race conditions identificadas

| # | Situação | Como ocorreria | Como foi evitada | Teste que comprova |
|---|---|---|---|---|
| 1 | Duas conexões tentam registrar o mesmo nome ao mesmo tempo | ambas poderiam passar na verificação e aparecer online com o mesmo nome | `adicionar` e `remover` são atômicos; comparação usa `Usuario.equals` | `ClientRegistryTest.adicaoConcorrenteDoMesmoNomeEAtomica` |
| 2 | Alterações do registry coincidem com broadcast | iteração direta da lista poderia lançar exceção ou pular uma conexão estável | `CopyOnWriteArrayList`; broadcast percorre `snapshot()` | `ChatServiceTest.concorrenciaEntreRegistroRemocaoEBroadcastNaoPerdeLinhasNoClienteEstavel` |
| 3 | A janela fecha durante a tentativa de conexão ou leitura | socket podia continuar aberto ou a thread de leitura ficar presa após a tela fechar | conexão registra o socket antes do connect, verifica cancelamento após conectar, usa timeout de 5 s e fecha socket/streams idempotentemente | `ConexaoChatIntegracaoTest.cancelarAntesDeConectarImpedeAberturaDoSocket`; `ConexaoChatIntegracaoTest.desconexaoLocalFechaLeituraENotificaUmaVez` |

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

### 2026-10-13 · Fase 8B — Base visual Swing e Java2D

- **Adicionado:** `Tema`, `Animacao`, `FundoFuturista`, `BolhaMensagem`, `CampoMensagem`, `BotaoArredondado` e a classe manual `CapturarComponentes` (não descoberta pela suíte).
- **Arquivos:** `src/main/java/br/unicesumar/chat/client/gui/**`, `src/test/java/br/unicesumar/chat/client/gui/CapturarComponentes.java`, cinco PNGs em `docs/img/`, `docs/ESTADO.md`, este relatório.
- **Testes:** `mvn -q test` passou com 49 testes; capturas geradas com `java -Djava.awt.headless=true` e renderização em `BufferedImage`, sem abrir janelas.
- **Decisão visual:** fundo azul-noite com halos discretos, família instalada selecionada na ordem pedida, gradiente ciano-violeta restrito ao botão e às mensagens próprias. Texto de mensagem é selecionável; editor implementa Enter, Shift+Enter, Esc e contador próximo do limite.
- **Movimento:** `Animacao` usa um `javax.swing.Timer` de 16 ms, ease-out cúbico e repaint apenas do componente animado; o timer para quando a lista de transições esvazia.
- **Próximo passo:** Fase 8C — telas de conexão e conversa conectadas via `OuvinteChat`.

### 2026-10-13 · Fase 8C — Telas Swing conectadas ao cliente TCP

- **Adicionado:** `TelaConexao`, `ListaMensagens`, `TelaChat`, `PainelTransicao` e `ClienteChatGui`; `CapturarTelas` gera prévias das duas telas em modo headless.
- **Alterado:** `ModeloConversa` aceita o nick local ao concluir o handshake; `ListaMensagens` anima bolhas/rolagem, mantém até 500 linhas e marca a fronteira do histórico; `CampoMensagem` limita entrada a 500 caracteres e achata quebras em espaços antes de enviar, pois o protocolo existente é linha a linha.
- **Arquivos:** `src/main/java/br/unicesumar/chat/client/gui/**`, `src/main/java/br/unicesumar/chat/client/ModeloConversa.java`, `src/test/java/br/unicesumar/chat/support/ArquiteturaTest.java`, `src/test/java/br/unicesumar/chat/client/gui/CapturarTelas.java`, `docs/img/tela-*.png`.
- **Testes:** `mvn -q test` passou com 49 testes; `ClienteChatGui.main` detectou headless e encerrou sem abrir janela; as capturas finais foram renderizadas em `BufferedImage` e revisadas.
- **Concorrência/EDT:** EDT recebe todos os eventos de rede via `SwingUtilities.invokeLater`; `chat-rede-leitura` só lê socket e publica eventos; executor `chat-envio` serializa connect/envio e é encerrado ao fechar; `Animacao` compartilha um único `Timer` na janela, que para ao não haver transições (exceto pulso online/spinner ativos).
- **Próximo passo:** Fase 8D — revisão visual, documentos e teste do comando de execução.

### 2026-10-13 · Fase 8D — Polimento, arquitetura e documentação da GUI

- **Adicionado:** entrada gráfica em README, configuração VS Code `Cliente (interface gráfica)`, regras de import em `docs/AGENTS.md`, diagramas e riscos de GUI no relatório final; duas integrações para cancelamento e fechamento local; teste de agrupamento na virada do dia.
- **Alterado:** painel 1.2/1.3 registra conexões, executor, EDT, animação e três races; `ConexaoChat` fecha writer/socket em falha e cancela a tentativa com timeout de 5 s; agrupamento considera virada de meia-noite.
- **Arquivos:** `.vscode/launch.json`, `README.md`, `docs/AGENTS.md`, `docs/ESTADO.md`, `docs/RELATORIO_DESENVOLVIMENTO.md`, `docs/RELATORIO_FINAL.md`, client, testes de integração/modelo.
- **Testes:** 52/52 passaram (41 unitários/arquitetura + 11 integrações; 0 falhas/erros/ignorados). `mvn -q package` e `mvn -q exec:java -Dexec.mainClass=br.unicesumar.chat.client.gui.ClienteChatGui` também concluíram; o segundo foi executado com `java.awt.headless=true`, sem janela. `launch.json` validado como JSON.
- **EDT:** a Event Dispatch Thread cria e altera os componentes Swing e executa as ações locais; ela não conecta nem envia mensagens.
- **Leitura de rede:** `chat-rede-leitura` lê UTF-8 do socket e chama `OuvinteChat`, sem tocar na interface.
- **Envio:** o executor de thread única `chat-envio` faz connect, envia mensagens em ordem e encerra com a janela.
- **Animação:** um `javax.swing.Timer` compartilhado publica quadros na EDT; ele para quando não há transições, exceto enquanto status online ou spinner estão ativos.
- **Fechamento:** callbacks de rede entram na EDT por `SwingUtilities.invokeLater`; fechar o socket interrompe a leitura, e o executor é encerrado sem manter a janela aberta.
- **Decisão / concorrência:** nenhuma dependência nova; o protocolo continua uma linha por mensagem, então Shift+Enter formata o rascunho, mas suas quebras viram espaços no envio. A GUI usa somente Swing/Java2D e mantém o console reutilizando `ConexaoChat`.
- **Próximo passo:** o aluno conferir foco, escala e integração visual em Windows/VS Code e gravar o vídeo de demonstração.

### 2026-10-14 · Validação de conversa gráfica e encerramento

- **Verificado:** duas telas `TelaChat` ligadas a `ConexaoChat` estabeleceram conexões reais com um servidor TCP e persistência H2 em memória. O cliente bruno enviou “boa noite”; ana e bruno receberam a linha, e o teste confirmou o texto renderizado em ambas as telas. Ao parar o servidor, ambos receberam o callback de desconexão e as telas indicaram `Desconectado`.
- **Teste:** `ConversaGuiIntegracaoTest` usa a EDT para construir/inspecionar componentes Swing, filas e limites de tempo para aguardar eventos. Nenhuma janela nativa foi aberta nesta sessão; o teste é executável em ambiente headless. A compilação inicial do teste falhou porque chamadas de método não são permitidas como recursos em `try-with-resources`; foi corrigida usando variáveis finais de conexão e o teste direcionado passou.
- **Suíte:** `mvn -q test`: 53/53 passaram (41 unitários/arquitetura + 12 integrações; 0 falhas/erros/ignorados).
- **Observação operacional:** ao parar o servidor, os handlers fecham os sockets concorrentemente. Um broadcast de saída pode tentar escrever em um socket que já está sendo fechado e registrar um aviso; isso não impediu o encerramento, não perdeu a mensagem validada e ambos os clientes receberam a desconexão.
- **Próximo passo:** revisão visual manual no desktop do aluno, conforme pendência do estado do projeto.

### 2026-10-20 · Revisão visual da interface Swing

- **Alterado:** paleta da GUI passou a usar azul-petróleo `#101E2B`, ardósia `#1D3441`, texto claro `#EAF1EE`, verde-sinal `#4FC3A1` e coral `#E98B68`. Fundo e superfícies ficaram sólidos; mensagens próprias e recebidas usam superfícies diferentes. Foco, campos e botão de envio mantêm contraste. As capturas `docs/img/tela-conexao.png` e `docs/img/tela-chat.png` foram refeitas e revisadas com renderização headless.
- **Teste primeiro:** `TemaVisualTest` falhou com a paleta anterior e passou após a mudança. A primeira captura revelou que o desenho da borda cobria os valores dos campos; foi corrigido para deixar o próprio `JTextField` pintar o fundo e o texto. Outra falha de compilação revelou uma borda interna `static` acessando estado da tela; ela passou a ser uma classe interna de instância.
- **Arquivos:** `Tema`, `FundoFuturista`, `TelaConexao`, `BolhaMensagem`, `CampoMensagem`, `BotaoArredondado`, `ListaMensagens`, `TemaVisualTest`, capturas em `docs/img/`, `docs/ESTADO.md` e este relatório.
- **Testes:** `mvn -q test`: 54/54 passaram (42 unitários/arquitetura + 12 integrações; 0 falhas/erros/ignorados). Capturas Swing em `BufferedImage`, sem abrir janelas.
- **Concorrência:** nenhuma mudança nos fluxos de rede ou compartilhamento; a atualização visual é executada na EDT como antes.
- **Próximo passo:** conferir foco e escala em janela real no desktop do aluno.

### 2026-10-20 · Barra de rolagem da conversa

- **Alterado:** `ListaMensagens` usa uma barra vertical compacta baseada no `BasicScrollBarUI`: trilho fino, alça arredondada, cor de destaque durante hover/arraste, setas removidas e largura constante de 14 px. O nome acessível e a dica descrevem sua função. O histórico de exemplo nas capturas foi ampliado para tornar a rolagem visível.
- **Teste primeiro:** `ListaMensagensVisualTest` falhou com `MetalScrollBarUI`; após a implementação confirmou o UI personalizado, a largura e o nome acessível. A captura em `BufferedImage` foi revisada.
- **Arquivos:** `ListaMensagens`, `ListaMensagensVisualTest`, `CapturarTelas`, `docs/img/tela-chat.png`, `docs/ESTADO.md` e este relatório.
- **Testes:** `mvn -q test`: 55/55 passaram (43 unitários/arquitetura + 12 integrações; 0 falhas/erros/ignorados).
- **Concorrência:** sem alteração na lógica compartilhada; rolagem e desenho Swing continuam restritos à EDT. A barra mantém navegação por roda, teclado e arraste fornecida pelo Swing.
- **Próximo passo:** conferir o comportamento da barra em uma janela real no Windows, macOS ou Linux.

### 2026-10-20 · Base de acesso ao Hugging Face

- **Adicionado:** `java-dotenv` 5.2.2, resolvido pelo Maven Central; `ConfiguracaoIA` é a única classe que chama `Dotenv.load()` e valida/exibe instrução clara se `HF_TOKEN` estiver ausente. `ClienteHuggingFace` reutiliza `HttpClient`, envia JSON em UTF-8, usa timeout de conexão de 10 s e de requisição de 90 s, redige o token da mensagem de erro e converte falhas de rede/timeout/status HTTP em `FalhaInferenciaException`. IDs de modelos e endpoints ficaram em `chat.properties`.
- **Arquitetura autorizada:** acrescentado `infraestrutura/ia`; `ArquiteturaTest` permite nesse pacote somente `java.net.URI` e `java.net.http.*`. A leitura do token continua centralizada. Não há funcionalidades de moderação, resumo, termômetro ou `/hacker` implementadas.
- **Teste manual executado:** `ConectividadeManualHuggingFace` chamou os quatro modelos. `unitary/multilingual-toxic-xlm-roberta` → HTTP 402, “You have no remaining credits. Purchase pre-paid credits to continue using Inference Providers. Alternatively, subscribe to PRO to get monthly included credits.”; `Qwen/Qwen2.5-1.5B-Instruct` → HTTP 400, “The requested model 'Qwen/Qwen2.5-1.5B-Instruct' is not supported by any provider you have enabled.”; `cardiffnlp/twitter-xlm-roberta-base-sentiment` → mesmo HTTP 402 de créditos; `Qwen/Qwen3.8-27B` → mesmo HTTP 402 de créditos. Não houve resposta de inferência bem-sucedida; nenhum modelo, provedor ou payload alternativo foi tentado.
- **Testes automáticos:** testes locais validam token ausente, cabeçalho bearer, corpo UTF-8, resposta de sucesso e erro HTTP. `mvn -q test`: 59/59 passaram (47 unitários/arquitetura + 12 integrações; 0 falhas/erros/ignorados). A classe de conectividade não possui `@Test` e não roda na suíte automática.
- **Segurança:** `.env` está ignorado pelo Git e não rastreado; seu conteúdo não foi impresso nem registrado. O teste só imprime dados da resposta e mensagens de erro saneadas.
- **Próximo passo:** habilitar créditos/provedor no Hugging Face e repetir a conectividade antes da Fase 1.
