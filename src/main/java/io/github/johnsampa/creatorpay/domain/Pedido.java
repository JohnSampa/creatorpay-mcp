package io.github.johnsampa.creatorpay.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Pedido de compra de um produto. O status só muda pelos métodos de transição abaixo,
 * para que a regra de negócio não fique espalhada em setters.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código público do pedido (ex.: CP-10001). O ID interno nunca é exposto. */
    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Produto produto;

    @Column(nullable = false)
    private String nomeComprador;

    @Column(nullable = false)
    private String emailComprador;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @Column(nullable = false)
    private Instant criadoEm;

    private Instant pagoEm;

    private Instant vencimentoEm;

    protected Pedido() {
    }

    public Pedido(String codigo, Produto produto, String nomeComprador, String emailComprador,
                  MetodoPagamento metodo, Instant criadoEm, Instant vencimentoEm) {
        this.codigo = codigo;
        this.produto = produto;
        this.nomeComprador = nomeComprador;
        this.emailComprador = emailComprador;
        this.valor = produto.getPreco();
        this.metodo = metodo;
        this.criadoEm = criadoEm;
        this.vencimentoEm = vencimentoEm;
        this.status = StatusPedido.PENDENTE;
    }

    public void marcarComoPago(Instant quando) {
        this.status = StatusPedido.PAGO;
        this.pagoEm = quando;
    }

    public void marcarComoVencido() {
        this.status = StatusPedido.VENCIDO;
    }

    public void marcarComoAbandonado() {
        this.status = StatusPedido.CARRINHO_ABANDONADO;
    }

    public void marcarComoReembolsado() {
        this.status = StatusPedido.REEMBOLSADO;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Produto getProduto() {
        return produto;
    }

    public String getNomeComprador() {
        return nomeComprador;
    }

    public String getEmailComprador() {
        return emailComprador;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public MetodoPagamento getMetodo() {
        return metodo;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getPagoEm() {
        return pagoEm;
    }

    public Instant getVencimentoEm() {
        return vencimentoEm;
    }
}
