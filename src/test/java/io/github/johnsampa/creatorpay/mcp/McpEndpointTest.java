package io.github.johnsampa.creatorpay.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de ponta a ponta do protocolo: sobe o servidor numa porta real e conversa com o endpoint
 * /mcp em JSON-RPC, como um cliente MCP (Claude, Cursor...) faria.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpEndpointTest {

    @Value("${local.server.port}")
    private int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void clienteMcpInicializaListaEChamaFerramentas() throws Exception {
        HttpResponse<String> init = enviar(null, """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{
                  "protocolVersion":"2025-06-18","capabilities":{},
                  "clientInfo":{"name":"teste","version":"1.0"}}}""");
        assertThat(init.statusCode()).isEqualTo(200);
        assertThat(init.body()).contains("creatorpay-mcp").contains("listarProdutores");

        String sessao = init.headers().firstValue("Mcp-Session-Id").orElseThrow();
        enviar(sessao, """
                {"jsonrpc":"2.0","method":"notifications/initialized"}""");

        String ferramentas = enviar(sessao, """
                {"jsonrpc":"2.0","id":2,"method":"tools/list"}""").body();
        assertThat(ferramentas).contains(
                "listarProdutores", "resumoDoProdutor", "vendasPorProduto", "statusPagamento",
                "carrinhosAbandonados", "pagamentosVencidos", "sugerirRecuperacao");

        String chamada = enviar(sessao, """
                {"jsonrpc":"2.0","id":3,"method":"tools/call",
                 "params":{"name":"statusPagamento","arguments":{"codigoPedido":"CP-10001"}}}""").body();
        assertThat(chamada).contains("CP-10001").contains("\"isError\":false");
    }

    private HttpResponse<String> enviar(String sessao, String json) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/mcp"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (sessao != null) {
            req.header("Mcp-Session-Id", sessao).header("MCP-Protocol-Version", "2025-06-18");
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }
}
