package io.github.johnsampa.creatorpay.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.github.johnsampa.creatorpay.domain.Pedido;
import io.github.johnsampa.creatorpay.domain.StatusPedido;
import io.github.johnsampa.creatorpay.repository.PedidoRepository;

@SpringBootTest
class WebhookControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AssinaturaWebhook assinatura;

    @Autowired
    private PedidoRepository pedidoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void pagamentoAprovadoMudaOStatusDoPedido() throws Exception {
        Pedido vencido = umPedidoCom(StatusPedido.VENCIDO);

        enviar(evento(novoId(), "pagamento.aprovado", vencido.getCodigo()), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("PROCESSADO"));

        Pedido atualizado = pedidoRepository.findByCodigo(vencido.getCodigo()).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(atualizado.getPagoEm()).isNotNull();
    }

    @Test
    void mesmoEventoEnviadoDuasVezesSoEhAplicadoUmaVez() throws Exception {
        Pedido pago = umPedidoCom(StatusPedido.PAGO);
        String idEvento = novoId();
        String corpo = evento(idEvento, "pagamento.reembolsado", pago.getCodigo());

        enviar(corpo, true).andExpect(jsonPath("$.resultado").value("PROCESSADO"));
        enviar(corpo, true).andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("DUPLICADO"));

        assertThat(pedidoRepository.findByCodigo(pago.getCodigo()).orElseThrow().getStatus())
                .isEqualTo(StatusPedido.REEMBOLSADO);
    }

    @Test
    void assinaturaInvalidaRetorna401ENaoAlteraNada() throws Exception {
        Pedido vencido = umPedidoCom(StatusPedido.VENCIDO);

        enviar(evento(novoId(), "pagamento.aprovado", vencido.getCodigo()), false)
                .andExpect(status().isUnauthorized());

        assertThat(pedidoRepository.findByCodigo(vencido.getCodigo()).orElseThrow().getStatus())
                .isEqualTo(StatusPedido.VENCIDO);
    }

    @Test
    void pedidoInexistenteRetorna404() throws Exception {
        enviar(evento(novoId(), "pagamento.aprovado", "CP-00000"), true).andExpect(status().isNotFound());
    }

    @Test
    void tipoDeEventoDesconhecidoRetorna400() throws Exception {
        Pedido pedido = umPedidoCom(StatusPedido.PENDENTE);

        enviar(evento(novoId(), "pagamento.estornado_parcial", pedido.getCodigo()), true)
                .andExpect(status().isBadRequest());
    }

    @Test
    void jsonInvalidoRetorna400() throws Exception {
        enviar("{isso nao e json", true).andExpect(status().isBadRequest());
    }

    private ResultActions enviar(String corpo, boolean assinar) throws Exception {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        String header = assinar ? assinatura.assinar(bytes) : "sha256=" + "0".repeat(64);
        return mockMvc.perform(post("/webhooks/pagamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .header(WebhookController.HEADER_ASSINATURA, header)
                .content(bytes));
    }

    private Pedido umPedidoCom(StatusPedido status) {
        return pedidoRepository.findAll().stream()
                .filter(p -> p.getStatus() == status)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Nenhum pedido com status " + status));
    }

    private static String evento(String idEvento, String tipo, String codigoPedido) {
        return """
                {"idEvento":"%s","tipo":"%s","codigoPedido":"%s"}""".formatted(idEvento, tipo, codigoPedido);
    }

    private static String novoId() {
        return "evt_" + UUID.randomUUID();
    }
}
