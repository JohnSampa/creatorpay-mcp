package io.github.johnsampa.creatorpay.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.johnsampa.creatorpay.domain.StatusPedido;
import io.github.johnsampa.creatorpay.dto.PedidoResumo;
import io.github.johnsampa.creatorpay.dto.ProdutorResumo;
import io.github.johnsampa.creatorpay.dto.ResumoProdutor;
import io.github.johnsampa.creatorpay.dto.VendasPorProduto;
import io.github.johnsampa.creatorpay.service.RecursoNaoEncontradoException;

/** Chama as ferramentas como o agente chamaria, com os dados fictícios gerados na inicialização. */
@SpringBootTest
class CreatorPayMcpToolsTest {

    @Autowired
    private CreatorPayMcpTools tools;

    @Test
    void listaOsTresProdutoresComSeusProdutos() {
        List<ProdutorResumo> produtores = tools.listarProdutores();

        assertThat(produtores).extracting(ProdutorResumo::nome)
                .containsExactly("Ana Fit", "Código do Zero", "Cozinha da Lu");
        assertThat(produtores).allSatisfy(p -> assertThat(p.quantidadeProdutos()).isPositive());
    }

    @Test
    void resumoTemNumerosConsistentes() {
        Long id = idDoProdutor("Código do Zero");

        ResumoProdutor resumo = tools.resumoDoProdutor(id, 60);

        assertThat(resumo.produtor()).isEqualTo("Código do Zero");
        assertThat(resumo.periodoDias()).isEqualTo(60);
        assertThat(resumo.faturamento()).isPositive();
        assertThat(resumo.taxaConversaoPercentual()).isBetween(BigDecimal.ZERO, new BigDecimal("100"));
        assertThat(resumo.ticketMedio().multiply(BigDecimal.valueOf(resumo.vendasPagas())))
                .isCloseTo(resumo.faturamento(), org.assertj.core.data.Offset.offset(new BigDecimal("1.00")));
    }

    @Test
    void usaPeriodoPadraoDe30DiasQuandoNaoInformado() {
        assertThat(tools.resumoDoProdutor(idDoProdutor("Ana Fit"), null).periodoDias()).isEqualTo(30);
    }

    @Test
    void rankingDeProdutosVemDoMaiorParaOMenorFaturamento() {
        List<VendasPorProduto> ranking = tools.vendasPorProduto(idDoProdutor("Cozinha da Lu"), 60);

        assertThat(ranking).isNotEmpty();
        assertThat(ranking).extracting(VendasPorProduto::faturamento)
                .isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void carrinhosAbandonadosSoTrazemEsseStatusComEmailMascarado() {
        List<PedidoResumo> carrinhos = tools.carrinhosAbandonados(idDoProdutor("Código do Zero"), 60);

        assertThat(carrinhos).isNotEmpty().hasSizeLessThanOrEqualTo(20);
        assertThat(carrinhos).allSatisfy(p -> {
            assertThat(p.status()).isEqualTo(StatusPedido.CARRINHO_ABANDONADO);
            assertThat(p.emailMascarado()).contains("***@");
        });
    }

    @Test
    void statusPagamentoAceitaCodigoEmMinusculo() {
        assertThat(tools.statusPagamento(" cp-10001 ").codigo()).isEqualTo("CP-10001");
    }

    @Test
    void produtorInexistenteOrientaOAgente() {
        assertThatThrownBy(() -> tools.resumoDoProdutor(999L, 30))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("listarProdutores");
    }

    @Test
    void periodoForaDoLimiteEhRejeitado() {
        Long id = idDoProdutor("Ana Fit");

        assertThatThrownBy(() -> tools.resumoDoProdutor(id, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tools.resumoDoProdutor(id, 365)).isInstanceOf(IllegalArgumentException.class);
    }

    private Long idDoProdutor(String nome) {
        return tools.listarProdutores().stream()
                .filter(p -> p.nome().equals(nome))
                .findFirst()
                .orElseThrow()
                .id();
    }
}
