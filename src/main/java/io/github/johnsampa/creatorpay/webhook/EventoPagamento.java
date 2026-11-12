package io.github.johnsampa.creatorpay.webhook;

/**
 * Corpo do webhook enviado pelo gateway de pagamento (simulado).
 *
 * @param idEvento     ID único do evento, usado para idempotência
 * @param tipo         pagamento.aprovado | pagamento.vencido | pagamento.reembolsado
 * @param codigoPedido código do pedido afetado, ex.: CP-10001
 */
public record EventoPagamento(String idEvento, String tipo, String codigoPedido) {
}
