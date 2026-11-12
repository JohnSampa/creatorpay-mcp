package io.github.johnsampa.creatorpay.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AssinaturaWebhookTest {

    private final AssinaturaWebhook assinatura = new AssinaturaWebhook("segredo-de-teste");
    private final byte[] corpo = """
            {"idEvento":"evt_1","tipo":"pagamento.aprovado","codigoPedido":"CP-10001"}
            """.getBytes(StandardCharsets.UTF_8);

    @Test
    void aceitaAssinaturaCorreta() {
        assertThat(assinatura.isValida(corpo, assinatura.assinar(corpo))).isTrue();
    }

    @Test
    void rejeitaCorpoAlterado() {
        String header = assinatura.assinar(corpo);
        byte[] alterado = new String(corpo, StandardCharsets.UTF_8)
                .replace("CP-10001", "CP-10002").getBytes(StandardCharsets.UTF_8);

        assertThat(assinatura.isValida(alterado, header)).isFalse();
    }

    @Test
    void rejeitaAssinaturaFeitaComOutroSegredo() {
        String headerDeOutroSegredo = new AssinaturaWebhook("outro-segredo").assinar(corpo);

        assertThat(assinatura.isValida(corpo, headerDeOutroSegredo)).isFalse();
    }

    @Test
    void rejeitaHeaderAusenteOuMalFormado() {
        assertThat(assinatura.isValida(corpo, null)).isFalse();
        assertThat(assinatura.isValida(corpo, "md5=abc")).isFalse();
        assertThat(assinatura.isValida(corpo, "sha256=nao-e-hex")).isFalse();
    }

    @Test
    void naoSobeSemSegredoConfigurado() {
        assertThatThrownBy(() -> new AssinaturaWebhook(" ")).isInstanceOf(IllegalStateException.class);
    }
}
