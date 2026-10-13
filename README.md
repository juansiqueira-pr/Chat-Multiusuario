# Chat Multiusuário com Histórico

Projeto acadêmico de POO em Java para a UniCesumar/UNIPA Curitiba.
Um servidor TCP atende vários clientes em paralelo, com uma thread por conexão.
As mensagens são persistidas com JDBC/H2, transmitidas a todos e entregues no histórico ao entrar.

## 1. Requisitos

- JDK 17+ é o mínimo do projeto; este checkout requer JDK 21 ou superior porque usa `maven.compiler.release=21`, conforme o JDK instalado.
- Maven 3.10 ou superior, disponível no `PATH` como `mvn`.
- VS Code com o **Extension Pack for Java**.

## 2. Obter e abrir

Clone o repositório fornecido pela disciplina ou abra a pasta do projeto que recebeu:

```text
code ChatMultiusuario
```

No VS Code, abra a pasta raiz que contém `pom.xml`.

## 3. Configuração

Edite `src/main/resources/chat.properties` antes de iniciar o servidor:

| Propriedade | Padrão | Uso |
|---|---|---|
| `porta` | `5555` | Porta TCP; `0` escolhe uma porta livre para testes. |
| `banco.url` | `jdbc:h2:./data/chat` | URL do H2 em arquivo. |
| `banco.usuario` | `sa` | Usuário do banco. |
| `banco.senha` | vazio | Senha do banco. |
| `historico.tamanho` | `20` | Mensagens enviadas a cada novo cliente. |

Se `chat.properties` não estiver no classpath, `Config` usa esses valores padrão.

## 4. Compilar e testar

Na raiz do projeto:

```powershell
mvn -q package
mvn -q test
```

A suíte tem testes unitários de modelo, registry, serviço, protocolo e configuração; integração JDBC com H2 em memória; e integração TCP com sockets reais para broadcast, histórico, desconexão abrupta, nick duplicado e mensagem vazia.

## 5. Iniciar o servidor

No terminal:

```powershell
mvn -q exec:java -Dexec.mainClass=br.unicesumar.chat.server.Servidor
```

No VS Code, abra **Run and Debug** e escolha **Servidor**. O banco de arquivo é criado na primeira inicialização.

## 6. Conectar clientes

Abra um terminal por cliente. Execute os três comandos abaixo em terminais separados:

```powershell
mvn -q exec:java -Dexec.mainClass=br.unicesumar.chat.client.ClienteChat -Dexec.args="localhost 5555 ana"
mvn -q exec:java -Dexec.mainClass=br.unicesumar.chat.client.ClienteChat -Dexec.args="localhost 5555 bruno"
mvn -q exec:java -Dexec.mainClass=br.unicesumar.chat.client.ClienteChat -Dexec.args="localhost 5555 carlos"
```

No VS Code, escolha **Cliente** em **Run and Debug**. Os argumentos podem ser editados em `.vscode/launch.json`. Se o nick não for passado, o cliente pergunta no teclado.

Exemplo de sessão: Ana digita `Oi, pessoal!`; os clientes veem `[13/10 09:00] ana: Oi, pessoal!`. Quando Bruno entra, todos veem `* bruno entrou`.

## 7. Comandos e formato

- `/quit`: encerra a conexão atual.
- Mensagem: `[dd/MM HH:mm] nome: texto`.
- Sistema: `* texto` (entrada/saída e avisos).
- Erro: `! texto` (só para o remetente).

## 8. Histórico e banco

O H2 cria `data/chat.mv.db` relativo à pasta de onde o servidor foi iniciado. Para zerar o histórico, pare o servidor e apague a pasta `data/`.

## 9. Estrutura de pacotes

```text
br.unicesumar.chat
├── client/       ClienteChat: terminal e conexão TCP do cliente.
├── config/       Config: propriedades e padrões.
├── model/        Usuario e Mensagem: objetos e validações de negócio.
├── persistence/  MensagemRepository e exceção: contrato sem SQL.
│   └── jdbc/     ConnectionFactory, schema e implementação JDBC.
├── server/       Servidor, ClienteHandler e Protocolo.
└── service/      ChatService, ClientRegistry e abstrações de conexão.
```

## 10. Concorrência

- O servidor cria uma thread nomeada por cliente.
- `ClientRegistry` usa `CopyOnWriteArrayList` e torna o nome único de forma atômica.
- O broadcast percorre um snapshot e não mantém lock do registry durante a escrita.
- `ClienteHandler.enviar()` é sincronizado para não misturar linhas no mesmo socket.
- `ChatService.ordemLock` serializa salvar+broadcast e registrar+enviar histórico.

## 11. Solução de problemas

- **Porta em uso:** altere `porta` em `chat.properties` ou encerre o processo que usa `5555`.
- **Acentos quebrados no Windows:** no terminal, execute `chcp 65001` antes do Maven.
- **Firewall:** permita conexões TCP na porta configurada para clientes na rede.
- **Banco em uso:** não rode dois servidores apontando ao mesmo `data/`; encerre o outro servidor primeiro.

## 12. Roteiro do vídeo

1. Inicie o servidor e mostre a porta ativa.
2. Conecte Ana, Bruno e Carlos em três terminais.
3. Troque mensagens e mostre que chegam aos três.
4. Conecte um quarto nick e mostre o histórico em ordem.
5. Feche um cliente abruptamente e prove que os demais continuam conversando.
6. Encerre os clientes com `/quit` e pare o servidor.

## 13. Uso de IA e limitações

IA generativa foi usada como apoio à implementação, aos testes e à documentação; decisões e código devem ser revisados e explicados pelo aluno. O histórico de desenvolvimento está em [docs/RELATORIO_DESENVOLVIMENTO.md](docs/RELATORIO_DESENVOLVIMENTO.md).

O núcleo não tem autenticação, salas, mensagens privadas, TLS, interface gráfica, timeout de inatividade ou reconexão automática. Os nicks identificam clientes apenas durante a conexão.
