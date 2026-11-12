package io.github.johnsampa.creatorpay.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.github.johnsampa.creatorpay.domain.MetodoPagamento;
import io.github.johnsampa.creatorpay.domain.Pedido;
import io.github.johnsampa.creatorpay.domain.StatusPedido;

/**
 * O que o agente de IA enxerga de um pedido. O e-mail do comprador sai mascarado:
 * o agente não precisa do dado completo para responder, então ele não sai do servidor.
 */
public record PedidoResumo(
        String codigo,
        String produto,
        String comprador,
        String emailMascarado,
        BigDecimal valor,
        MetodoPagamento metodo,
        StatusPedido status,
        Instant criadoEm,
        Instant pagoEm,
        Instant vencimentoEm) {

    public static PedidoResumo de(Pedido pedido) {
        return new PedidoResumo(
                pedido.getCodigo(),
                pedido.getProduto().getNome(),
                pedido.getNomeComprador(),
                mascararEmail(pedido.getEmailComprador()),
                pedido.getValor(),
                pedido.getMetodo(),
                pedido.getStatus(),
                pedido.getCriadoEm(),
                pedido.getPagoEm(),
                pedido.getVencimentoEm());
    }

    static String mascararEmail(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        String inicio = email.substring(0, Math.min(2, arroba));
        return inicio + "***" + email.substring(arroba);
    }
}
