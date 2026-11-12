package io.github.johnsampa.creatorpay.dto;

import java.math.BigDecimal;

public record VendasPorProduto(String produto, long vendas, BigDecimal faturamento) {
}
