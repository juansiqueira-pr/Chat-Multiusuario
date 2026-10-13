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
| `ClientRegistry` (lista de conexões) | todas as threads de cliente | modificação durante o broadcast; nome duplicado | `CopyOnWriteArrayList` + `adicionar` atômico + `snapshot()` | planejado |
| `ClienteHandler.enviar()` | threads que fazem broadcast | linhas de duas mensagens intercaladas | método `synchronized` | planejado |
| `ChatService` (salvar+broadcast; conectar+histórico) | todas as threads de cliente | ordem do banco diferente da exibida; perder mensagem ao entrar | `ordemLock` | planejado |
| `Connection` JDBC | threads que salvam/consultam | uso concorrente de uma conexão | uma `Connection` por operação | planejado |
| `Mensagem`, `Usuario` | todas | nenhum | imutáveis | planejado |

### 1.3 Race conditions identificadas

| # | Situação | Como ocorreria | Como foi evitada | Teste que comprova |
|---|---|---|---|---|
| — | *(nenhuma registrada ainda)* | | | |

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
