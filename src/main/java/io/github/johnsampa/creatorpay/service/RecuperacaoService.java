package io.github.johnsampa.creatorpay.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.johnsampa.creatorpay.domain.MetodoPagamento;
import io.github.johnsampa.creatorpay.domain.Pedido;
import io.github.johnsampa.creatorpay.dto.SugestaoRecuperacao;
import io.github.johnsampa.creatorpay.repository.PedidoRepository;

/**
 * Regras de recuperação de vendas. São regras fixas (não é a IA que decide): o agente
 * consulta a sugestão e só redige ou envia a mensagem. Assim o comportamento é previsível e testável.
 */
@Service
@Transactional(readOnly = true)
public class RecuperacaoService {

    static final long HORAS_LEMBRETE_SIMPLES = 24;
    static final long HORAS_CUPOM_10 = 72;

    private final PedidoRepository pedidoRepository;
    private final Clock clock;

    public RecuperacaoService(PedidoRepository pedidoRepository, Clock clock) {
        this.pedidoRepository = pedidoRepository;
        this.clock = clock;
    }

    public SugestaoRecuperacao sugerir(String codigoPedido) {
        Pedido pedido = VendasService.buscarPedido(pedidoRepository, codigoPedido);
        return sugerirPara(pedido, clock.instant());
    }

    /** Regra pura: recebe o pedido e o "agora", sem acessar banco. */
    static SugestaoRecuperacao sugerirPara(Pedido pedido, Instant agora) {
        String nome = primeiroNome(pedido.getNomeComprador());
        String produto = pedido.getProduto().getNome();

        return switch (pedido.getStatus()) {
            case VENCIDO -> sugerirParaVencido(pedido, nome, produto);
            case CARRINHO_ABANDONADO -> sugerirParaCarrinho(pedido, nome, produto, agora);
            case PENDENTE -> semAcao(pedido, "Aguardar: o pagamento ainda está dentro do prazo.");
            case PAGO -> semAcao(pedido, "Nenhuma: o pedido já foi pago.");
            case REEMBOLSADO -> semAcao(pedido, "Nenhuma: pedido reembolsado, não fazer recuperação.");
        };
    }

    private static SugestaoRecuperacao sugerirParaVencido(Pedido pedido, String nome, String produto) {
        if (pedido.getMetodo() == MetodoPagamento.BOLETO) {
            return comAcao(pedido, "WhatsApp",
                    "Reenviar a cobrança por PIX, que aprova na hora, no lugar do boleto vencido.",
                    "Oi, " + nome + "! Vi que o boleto do " + produto + " venceu. "
                            + "Gerei um PIX pra você garantir seu acesso agora: [link]");
        }
        if (pedido.getMetodo() == MetodoPagamento.PIX) {
            return comAcao(pedido, "WhatsApp",
                    "Gerar um novo PIX e reenviar ao comprador.",
                    "Oi, " + nome + "! O PIX do " + produto + " expirou, mas é rapidinho gerar outro: [link]");
        }
        return comAcao(pedido, "E-mail",
                "Sugerir nova tentativa com outro cartão ou com PIX.",
                "Oi, " + nome + "! Não conseguimos aprovar o pagamento do " + produto
                        + ". Você pode tentar outro cartão ou pagar por PIX: [link]");
    }

    private static SugestaoRecuperacao sugerirParaCarrinho(Pedido pedido, String nome, String produto, Instant agora) {
        long horas = Duration.between(pedido.getCriadoEm(), agora).toHours();

        if (horas < HORAS_LEMBRETE_SIMPLES) {
            return comAcao(pedido, "E-mail",
                    "Lembrete simples, sem desconto (carrinho abandonado há menos de 24h).",
                    "Oi, " + nome + "! Você deixou o " + produto + " no carrinho. "
                            + "Ficou alguma dúvida? Seu acesso está a um clique: [link]");
        }
        if (horas < HORAS_CUPOM_10) {
            return comAcao(pedido, "WhatsApp",
                    "Lembrete com cupom de 10% (carrinho abandonado entre 24h e 72h).",
                    "Oi, " + nome + "! O " + produto + " ainda está te esperando. "
                            + "Use o cupom VOLTA10 e ganhe 10% de desconto: [link]");
        }
        return comAcao(pedido, "E-mail",
                "Última tentativa: cupom de 15% válido por 48h (carrinho abandonado há mais de 72h).",
                "Oi, " + nome + "! Última chamada: o cupom ULTIMACHANCE15 dá 15% de desconto no "
                        + produto + " e vale só até amanhã: [link]");
    }

    private static SugestaoRecuperacao comAcao(Pedido pedido, String canal, String acao, String mensagem) {
        return new SugestaoRecuperacao(pedido.getCodigo(), pedido.getStatus(), true, canal, acao, mensagem);
    }

    private static SugestaoRecuperacao semAcao(Pedido pedido, String acao) {
        return new SugestaoRecuperacao(pedido.getCodigo(), pedido.getStatus(), false, "nenhum", acao, null);
    }

    private static String primeiroNome(String nomeCompleto) {
        return nomeCompleto.trim().split("\\s+")[0];
    }
}
