package br.com.bernisoftware.bfocus;

import java.time.Duration;
import java.util.Map;

/** 5xx — erro do servidor (informe o {@link #getRequestId()} ao suporte). 502/503/504 são tentados de novo antes. */
public class ServerException extends BfocusException {
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
    public ServerException(String code, int status, String message, String requestId, Map<String, String> validation,
                           Duration retryAfter, String requiredScope, Throwable cause) {
        super(code, status, message, requestId, validation, retryAfter, requiredScope, cause);
    }
}
