package io.github.johnsampa.creatorpay.webhook;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    static final String HEADER_ASSINATURA = "X-CreatorPay-Signature";

    private final AssinaturaWebhook assinatura;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    public WebhookController(AssinaturaWebhook assinatura, WebhookService webhookService, ObjectMapper objectMapper) {
        this.assinatura = assinatura;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    /**
     * Recebe o corpo como bytes crus: a assinatura é calculada sobre os bytes exatos que o
     * gateway enviou. Se o JSON fosse convertido antes, qualquer diferença de espaço ou ordem
     * de campos quebraria a verificação.
     */
    @PostMapping("/pagamentos")
    public ResponseEntity<Map<String, String>> receberPagamento(
            @RequestHeader(name = HEADER_ASSINATURA, required = false) String cabecalhoAssinatura,
            @RequestBody byte[] corpo) {

        if (!assinatura.isValida(corpo, cabecalhoAssinatura)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Assinatura inválida."));
        }

        EventoPagamento evento = objectMapper.readValue(corpo, EventoPagamento.class);
        ResultadoWebhook resultado = webhookService.processar(evento);
        return ResponseEntity.ok(Map.of("resultado", resultado.name()));
    }
}
