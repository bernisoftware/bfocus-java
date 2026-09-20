package br.com.bernisoftware.bfocus;

import java.time.Duration;
import java.util.Map;

/**
 * 403 — chave desligada, IP não liberado, escopo faltando ({@code INTEGRATION_SCOPE_MISSING}, veja
 * {@link #getRequiredScope()}) ou módulo não contratado ({@code MODULE_NOT_CONTRACTED}: base de conhecimento e
 * agentes de IA exigem o módulo de Atendimento).
 */
public class PermissionDeniedException extends BfocusException {
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
    public PermissionDeniedException(String code, int status, String message, String requestId, Map<String, String> validation,
                                     Duration retryAfter, String requiredScope, Throwable cause) {
        super(code, status, message, requestId, validation, retryAfter, requiredScope, cause);
    }

    /**
     * Cria o erro, com o {@code data} do corpo.
     *
     * @param code código estável
     * @param status status HTTP
     * @param message texto legível
     * @param requestId id da requisição
     * @param validation campo → motivo
     * @param data o {@code data} do corpo do erro
     * @param retryAfter espera pedida pela API
     * @param requiredScope escopo exigido
     * @param cause causa original
     */
    public PermissionDeniedException(String code, int status, String message, String requestId, Map<String, String> validation,
                                     Map<String, Object> data, Duration retryAfter, String requiredScope, Throwable cause) {
        super(code, status, message, requestId, validation, data, retryAfter, requiredScope, cause);
    }
}
