package br.unicesumar.chat.infraestrutura.ia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracaoIATest {
    @Test
    void tokenAusenteGeraInstrucaoSemExporValor() {
        IllegalStateException excecao = assertThrows(IllegalStateException.class,
                () -> ConfiguracaoIA.validarToken("  "));

        assertEquals("Configuração da IA ausente: defina HF_TOKEN no arquivo .env na raiz do projeto "
                + "(ex.: HF_TOKEN=seu_token).", excecao.getMessage());
    }

    @Test
    void tokenConfiguradoEDevolvidoSemEspacosExcedentes() {
        assertEquals("hf_valido", ConfiguracaoIA.validarToken(" hf_valido "));
    }
}
