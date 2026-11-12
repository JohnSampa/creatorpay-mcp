package io.github.johnsampa.creatorpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.johnsampa.creatorpay.domain.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
