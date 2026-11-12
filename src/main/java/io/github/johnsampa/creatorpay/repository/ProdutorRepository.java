package io.github.johnsampa.creatorpay.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.johnsampa.creatorpay.domain.Produtor;
import io.github.johnsampa.creatorpay.dto.ProdutorResumo;

public interface ProdutorRepository extends JpaRepository<Produtor, Long> {

    @Query("""
            select new io.github.johnsampa.creatorpay.dto.ProdutorResumo(
                pr.id, pr.nome, pr.nicho, count(p))
            from Produtor pr left join Produto p on p.produtor = pr
            group by pr.id, pr.nome, pr.nicho
            order by pr.id
            """)
    List<ProdutorResumo> listarComQuantidadeDeProdutos();
}
