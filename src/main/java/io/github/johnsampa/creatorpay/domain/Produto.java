package io.github.johnsampa.creatorpay.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Produto digital (curso, e-book, mentoria...) de um produtor. */
@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Produtor produtor;

    protected Produto() {
    }

    public Produto(String nome, BigDecimal preco, Produtor produtor) {
        this.nome = nome;
        this.preco = preco;
        this.produtor = produtor;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public Produtor getProdutor() {
        return produtor;
    }
}
