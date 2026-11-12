package io.github.johnsampa.creatorpay.webhook;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.johnsampa.creatorpay.domain.EventoWebhook;
import io.github.johnsampa.creatorpay.domain.Pedido;
import io.github.johnsampa.creatorpay.repository.EventoWebhookRepository;
import io.github.johnsampa.creatorpay.repository.PedidoRepository;
import io.github.johnsampa.creatorpay.service.RecursoNaoEncontradoException;

@Service
public class WebhookService {

    private final PedidoRepository pedidoRepository;
    private final EventoWebhookRepository eventoRepository;
    private final Clock clock;

    public WebhookService(PedidoRepository pedidoRepository, EventoWebhookRepository eventoRepository, Clock clock) {
        this.pedidoRepository = pedidoRepository;
        this.eventoRepository = eventoRepository;
        this.clock = clock;
    }

    /**
     * Aplica o evento ao pedido. Gateways reenviam o mesmo evento quando não recebem 2xx a tempo,
     * então um evento já visto é ignorado (idempotência), em vez de ser aplicado de novo.
     */
    @Transactional
    public ResultadoWebhook processar(EventoPagamento evento) {
        validar(evento);
        if (eventoRepository.existsById(evento.idEvento())) {
            return ResultadoWebhook.DUPLICADO;
        }

        Pedido pedido = pedidoRepository.findByCodigo(evento.codigoPedido())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido " + evento.codigoPedido() + " não encontrado."));

        switch (evento.tipo()) {
            case "pagamento.aprovado" -> pedido.marcarComoPago(clock.instant());
            case "pagamento.vencido" -> pedido.marcarComoVencido();
            case "pagamento.reembolsado" -> pedido.marcarComoReembolsado();
            default -> throw new IllegalArgumentException("Tipo de evento não suportado: " + evento.tipo());
        }

        eventoRepository.save(new EventoWebhook(
                evento.idEvento(), evento.tipo(), evento.codigoPedido(), clock.instant()));
        return ResultadoWebhook.PROCESSADO;
    }

    private static void validar(EventoPagamento evento) {
        if (evento == null || isVazio(evento.idEvento()) || isVazio(evento.tipo()) || isVazio(evento.codigoPedido())) {
            throw new IllegalArgumentException("Os campos idEvento, tipo e codigoPedido são obrigatórios.");
        }
    }

    private static boolean isVazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
