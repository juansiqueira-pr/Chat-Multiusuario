package br.unicesumar.chat.client;

/** Recebe eventos da conexão sem depender de uma interface gráfica. */
public interface OuvinteChat {
    /** Informa que o servidor aceitou o nick. */
    void aoAceitar(String nome);

    /** Entrega uma linha já interpretada do servidor. */
    void aoReceber(LinhaServidor linha);

    /** Informa que o servidor recusou a conexão. */
    void aoRejeitar(String motivo);

    /** Informa que a conexão terminou. */
    void aoDesconectar(String motivo);
}
