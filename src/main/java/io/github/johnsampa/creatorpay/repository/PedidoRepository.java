package io.github.johnsampa.creatorpay.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.johnsampa.creatorpay.domain.Pedido;
import io.github.johnsampa.creatorpay.domain.StatusPedido;
import io.github.johnsampa.creatorpay.dto.VendasPorProduto;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = "produto")
    Optional<Pedido> findByCodigo(String codigo);

    List<Pedido> findByProdutoProdutorIdAndCriadoEmGreaterThanEqual(Long produtorId, Instant desde);

    @EntityGraph(attributePaths = "produto")
    List<Pedido> findByProdutoProdutorIdAndStatusAndCriadoEmGreaterThanEqualOrderByValorDesc(
            Long produtorId, StatusPedido status, Instant desde);

    @EntityGraph(attributePaths = "produto")
    List<Pedido> findByProdutoProdutorIdAndStatusOrderByVencimentoEmDesc(Long produtorId, StatusPedido status);

    /** Agregação feita no banco (JPQL), não em memória. */
    @Query("""
            select new io.github.johnsampa.creatorpay.dto.VendasPorProduto(
                p.produto.nome, count(p), sum(p.valor))
            from Pedido p
            where p.produto.produtor.id = :produtorId
              and p.status = :status
              and p.criadoEm >= :desde
            group by p.produto.nome
            order by sum(p.valor) desc
            """)
    List<VendasPorProduto> somarVendasPorProduto(@Param("produtorId") Long produtorId,
                                                 @Param("status") StatusPedido status,
                                                 @Param("desde") Instant desde);
}
