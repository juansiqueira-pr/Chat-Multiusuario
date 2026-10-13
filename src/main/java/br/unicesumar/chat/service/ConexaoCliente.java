package br.unicesumar.chat.service;

import br.unicesumar.chat.model.Usuario;

/** Abstrai uma conexão capaz de receber linhas do serviço de chat. */
public interface ConexaoCliente {
    /** Retorna o usuário associado à conexão. */
    Usuario getUsuario();

    /** Envia uma linha ao cliente ou lança falha se a escrita não funcionar. */
    void enviar(String linha) throws FalhaEnvioException;
}
