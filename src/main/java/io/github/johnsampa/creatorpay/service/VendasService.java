package io.github.johnsampa.creatorpay.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.johnsampa.creatorpay.domain.Pedido;
import io.github.johnsampa.creatorpay.domain.Produtor;
import io.github.johnsampa.creatorpay.domain.StatusPedido;
import io.github.johnsampa.creatorpay.dto.PedidoResumo;
import io.github.johnsampa.creatorpay.dto.ProdutorResumo;
import io.github.johnsampa.creatorpay.dto.ResumoProdutor;
import io.github.johnsampa.creatorpay.dto.VendasPorProduto;
import io.github.johnsampa.creatorpay.repository.PedidoRepository;
import io.github.johnsampa.creatorpay.repository.ProdutorRepository;

/** Consultas de vendas usadas pelas ferramentas MCP. Tudo somente leitura. */
@Service
@Transactional(readOnly = true)
public class VendasService {

    static final int DIAS_MAXIMO = 90;
    static final int LIMITE_LISTAS = 20;

    private final ProdutorRepository produtorRepository;
    private final PedidoRepository pedidoRepository;
    private final Clock clock;

    public VendasService(ProdutorRepository produtorRepository, PedidoRepository pedidoRepository, Clock clock) {
        this.produtorRepository = produtorRepository;
        this.pedidoRepository = pedidoRepository;
        this.clock = clock;
    }

    public List<ProdutorResumo> listarProdutores() {
        return produtorRepository.listarComQuantidadeDeProdutos();
    }

    public ResumoProdutor resumoDoProdutor(Long produtorId, int dias) {
        Produtor produtor = buscarProdutor(produtorId);
        List<Pedido> pedidos = pedidoRepository
                .findByProdutoProdutorIdAndCriadoEmGreaterThanEqual(produtorId, inicioDoPeriodo(dias));

        Map<StatusPedido, List<Pedido>> porStatus = pedidos.stream()
                .collect(Collectors.groupingBy(Pedido::getStatus));
        List<Pedido> pagos = porStatus.getOrDefault(StatusPedido.PAGO, List.of());
        List<Pedido> abandonados = porStatus.getOrDefault(StatusPedido.CARRINHO_ABANDONADO, List.of());
        List<Pedido> vencidos = porStatus.getOrDefault(StatusPedido.VENCIDO, List.of());

        BigDecimal faturamento = somar(pagos);
        BigDecimal ticketMedio = pagos.isEmpty()
                ? BigDecimal.ZERO
                : faturamento.divide(BigDecimal.valueOf(pagos.size()), 2, RoundingMode.HALF_UP);
        BigDecimal taxaConversao = pedidos.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(pagos.size() * 100L)
                        .divide(BigDecimal.valueOf(pedidos.size()), 1, RoundingMode.HALF_UP);

        return new ResumoProdutor(
                produtor.getNome(),
                dias,
                faturamento,
                pagos.size(),
                ticketMedio,
                taxaConversao,
                abandonados.size(),
                somar(abandonados),
                vencidos.size(),
                somar(vencidos));
    }

    public List<VendasPorProduto> vendasPorProduto(Long produtorId, int dias) {
        buscarProdutor(produtorId);
        return pedidoRepository.somarVendasPorProduto(produtorId, StatusPedido.PAGO, inicioDoPeriodo(dias));
    }

    public List<PedidoResumo> carrinhosAbandonados(Long produtorId, int dias) {
        buscarProdutor(produtorId);
        return pedidoRepository
                .findByProdutoProdutorIdAndStatusAndCriadoEmGreaterThanEqualOrderByValorDesc(
                        produtorId, StatusPedido.CARRINHO_ABANDONADO, inicioDoPeriodo(dias))
                .stream()
                .limit(LIMITE_LISTAS)
                .map(PedidoResumo::de)
                .toList();
    }

    public List<PedidoResumo> pagamentosVencidos(Long produtorId) {
        buscarProdutor(produtorId);
        return pedidoRepository
                .findByProdutoProdutorIdAndStatusOrderByVencimentoEmDesc(produtorId, StatusPedido.VENCIDO)
                .stream()
                .limit(LIMITE_LISTAS)
                .map(PedidoResumo::de)
                .toList();
    }

    public PedidoResumo statusPagamento(String codigoPedido) {
        return PedidoResumo.de(buscarPedido(pedidoRepository, codigoPedido));
    }

    static Pedido buscarPedido(PedidoRepository repository, String codigoPedido) {
        if (codigoPedido == null || codigoPedido.isBlank()) {
            throw new IllegalArgumentException("Informe o código do pedido, no formato CP-10001.");
        }
        String codigo = codigoPedido.trim().toUpperCase(Locale.ROOT);
        return repository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido " + codigo + " não encontrado. Confira o código (formato CP-10001)."));
    }

    private Produtor buscarProdutor(Long produtorId) {
        if (produtorId == null) {
            throw new IllegalArgumentException("Informe o ID do produtor. Use listarProdutores para descobrir o ID.");
        }
        return produtorRepository.findById(produtorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produtor " + produtorId + " não encontrado. Use listarProdutores para ver os IDs válidos."));
    }

    private Instant inicioDoPeriodo(int dias) {
        if (dias < 1 || dias > DIAS_MAXIMO) {
            throw new IllegalArgumentException("O período deve ser de 1 a " + DIAS_MAXIMO + " dias.");
        }
        return clock.instant().minus(Duration.ofDays(dias));
    }

    private static BigDecimal somar(List<Pedido> pedidos) {
        return pedidos.stream().map(Pedido::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
