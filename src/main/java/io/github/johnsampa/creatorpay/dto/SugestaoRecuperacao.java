package io.github.johnsampa.creatorpay.dto;

import io.github.johnsampa.creatorpay.domain.StatusPedido;

public record SugestaoRecuperacao(
        String codigoPedido,
        StatusPedido status,
        boolean acaoNecessaria,
        String canal,
        String acao,
        String mensagemSugerida) {
}
