# Relatório final — Chat Multiusuário com Histórico

## Arquitetura

```mermaid
flowchart LR
  Cliente[client] --> Servidor[server]
  Servidor --> Servico[service]
  Servico --> Modelo[model]
  Servico --> Contrato[persistence]
  JDBC[persistence.jdbc] -. implementa .-> Contrato
  Servidor -. montagem .-> JDBC
  GUI[client.gui] --> Cliente[client TCP]
```

```mermaid
classDiagram
  Servidor --> ClienteHandler
  ClienteHandler ..|> ConexaoCliente
  ChatService --> ClientRegistry
  ChatService --> MensagemRepository
  ChatService --> Mensagem
  JdbcMensagemRepository ..|> MensagemRepository
  ClientRegistry o--> ConexaoCliente
  Mensagem --> Usuario
  ClienteChatGui --> ConexaoChat
  ClienteChatGui --> TelaConexao
  ClienteChatGui --> TelaChat
  TelaChat --> ListaMensagens
  ListaMensagens --> ModeloConversa
```

## Fluxo de uma mensagem

```mermaid
sequenceDiagram
  participant C as ClienteHandler
  participant S as ChatService
  participant R as MensagemRepository
  participant L as ClientRegistry
  C->>S: enviarMensagem(usuario, texto)
  Note over S: ordemLock adquirido
  S->>R: salvar(Mensagem)
  R-->>S: mensagem com ID
  S->>L: snapshot()
  L-->>S: conexões imutáveis
  S-->>C: broadcast sincronizado para cada conexão
```

## Concorrência e persistência

| Estado compartilhado | Proteção |
|---|---|
| Conexões ativas | `CopyOnWriteArrayList`, operações de nome atômicas e snapshot imutável. |
| Ciclo do servidor | Estado `volatile`, transições sob `cicloLock` e coleção concorrente de handlers. |
| Ordem do serviço | `ordemLock` protege salvar+broadcast e registrar+histórico; clientes lentos reduzem paralelismo. |
| Saída de cada handler | `enviar()` sincronizado; uma linha completa por escrita. |
| Resumo de histórico | Snapshot das últimas mensagens; consulta Groq fora de `ordemLock`; resposta somente ao solicitante. |
| Handlers do servidor | Conjunto concorrente; `parar()` fecha servidor e sockets ativos. |
| Conexão dos clientes console e Swing | `ConexaoChat` usa UTF-8, escrita sincronizada, cancelamento idempotente; leitura notifica o ouvinte em thread própria. |
| Componentes Swing | Só a EDT altera telas; callbacks de rede usam `SwingUtilities.invokeLater`. |
| Envio da GUI | Um executor de thread única ordena conexão/envio e é encerrado ao fechar a janela. |
| Animações | Um `javax.swing.Timer` compartilhado atualiza apenas componentes animados e para quando ocioso. |
| JDBC | Uma `Connection` por operação, sempre fechada; `PreparedStatement` e IDs gerados pelo banco. |
| `Usuario` e `Mensagem` | Imutáveis, podem ser compartilhados sem lock. |

Uma race de nick duplicado foi evitada tornando a inclusão atômica (`ClientRegistryTest`). Alterações no registry durante broadcast são seguras por snapshot e foram exercitadas com `ExecutorService` e `CountDownLatch` (`ChatServiceTest`). O lock de ordenação impede que uma mensagem fique entre o registro do novo cliente e o envio do histórico.

O fechamento da GUI durante conexão/leitura também fecha o socket e os streams; o connect tem timeout de 5 s e verifica cancelamento (`ConexaoChatIntegracaoTest`). O cliente gráfico usa Swing/Java2D sem dependências adicionais, paleta azul-petróleo, ardósia, verde-sinal e coral, com alternância clara e capturas em `docs/img/`. Como o protocolo existente envia uma linha por mensagem, quebras feitas com Shift+Enter são convertidas em espaços ao enviar.

O comando `/historico` (também `/histórico`) resume as mensagens recentes com Groq e responde em privado. `ServidorIntegracaoTest` comprova privacidade e continuidade do chat quando o modelo falha; o teste local de HTTP valida o formato de resposta.

O H2 usa `./data/chat`; o schema é inicializado com `CREATE ... IF NOT EXISTS`. O histórico busca os últimos IDs em ordem decrescente e os inverte para a ordem cronológica. Os testes JDBC usam bancos em memória com nome aleatório.

## Escopo, extras e declaração de IA

Os extras não foram implementados. Sugestões para evolução: timeout/heartbeat de inatividade; teste de carga com muitos sockets concorrentes; e teste de reinicialização que demonstre persistência após encerrar o servidor.

IA generativa auxiliou implementação, testes e documentação. O aluno deve revisar e conseguir explicar o código e as decisões; o log está em [docs/RELATORIO_DESENVOLVIMENTO.md](RELATORIO_DESENVOLVIMENTO.md).
