package br.unicesumar.chat.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

class ArquiteturaTest {
    private static final Pattern IMPORTACAO =
            Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?([\\w.*]+);\\s*$");

    @Test
    void importacoesRespeitamAsRegrasDeCamadas() throws IOException {
        Path raiz = Path.of("src", "main", "java");
        List<String> violacoes = new ArrayList<>();
        try (Stream<Path> arquivos = Files.walk(raiz)) {
            arquivos.filter(caminho -> caminho.toString().endsWith(".java"))
                    .forEach(caminho -> verificarArquivo(raiz, caminho, violacoes));
        }
        if (!violacoes.isEmpty()) {
            fail("Importações proibidas encontradas:\n" + String.join("\n", violacoes));
        }
    }

    private static void verificarArquivo(Path raiz, Path arquivo, List<String> violacoes) {
        try {
            String codigo = Files.readString(arquivo);
            String pacote = raiz.relativize(arquivo).getParent().toString().replace('\\', '/');
            Matcher correspondencia = IMPORTACAO.matcher(codigo);
            while (correspondencia.find()) {
                String importacao = correspondencia.group(1);
                verificarImportacao(arquivo, pacote, importacao, violacoes);
            }
        } catch (IOException excecao) {
            throw new IllegalStateException("Não foi possível ler " + arquivo + ".", excecao);
        }
    }

    private static void verificarImportacao(Path arquivo, String pacote, String importacao,
                                            List<String> violacoes) {
        String caminho = arquivo.toString().replace('\\', '/');
        boolean jdbc = pacote.startsWith("br/unicesumar/chat/persistence/jdbc");
        boolean servidorOuCliente = pacote.startsWith("br/unicesumar/chat/server")
                || pacote.startsWith("br/unicesumar/chat/client");
        if (importacao.startsWith("java.sql.") && !jdbc) {
            violacoes.add(caminho + " importa " + importacao + " fora de persistence/jdbc");
        }
        boolean clienteIa = pacote.startsWith("br/unicesumar/chat/infraestrutura/ia")
                && (importacao.equals("java.net.URI") || importacao.startsWith("java.net.http."));
        if (importacao.startsWith("java.net.") && !servidorOuCliente && !clienteIa) {
            violacoes.add(caminho + " importa " + importacao + " fora das camadas de rede autorizadas");
        }
        if (pacote.startsWith("br/unicesumar/chat/model")
                && importacao.startsWith("br.unicesumar.chat.")
                && !importacao.startsWith("br.unicesumar.chat.model.")) {
            violacoes.add(caminho + " faz model depender de outra camada: " + importacao);
        }
        if (pacote.startsWith("br/unicesumar/chat/service")
                && (importacao.startsWith("br.unicesumar.chat.server.")
                || importacao.startsWith("br.unicesumar.chat.persistence.jdbc."))) {
            violacoes.add(caminho + " faz service depender de camada concreta: " + importacao);
        }
        if (pacote.startsWith("br/unicesumar/chat/client")
                && (importacao.startsWith("br.unicesumar.chat.service.")
                || importacao.startsWith("br.unicesumar.chat.persistence.")
                || importacao.startsWith("br.unicesumar.chat.server.")
                || importacao.startsWith("br.unicesumar.chat.model.")
                || importacao.startsWith("br.unicesumar.chat.config."))) {
            violacoes.add(caminho + " faz client depender de camadas do núcleo: " + importacao);
        }
        if ((importacao.startsWith("javax.swing.") || importacao.startsWith("java.awt."))
                && !pacote.startsWith("br/unicesumar/chat/client/gui")) {
            violacoes.add(caminho + " importa Swing/AWT fora de client/gui: " + importacao);
        }
        if (pacote.startsWith("br/unicesumar/chat/persistence") && !jdbc
                && (importacao.startsWith("br.unicesumar.chat.server.")
                || importacao.startsWith("br.unicesumar.chat.service."))) {
            violacoes.add(caminho + " faz persistence depender de server/service: " + importacao);
        }
    }
}
