package io.github.johnsampa.creatorpay.webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Verifica a assinatura HMAC-SHA256 dos webhooks, no formato do header
 * {@code X-CreatorPay-Signature: sha256=<hex>}, calculada sobre o corpo cru da requisição.
 *
 * Por que HMAC: qualquer um pode chamar a URL do webhook. A assinatura prova que quem enviou
 * conhece o segredo compartilhado e que o corpo não foi alterado no caminho.
 */
@Component
public class AssinaturaWebhook {

    static final String PREFIXO = "sha256=";
    private static final String ALGORITMO = "HmacSHA256";

    private final byte[] segredo;

    public AssinaturaWebhook(@Value("${creatorpay.webhook.secret}") String segredo) {
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException("Defina creatorpay.webhook.secret (ou CREATORPAY_WEBHOOK_SECRET).");
        }
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValida(byte[] corpo, String cabecalho) {
        if (corpo == null || cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            return false;
        }
        byte[] recebida;
        try {
            recebida = HexFormat.of().parseHex(cabecalho.substring(PREFIXO.length()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        // Comparação em tempo constante: não "vaza" quantos bytes acertaram (ataque de timing).
        return MessageDigest.isEqual(calcular(corpo), recebida);
    }

    /** Gera o valor do header para um corpo. Usado nos testes e no exemplo do README. */
    public String assinar(byte[] corpo) {
        return PREFIXO + HexFormat.of().formatHex(calcular(corpo));
    }

    private byte[] calcular(byte[] corpo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(segredo, ALGORITMO));
            return mac.doFinal(corpo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível calcular o HMAC", e);
        }
    }
}
