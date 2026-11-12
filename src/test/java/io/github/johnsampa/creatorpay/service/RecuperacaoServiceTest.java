package io.github.johnsampa.creatorpay.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.johnsampa.creatorpay.domain.MetodoPagamento;
import io.github.johnsampa.creatorpay.domain.Pedido;
import io.github.johnsampa.creatorpay.domain.Produto;
import io.github.johnsampa.creatorpay.domain.Produtor;
import io.github.johnsampa.creatorpay.dto.SugestaoRecuperacao;

/** Testa só as regras (sem Spring e sem banco): rápido e sem depender de dados gerados. */
class RecuperacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-12T12:00:00Z");
    private static final Produto PRODUTO =
            new Produto("Curso Java do Zero", new BigDecimal("397.00"), new Produtor("Código do Zero", "Programação"));

    @Test
    void boletoVencidoSugereTrocarPorPixNoWhatsApp() {
        Pedido pedido = pedido(MetodoPagamento.BOLETO, Duration.ofDays(5));
        pedido.marcarComoVencido();

        SugestaoRecuperacao sugestao = RecuperacaoService.sugerirPara(pedido, AGORA);

        assertThat(sugestao.acaoNecessaria()).isTrue();
        assertThat(sugestao.canal()).isEqualTo("WhatsApp");
        assertThat(sugestao.acao()).contains("PIX");
        assertThat(sugestao.mensagemSugerida()).startsWith("Oi, Ana!").contains("Curso Java do Zero");
    }

    @Test
    void pixVencidoSugereGerarNovoPix() {
        Pedido pedido = pedido(MetodoPagamento.PIX, Duration.ofHours(2));
        pedido.marcarComoVencido();

        assertThat(RecuperacaoService.sugerirPara(pedido, AGORA).acao()).contains("novo PIX");
    }

    @Test
    void carrinhoRecenteRecebeLembreteSemDesconto() {
        Pedido pedido = pedido(MetodoPagamento.CARTAO, Duration.ofHours(3));
        pedido.marcarComoAbandonado();

        SugestaoRecuperacao sugestao = RecuperacaoService.sugerirPara(pedido, AGORA);

        assertThat(sugestao.canal()).isEqualTo("E-mail");
        assertThat(sugestao.mensagemSugerida()).doesNotContain("cupom");
    }

    @Test
    void carrinhoEntre24e72HorasRecebeCupomDe10() {
        Pedido pedido = pedido(MetodoPagamento.CARTAO, Duration.ofHours(48));
        pedido.marcarComoAbandonado();

        SugestaoRecuperacao sugestao = RecuperacaoService.sugerirPara(pedido, AGORA);

        assertThat(sugestao.canal()).isEqualTo("WhatsApp");
        assertThat(sugestao.mensagemSugerida()).contains("VOLTA10");
    }

    @Test
    void carrinhoAntigoRecebeUltimaTentativaComCupomDe15() {
        Pedido pedido = pedido(MetodoPagamento.PIX, Duration.ofDays(5));
        pedido.marcarComoAbandonado();

        assertThat(RecuperacaoService.sugerirPara(pedido, AGORA).mensagemSugerida()).contains("ULTIMACHANCE15");
    }

    @Test
    void limiteDe24HorasExatasJaRecebeCupom() {
        Pedido pedido = pedido(MetodoPagamento.CARTAO, Duration.ofHours(24));
        pedido.marcarComoAbandonado();

        assertThat(RecuperacaoService.sugerirPara(pedido, AGORA).mensagemSugerida()).contains("VOLTA10");
    }

    @Test
    void pedidoPagoNaoPrecisaDeAcao() {
        Pedido pedido = pedido(MetodoPagamento.PIX, Duration.ofDays(1));
        pedido.marcarComoPago(AGORA);

        SugestaoRecuperacao sugestao = RecuperacaoService.sugerirPara(pedido, AGORA);

        assertThat(sugestao.acaoNecessaria()).isFalse();
        assertThat(sugestao.mensagemSugerida()).isNull();
    }

    @Test
    void pedidoReembolsadoNaoDeveSerRecuperado() {
        Pedido pedido = pedido(MetodoPagamento.CARTAO, Duration.ofDays(10));
        pedido.marcarComoPago(AGORA.minus(Duration.ofDays(9)));
        pedido.marcarComoReembolsado();

        assertThat(RecuperacaoService.sugerirPara(pedido, AGORA).acaoNecessaria()).isFalse();
    }

    private static Pedido pedido(MetodoPagamento metodo, Duration criadoHa) {
        Instant criadoEm = AGORA.minus(criadoHa);
        return new Pedido("CP-99999", PRODUTO, "Ana Souza", "ana@email.test", metodo, criadoEm,
                criadoEm.plus(Duration.ofDays(3)));
    }
}
