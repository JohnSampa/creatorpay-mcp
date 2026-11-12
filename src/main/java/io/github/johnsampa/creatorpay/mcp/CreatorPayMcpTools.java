package io.github.johnsampa.creatorpay.mcp;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import io.github.johnsampa.creatorpay.dto.PedidoResumo;
import io.github.johnsampa.creatorpay.dto.ProdutorResumo;
import io.github.johnsampa.creatorpay.dto.ResumoProdutor;
import io.github.johnsampa.creatorpay.dto.SugestaoRecuperacao;
import io.github.johnsampa.creatorpay.dto.VendasPorProduto;
import io.github.johnsampa.creatorpay.service.RecuperacaoService;
import io.github.johnsampa.creatorpay.service.VendasService;

/**
 * Ferramentas expostas aos agentes de IA via MCP.
 *
 * Esta classe faz o papel de um controller: não tem regra de negócio, só traduz a chamada do
 * agente para os serviços. As descrições são escritas para o modelo, não para pessoas: dizem
 * quando usar cada ferramenta, o formato dos parâmetros e o que fazer antes ou depois.
 */
@Component
public class CreatorPayMcpTools {

    private static final int DIAS_PADRAO = 30;

    private final VendasService vendasService;
    private final RecuperacaoService recuperacaoService;

    public CreatorPayMcpTools(VendasService vendasService, RecuperacaoService recuperacaoService) {
        this.vendasService = vendasService;
        this.recuperacaoService = recuperacaoService;
    }

    @McpTool(name = "listarProdutores",
            description = "Lista os produtores (creators) da plataforma com ID, nome, nicho e quantidade de produtos. "
                    + "Use primeiro quando o usuário citar um produtor pelo nome, para descobrir o ID.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public List<ProdutorResumo> listarProdutores() {
        return vendasService.listarProdutores();
    }

    @McpTool(name = "resumoDoProdutor",
            description = "Resumo de vendas de um produtor no período: faturamento, número de vendas pagas, ticket médio, "
                    + "taxa de conversão (%) e quanto há em carrinhos abandonados e pagamentos vencidos. "
                    + "Bom ponto de partida para perguntas gerais como 'como estão as vendas do produtor X?'.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public ResumoProdutor resumoDoProdutor(
            @McpToolParam(description = "ID do produtor (obtido em listarProdutores)") Long produtorId,
            @McpToolParam(description = "Período em dias, de 1 a 90. Padrão: 30", required = false) Integer dias) {
        return vendasService.resumoDoProdutor(produtorId, diasOuPadrao(dias));
    }

    @McpTool(name = "vendasPorProduto",
            description = "Ranking dos produtos de um produtor por faturamento no período (somente vendas pagas).",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public List<VendasPorProduto> vendasPorProduto(
            @McpToolParam(description = "ID do produtor (obtido em listarProdutores)") Long produtorId,
            @McpToolParam(description = "Período em dias, de 1 a 90. Padrão: 30", required = false) Integer dias) {
        return vendasService.vendasPorProduto(produtorId, diasOuPadrao(dias));
    }

    @McpTool(name = "statusPagamento",
            description = "Consulta um pedido pelo código (formato CP-10001): status, método de pagamento, valor e datas. "
                    + "O e-mail do comprador vem mascarado.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public PedidoResumo statusPagamento(
            @McpToolParam(description = "Código do pedido, ex.: CP-10001") String codigoPedido) {
        return vendasService.statusPagamento(codigoPedido);
    }

    @McpTool(name = "carrinhosAbandonados",
            description = "Lista até 20 carrinhos abandonados de um produtor no período, do maior para o menor valor. "
                    + "Para cada um, chame sugerirRecuperacao se o usuário quiser recuperar a venda.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public List<PedidoResumo> carrinhosAbandonados(
            @McpToolParam(description = "ID do produtor (obtido em listarProdutores)") Long produtorId,
            @McpToolParam(description = "Período em dias, de 1 a 90. Padrão: 30", required = false) Integer dias) {
        return vendasService.carrinhosAbandonados(produtorId, diasOuPadrao(dias));
    }

    @McpTool(name = "pagamentosVencidos",
            description = "Lista até 20 pedidos com PIX ou boleto vencido de um produtor, do vencimento mais recente "
                    + "para o mais antigo. Para cada um, chame sugerirRecuperacao se o usuário quiser recuperar a venda.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public List<PedidoResumo> pagamentosVencidos(
            @McpToolParam(description = "ID do produtor (obtido em listarProdutores)") Long produtorId) {
        return vendasService.pagamentosVencidos(produtorId);
    }

    @McpTool(name = "sugerirRecuperacao",
            description = "Sugere como recuperar a venda de um pedido: canal (WhatsApp ou e-mail), ação e um rascunho de "
                    + "mensagem. As regras são fixas e dependem do status, do método de pagamento e de há quanto tempo o "
                    + "carrinho foi abandonado. Esta ferramenta NÃO envia nada: só sugere.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public SugestaoRecuperacao sugerirRecuperacao(
            @McpToolParam(description = "Código do pedido, ex.: CP-10001") String codigoPedido) {
        return recuperacaoService.sugerir(codigoPedido);
    }

    private static int diasOuPadrao(Integer dias) {
        return dias == null ? DIAS_PADRAO : dias;
    }
}
