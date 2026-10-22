# ESTADO DO PROJETO

> Mapa compacto do estado atual; histórico de mudanças em `RELATORIO_DESENVOLVIMENTO.md`.

**Atualizado em:** 2026-10-22 · **Fase atual:** assistente técnico privado `/hacker`
**Marco "núcleo obrigatório completo":** ✅ atingido
**Testes:** `mvn -q test` passou: 78 testes, 0 falhas, 0 erros, 0 ignorados (61 unitários/visuais/arquitetura + 17 integrações).
**Próximo passo:** implementar termômetro do clima quando solicitado.

## Requisitos obrigatórios

| # | Requisito | Evidência | Estado |
|---|---|---|---|
| 1 | Servidor TCP com `accept()` | `Servidor`, `ServidorIntegracaoTest` | ✅ |
| 2 | Uma thread por conexão | `Servidor`, integração TCP | ✅ |
| 3 | Handler `Runnable` por conexão | `ClienteHandler`, integração TCP | ✅ |
| 4 | Registry thread-safe | `ClientRegistryTest` | ✅ |
| 5 | Broadcast aos conectados | `ChatServiceTest`, integração TCP | ✅ |
| 6 | Mensagem imutável e validada | `MensagemTest` | ✅ |
| 7 | Persistência JDBC | `JdbcMensagemRepositoryIntegracaoTest` | ✅ |
| 8 | SQL isolado no repository JDBC | `ArquiteturaTest` | ✅ |
| 9 | Histórico ao conectar | `ChatServiceTest`, integração TCP | ✅ |
| 10 | Desconexão segura | `ServidorIntegracaoTest` | ✅ |
| 11 | Pelo menos cinco testes unitários | suíte JUnit | ✅ |
| 12 | Separação em pacotes | `ArquiteturaTest` | ✅ |

Vídeo com 3+ clientes: ⬜ responsabilidade do aluno. Marco de núcleo completo permanece atingido.

## Rubrica de 5,0 pontos

| Critério | Pontos | Evidência |
|---|---:|---|
| Clientes simultâneos | 1,3 | `Servidor`, `ClienteHandler`, integração TCP |
| Broadcast ordenado e seguro | 1,2 | `ChatServiceTest`, teste concorrente |
| JDBC | 1,0 | `JdbcMensagemRepositoryIntegracaoTest` |
| Pacotes e responsabilidades | 0,5 | `ArquiteturaTest` |
| Testes de lógica | 0,5 | `ChatServiceTest`, suíte JUnit |
| Organização e relatório | 0,5 | README e relatórios |

## Funcionalidades e classes-chave

- Núcleo: `model/`, `persistence/`, `persistence/jdbc/`, `service/`, `server/`, `client/` e `client/gui/`.
- Moderação: `ModeradorDeMensagens`, `ModeradorGroq`, `ModeradorResiliente`; mensagens permitidas seguem para persistência e broadcast, ofensivas são recusadas privadamente.
- Resumo: `ResumidorDeConversa`, `ResumidorGroq`; `/historico` e `/histórico` resumem as últimas `historico.tamanho` mensagens e respondem apenas ao solicitante.
- Assistente técnico: `AssistenteTecnico`, `AssistenteTecnicoGroq`; `/hacker <pergunta>` consulta o modelo configurado para orientação educativa/defensiva e responde apenas ao solicitante.
- Configuração de IA: `ConfiguracaoIA` lê `GROQ_API_KEY`; `ClienteGroq` centraliza HTTP e tratamento de rede, timeout e erros da API.
- Falhas de resumo/assistente: exceções específicas geram aviso privado; outras conexões continuam ativas.
- Testes da etapa: `AssistenteTecnicoGroqTest`, `ProtocoloTest` e `ServidorIntegracaoTest` cobrem política enviada, resposta, comando, privacidade, pergunta vazia e falha.

## Decisões fixas

- JDK 21 no ambiente atual; Maven; H2 em `./data/chat`; porta padrão `5555`; histórico padrão de 20 mensagens.
- `ChatService.ordemLock` protege persistência+broadcast e conexão+histórico; resumo copia o histórico sob lock e chama a IA fora dele.
- Uma espera Groq pode pausar a thread do cliente solicitante por até 90 s; outras conexões não aguardam esse lock.
- Moderação e resumo exigem `GROQ_API_KEY` em `.env`; sem chave, moderação opera fail-open e resumo informa indisponibilidade.
- Comandos implementados: `/quit`, `/historico`, `/histórico` e `/hacker <pergunta>`; o termômetro permanece pendente.

## Como executar

- Testes: `mvn -q test`
- Servidor: `mvn -q exec:java -Dexec.mainClass=br.unicesumar.chat.server.Servidor`
- Cliente GUI: `mvn -q exec:java -Dexec.mainClass=br.unicesumar.chat.client.gui.ClienteChatGui`

## Pendências conhecidas

- Fazer o vídeo acadêmico com três ou mais clientes.
- Implementar o termômetro em etapa futura, mediante solicitação.
- O prompt do `/hacker` solicita uso apenas educativo/autorizado e recusa orientação a ataques reais; isso é uma regra de comportamento do modelo, não um controle de acesso ou garantia de segurança.
- Quebras de linha do editor Swing são convertidas em espaços porque o protocolo TCP usa uma linha por mensagem.
