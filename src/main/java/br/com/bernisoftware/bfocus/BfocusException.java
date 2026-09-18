package br.com.bernisoftware.bfocus;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erro devolvido pela API do bFocus (qualquer status fora de 2xx), resposta inválida ou falha de rede.
 * Unchecked, como é o normal em SDKs Java.
 *
 * <p><b>Na sua lógica, use {@link #getCode()}</b> — é estável (ex.: {@code CUSTOMER_NOT_FOUND},
 * {@code INTEGRATION_SCOPE_MISSING}); a mensagem é só para leitura humana e pode mudar. Ao falar com o suporte,
 * informe o {@link #getRequestId()}.
 *
 * <p>Subclasses: {@link AuthenticationException} (401), {@link PermissionDeniedException} (403),
 * {@link NotFoundException} (404), {@link ConflictException} (409), {@link ValidationException} (422),
 * {@link RateLimitException} (429), {@link ServerException} (5xx) e {@link NetworkException} (conexão/tempo
 * esgotado). Qualquer outro status — e uma resposta 2xx que não é o envelope JSON da API
 * ({@link #INVALID_RESPONSE}) — vem como esta classe base.
 */
public class BfocusException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Código da resposta 2xx que não é o envelope JSON esperado (proxy, página de erro…). */
    public static final String INVALID_RESPONSE = "INVALID_RESPONSE";

    private final String code;
    private final int status;
    private final String requestId;
    // Sempre Collections.emptyMap() ou unmodifiableMap(LinkedHashMap) — ambos serializáveis; só o tipo
    // declarado (Map) não é, e o -Xlint:serial do JDK 21 acusaria isso.
    @SuppressWarnings("serial")
    private final Map<String, String> validation;
    private final Duration retryAfter;
    private final String requiredScope;

    /**
     * Cria o erro (útil também para simular falhas nos testes da sua aplicação).
     *
     * @param code código estável do erro
     * @param status status HTTP ({@code 0} em erro de rede)
     * @param message texto legível
     * @param requestId id da requisição (informe ao suporte); pode ser {@code null}
     * @param validation campo → motivo, em erros de validação; {@code null} = vazio
     * @param retryAfter espera pedida pela API (só em 429); pode ser {@code null}
     * @param requiredScope escopo que faltou na chave (só em 403 de escopo); pode ser {@code null}
     * @param cause causa original; pode ser {@code null}
     */
    public BfocusException(String code, int status, String message, String requestId, Map<String, String> validation,
                           Duration retryAfter, String requiredScope, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.requestId = requestId;
        this.validation = validation == null || validation.isEmpty()
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(validation));
        this.retryAfter = retryAfter;
        this.requiredScope = requiredScope;
    }

    /**
     * Cria o erro só com código, status e mensagem.
     *
     * @param code código estável do erro
     * @param status status HTTP
     * @param message texto legível
     */
    public BfocusException(String code, int status, String message) {
        this(code, status, message, null, null, null, null, null);
    }

    /**
     * Código estável do erro: {@code error} do corpo, senão {@code message}, senão {@code HTTP_<status>} (corpo
     * não-JSON); {@code NETWORK_ERROR} em falha de rede/tempo esgotado; {@code INVALID_RESPONSE} em resposta 2xx
     * fora do formato da API.
     *
     * @return o código
     */
    public String getCode() {
        return code;
    }

    /**
     * Status HTTP ({@code 0} em erro de rede).
     *
     * @return o status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Id da requisição — informe ao suporte. Vem do {@code request_id} do corpo, senão do header
     * {@code X-Request-Id} da resposta, senão é o {@code X-Request-Id} que a SDK enviou (a API ecoa o do
     * cliente, então bate com o log dela). Preenchido em toda exceção lançada pela SDK, inclusive
     * {@link NetworkException}.
     *
     * @return o id, ou {@code null} num erro criado à mão
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Campo → motivo, em erros de validação (422). Vazio nos demais.
     *
     * @return mapa imutável, nunca {@code null}
     */
    public Map<String, String> getValidation() {
        return validation;
    }

    /**
     * Espera pedida pela API no header {@code Retry-After} — só em 429 (a SDK já esperou e tentou de novo
     * antes de lançar).
     *
     * @return a espera, ou {@code null}
     */
    public Duration getRetryAfter() {
        return retryAfter;
    }

    /**
     * Escopo que faltou na chave, do header {@code X-Required-Scope} (só em 403 de escopo).
     *
     * @return o escopo (ex.: {@code products:read}), ou {@code null}
     */
    public String getRequiredScope() {
        return requiredScope;
    }
}
