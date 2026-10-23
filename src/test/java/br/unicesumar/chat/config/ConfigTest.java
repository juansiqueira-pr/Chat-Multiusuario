package br.unicesumar.chat.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTest {
    @Test
    void carregaOsValoresDoRecursoPadrao() {
        Config config = Config.carregar();

        assertEquals(5555, config.getPorta());
        assertEquals("jdbc:h2:./data/chat", config.getBancoUrl());
        assertEquals("sa", config.getBancoUsuario());
        assertEquals("", config.getBancoSenha());
        assertEquals(20, config.getHistoricoTamanho());
        assertEquals(5, config.getClimaIntervaloMensagens());
    }

    @Test
    void usaPadroesQuandoOArquivoNaoExiste() {
        Config config = Config.carregar(null);

        assertEquals(5555, config.getPorta());
        assertEquals("jdbc:h2:./data/chat", config.getBancoUrl());
        assertEquals("sa", config.getBancoUsuario());
        assertEquals("", config.getBancoSenha());
        assertEquals(20, config.getHistoricoTamanho());
        assertEquals(5, config.getClimaIntervaloMensagens());
    }

    @Test
    void preenchePropriedadesAusentesComPadroes() {
        byte[] bytes = "porta=0\nhistorico.tamanho=3\n".getBytes(StandardCharsets.UTF_8);
        Config config = Config.carregar(new ByteArrayInputStream(bytes));

        assertEquals(0, config.getPorta());
        assertEquals(3, config.getHistoricoTamanho());
        assertEquals("jdbc:h2:./data/chat", config.getBancoUrl());
    }

    @Test
    void permiteConfigurarIntervaloDeAnaliseDoClima() {
        byte[] bytes = "clima.intervalo.mensagens=8\n".getBytes(StandardCharsets.UTF_8);

        assertEquals(8, Config.carregar(new ByteArrayInputStream(bytes)).getClimaIntervaloMensagens());
    }
}
