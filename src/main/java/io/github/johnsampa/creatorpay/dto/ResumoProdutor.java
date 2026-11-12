package io.github.johnsampa.creatorpay.dto;

import java.math.BigDecimal;

public record ResumoProdutor(
        String produtor,
        int periodoDias,
        BigDecimal faturamento,
        long vendasPagas,
        BigDecimal ticketMedio,
        BigDecimal taxaConversaoPercentual,
        long carrinhosAbandonados,
        BigDecimal valorEmCarrinhosAbandonados,
        long pagamentosVencidos,
        BigDecimal valorEmPagamentosVencidos) {
}
