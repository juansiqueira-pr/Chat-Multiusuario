package br.unicesumar.chat.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Lê as propriedades do chat e fornece valores padrão quando o arquivo falta. */
public final class Config {
    private static final int PORTA_PADRAO = 5555;
    private static final String URL_PADRAO = "jdbc:h2:./data/chat";
    private static final String USUARIO_PADRAO = "sa";
    private static final int HISTORICO_PADRAO = 20;
    private static final int CLIMA_INTERVALO_PADRAO = 5;

    private final int porta;
    private final String bancoUrl;
    private final String bancoUsuario;
    private final String bancoSenha;
    private final int historicoTamanho;
    private final int climaIntervaloMensagens;

    private Config(int porta, String bancoUrl, String bancoUsuario, String bancoSenha, int historicoTamanho,
                   int climaIntervaloMensagens) {
        this.porta = porta;
        this.bancoUrl = bancoUrl;
        this.bancoUsuario = bancoUsuario;
        this.bancoSenha = bancoSenha;
        this.historicoTamanho = historicoTamanho;
        this.climaIntervaloMensagens = climaIntervaloMensagens;
    }

    /** Carrega as propriedades do recurso chat.properties ou usa os padrões. */
    public static Config carregar() {
        return carregar(Config.class.getResourceAsStream("/chat.properties"));
    }

    /** Carrega propriedades de um fluxo ou usa os padrões quando ele é null. */
    public static Config carregar(InputStream fluxo) {
        Properties propriedades = new Properties();
        if (fluxo != null) {
            try (InputStream entrada = fluxo) {
                propriedades.load(entrada);
            } catch (IOException excecao) {
                throw new IllegalStateException("Não foi possível ler chat.properties.", excecao);
            }
        }
        int porta = inteiro(propriedades.getProperty("porta"), PORTA_PADRAO);
        int historico = inteiro(propriedades.getProperty("historico.tamanho"), HISTORICO_PADRAO);
        int climaIntervalo = inteiro(propriedades.getProperty("clima.intervalo.mensagens"),
                CLIMA_INTERVALO_PADRAO);
        if (porta < 0 || porta > 65535) {
            porta = PORTA_PADRAO;
        }
        if (historico < 0) {
            historico = HISTORICO_PADRAO;
        }
        if (climaIntervalo < 1) climaIntervalo = CLIMA_INTERVALO_PADRAO;
        return new Config(
                porta,
                propriedades.getProperty("banco.url", URL_PADRAO),
                propriedades.getProperty("banco.usuario", USUARIO_PADRAO),
                propriedades.getProperty("banco.senha", ""),
                historico,
                climaIntervalo);
    }

    /** Retorna a porta TCP configurada. */
    public int getPorta() {
        return porta;
    }

    /** Retorna a URL JDBC configurada. */
    public String getBancoUrl() {
        return bancoUrl;
    }

    /** Retorna o usuário configurado do banco. */
    public String getBancoUsuario() {
        return bancoUsuario;
    }

    /** Retorna a senha configurada do banco. */
    public String getBancoSenha() {
        return bancoSenha;
    }

    /** Retorna o limite de mensagens entregues no histórico inicial. */
    public int getHistoricoTamanho() {
        return historicoTamanho;
    }

    /** Retorna o intervalo de mensagens entre análises do clima. */
    public int getClimaIntervaloMensagens() { return climaIntervaloMensagens; }

    private static int inteiro(String valor, int padrao) {
        if (valor == null) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException excecao) {
            return padrao;
        }
    }
}
