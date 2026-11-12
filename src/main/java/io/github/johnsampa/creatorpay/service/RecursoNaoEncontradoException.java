package io.github.johnsampa.creatorpay.service;

/**
 * Lançada quando um produtor ou pedido não existe. A mensagem é pensada para o agente de IA
 * ler e saber o que fazer em seguida (ex.: "use listarProdutores").
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
