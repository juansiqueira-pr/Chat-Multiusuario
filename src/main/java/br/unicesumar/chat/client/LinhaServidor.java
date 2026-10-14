package br.unicesumar.chat.client;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Representa uma linha do protocolo do servidor já classificada para apresentação. */
public record LinhaServidor(Tipo tipo, String remetente, LocalTime horario, String texto, String original) {
    private static final Pattern MENSAGEM = Pattern.compile(
            "^\\[(\\d{2}/\\d{2}) (\\d{2}:\\d{2})\\] ([^:]+): (.*)$");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Tipos de linha enviados pelo servidor. */
    public enum Tipo { MENSAGEM, SISTEMA, ERRO, ACEITE }

    /** Interpreta uma linha sem propagar erros de formato. */
    public static LinhaServidor interpretar(String linha) {
        String segura = linha == null ? "" : linha;
        if (segura.startsWith("! ")) return new LinhaServidor(Tipo.ERRO, "", null, segura.substring(2), segura);
        if (segura.startsWith("* Conectado como ")) {
            String nome = segura.substring("* Conectado como ".length());
            return new LinhaServidor(Tipo.ACEITE, nome, null, nome, segura);
        }
        if (segura.startsWith("* ")) return new LinhaServidor(Tipo.SISTEMA, "", null, segura.substring(2), segura);
        Matcher matcher = MENSAGEM.matcher(segura);
        if (matcher.matches()) {
            try {
                return new LinhaServidor(Tipo.MENSAGEM, matcher.group(3),
                        LocalTime.parse(matcher.group(2), HORA), matcher.group(4), segura);
            } catch (DateTimeParseException excecao) {
                return new LinhaServidor(Tipo.SISTEMA, "", null, segura, segura);
            }
        }
        return new LinhaServidor(Tipo.SISTEMA, "", null, segura, segura);
    }
}
