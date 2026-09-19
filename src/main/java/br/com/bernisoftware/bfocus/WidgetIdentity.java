package br.com.bernisoftware.bfocus;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Identidade do widget do bFocus (local, sem rede e sem chave de API). Rode no SEU backend: assine a identidade
 * do usuário logado e entregue a assinatura ao front, que abre o widget com ela.
 *
 * <p>Duas versões, ambas aceitas pela API: {@link #sign} (v1, sem validade) e {@link #signV2} (v2, com validade —
 * vale de 7 dias atrás até 5 minutos à frente; gere a cada renderização da página e nunca guarde).
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
        return hmacHex(secret, "v1:" + userExternalId + ":" + customerExternalId);
    }

    /**
     * Identidade v2 (com validade), assinada agora. Veja {@link #signV2(String, String, String, Instant)}.
     *
     * @param secret segredo do widget (painel do bFocus). Nunca o envie ao navegador.
     * @param userExternalId {@code external_id} do usuário logado no seu sistema (sem {@code :})
     * @param customerExternalId {@code external_id} do cliente (empresa) desse usuário
     * @return {@code "v2.<ts>.<hex>"}
     * @throws IllegalArgumentException segredo vazio ou {@code :} no id do usuário
     */
    public static String signV2(String secret, String userExternalId, String customerExternalId) {
        return signV2(secret, userExternalId, customerExternalId, Instant.now());
    }

    /**
     * Identidade v2 (com validade): {@code "v2.<ts>.<hex>"}, com {@code ts} = segundos unix inteiros de {@code now}
     * e {@code hex} = {@code HMAC-SHA256(secret, "v2:" + ts + ":" + userExternalId + ":" + customerExternalId)}
     * (UTF-8) em hexadecimal minúsculo. A API aceita de 7 dias atrás até 5 minutos à frente: gere a cada
     * renderização da página, nunca guarde. Entregue no mesmo lugar da v1 ({@code userHash} do widget).
     *
     * @param secret segredo do widget (painel do bFocus). Nunca o envie ao navegador.
     * @param userExternalId {@code external_id} do usuário logado no seu sistema — sem {@code :} (é o separador; o
     *        id do cliente pode ter)
     * @param customerExternalId {@code external_id} do cliente (empresa) desse usuário
     * @param now o instante da assinatura (fração de segundo descartada)
     * @return {@code "v2.<ts>.<hex>"}
     * @throws IllegalArgumentException segredo vazio, {@code :} no id do usuário ou instante antes de 1970
     */
    public static String signV2(String secret, String userExternalId, String customerExternalId, Instant now) {
        Objects.requireNonNull(secret, "secret");
        Objects.requireNonNull(userExternalId, "userExternalId");
        Objects.requireNonNull(customerExternalId, "customerExternalId");
        Objects.requireNonNull(now, "now");
        if (secret.isEmpty()) {
            throw new IllegalArgumentException("O segredo do widget não pode ser vazio.");
        }
        if (userExternalId.indexOf(':') >= 0) {
            throw new IllegalArgumentException("O external_id do usuário não pode ter ':' na identidade v2 (é o separador; a API recusa)"
                    + " — use '-' (ex.: 'app-77').");
        }
        long ts = now.getEpochSecond();
        if (ts < 0) {
            throw new IllegalArgumentException("O instante da assinatura não pode ser anterior a 1970 (segundos unix negativos).");
        }
        return "v2." + ts + "." + hmacHex(secret, "v2:" + ts + ":" + userExternalId + ":" + customerExternalId);
    }

    private static String hmacHex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
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
