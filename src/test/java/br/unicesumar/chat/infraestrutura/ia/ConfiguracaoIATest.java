package br.unicesumar.chat.infraestrutura.ia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracaoIATest {
    @Test
    void chaveGroqAusenteGeraInstrucaoSemExporValor() {
        IllegalStateException excecao = assertThrows(IllegalStateException.class,
                () -> ConfiguracaoIA.validarChaveGroq("  "));

        assertEquals("Configuração da IA ausente: defina GROQ_API_KEY no arquivo .env na raiz do projeto "
                + "(ex.: GROQ_API_KEY=sua_chave).", excecao.getMessage());
    }

    @Test
    void chaveConfiguradaEDevolvidaSemEspacosExcedentes() {
        assertEquals("gsk_valido", ConfiguracaoIA.validarChaveGroq(" gsk_valido "));
    }
}
