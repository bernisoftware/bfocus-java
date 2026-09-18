package br.com.bernisoftware.bfocus;

import java.time.Duration;
import java.util.Map;

/**
 * 429 — limite de requisições da chave. A SDK já esperou e tentou de novo; esta exceção vem quando as novas
 * tentativas acabaram. Espera sugerida em {@link #getRetryAfter()}.
 */
public class RateLimitException extends BfocusException {
    private static final long serialVersionUID = 1L;

    /**
     * Cria o erro.
     *
     * @param code código estável
     * @param status status HTTP
     * @param message texto legível
     * @param requestId id da requisição
     * @param validation campo → motivo
     * @param retryAfter espera pedida pela API
     * @param requiredScope escopo exigido
     * @param cause causa original
     */
    public RateLimitException(String code, int status, String message, String requestId, Map<String, String> validation,
                              Duration retryAfter, String requiredScope, Throwable cause) {
        super(code, status, message, requestId, validation, retryAfter, requiredScope, cause);
    }
}
