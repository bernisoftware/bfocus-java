package br.com.bernisoftware.bfocus;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Identidade do widget do bFocus (local, sem rede e sem chave de API). Rode no SEU backend: assine a identidade
 * do usuário logado e entregue a assinatura ao front, que abre o widget com ela.
 */
public final class WidgetIdentity {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private WidgetIdentity() {
    }

    /**
     * {@code HMAC-SHA256(secret, "v1:" + userExternalId + ":" + customerExternalId)} (UTF-8), em hexadecimal
     * minúsculo.
     *
     * @param secret segredo do widget (painel do bFocus). Nunca o envie ao navegador.
     * @param userExternalId {@code external_id} do usuário logado no seu sistema
     * @param customerExternalId {@code external_id} do cliente (empresa) desse usuário
     * @return assinatura hexadecimal minúscula (64 caracteres)
     * @throws IllegalArgumentException segredo vazio
     */
    public static String sign(String secret, String userExternalId, String customerExternalId) {
        Objects.requireNonNull(secret, "secret");
        Objects.requireNonNull(userExternalId, "userExternalId");
        Objects.requireNonNull(customerExternalId, "customerExternalId");
        if (secret.isEmpty()) {
            throw new IllegalArgumentException("O segredo do widget não pode ser vazio.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(("v1:" + userExternalId + ":" + customerExternalId).getBytes(StandardCharsets.UTF_8));
            char[] out = new char[digest.length * 2];
            for (int k = 0; k < digest.length; k++) {
                out[2 * k] = HEX[(digest[k] >> 4) & 0xF];
                out[2 * k + 1] = HEX[digest[k] & 0xF];
            }
            return new String(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível nesta JVM", e);
        }
    }
}
