package io.github.johnsampa.creatorpay.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro de cada evento de webhook já processado. O ID do evento é a chave primária:
 * é isso que garante a idempotência (o mesmo evento entregue duas vezes só é aplicado uma vez).
 */
@Entity
@Table(name = "eventos_webhook")
public class EventoWebhook {

    @Id
    private String idEvento;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String codigoPedido;

    @Column(nullable = false)
    private Instant recebidoEm;

    protected EventoWebhook() {
    }

    public EventoWebhook(String idEvento, String tipo, String codigoPedido, Instant recebidoEm) {
        this.idEvento = idEvento;
        this.tipo = tipo;
        this.codigoPedido = codigoPedido;
        this.recebidoEm = recebidoEm;
    }

    public String getIdEvento() {
        return idEvento;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCodigoPedido() {
        return codigoPedido;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }
}
