package br.com.bernisoftware.bfocus;

/**
 * Falha de conexão, tempo esgotado ou chamada interrompida ({@code getStatus() == 0},
 * {@code getCode() == "NETWORK_ERROR"}). O {@link #getRequestId()} é o {@code X-Request-Id} que a SDK enviou.
 */
public class NetworkException extends BfocusException {
    private static final long serialVersionUID = 1L;

    /** Código de todo erro de rede. */
    public static final String NETWORK_ERROR = "NETWORK_ERROR";

    /**
     * Cria o erro de rede.
     *
     * @param message texto legível
     * @param requestId o {@code X-Request-Id} enviado
     * @param cause causa original ({@code ConnectException}, {@code HttpTimeoutException}…)
     */
    public NetworkException(String message, String requestId, Throwable cause) {
        super(NETWORK_ERROR, 0, message, requestId, null, null, null, cause);
    }
}
