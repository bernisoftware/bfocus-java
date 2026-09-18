package br.com.bernisoftware.bfocus;

import java.time.Duration;
import java.util.Objects;

/**
 * Opções de UMA chamada (imutável). Todo método da SDK tem uma sobrecarga que aceita {@code RequestOptions}
 * como último argumento ({@code null} = padrão).
 *
 * <pre>{@code
 * client.customers().interactions().create("ERP 1042", "Pedido 1042 faturado.",
 *         RequestOptions.idempotencyKey("pedido-1042-faturado"));
 * }</pre>
 */
public final class RequestOptions {
    private final String idempotencyKey;
    private final Duration timeout;

    private RequestOptions(String idempotencyKey, Duration timeout) {
        this.idempotencyKey = idempotencyKey;
        this.timeout = timeout;
    }

    /**
     * Novo builder.
     *
     * @return o builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Atalho: só a {@code Idempotency-Key}.
     *
     * @param key a chave (1–255 caracteres)
     * @return as opções
     */
    public static RequestOptions idempotencyKey(String key) {
        return builder().idempotencyKey(key).build();
    }

    /**
     * Atalho: só o tempo limite por tentativa.
     *
     * @param timeout tempo limite (positivo)
     * @return as opções
     */
    public static RequestOptions timeout(Duration timeout) {
        return builder().timeout(timeout).build();
    }

    /**
     * {@code Idempotency-Key} própria para escritas (POST/PUT/DELETE). Sem ela, a SDK gera uma por chamada e a
     * repete nas novas tentativas. Ignorada em leituras.
     *
     * @return a chave, ou {@code null}
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * Tempo limite por tentativa só desta chamada (sobrepõe o do cliente).
     *
     * @return o tempo limite, ou {@code null}
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Builder pré-preenchido com estas opções.
     *
     * @return o builder
     */
    public Builder toBuilder() {
        return new Builder().idempotencyKey(idempotencyKey).timeout(timeout);
    }

    RequestOptions withIdempotencyKey(String key) {
        return new RequestOptions(key, timeout);
    }

    /** Builder de {@link RequestOptions}. */
    public static final class Builder {
        private String idempotencyKey;
        private Duration timeout;

        private Builder() {
        }

        /**
         * {@code Idempotency-Key} própria (escritas). Informe a sua para que um reenvio feito pela SUA aplicação
         * (ex.: job reexecutado) também seja deduplicado pela API.
         *
         * @param key a chave; {@code null} = a SDK gera uma
         * @return este builder
         */
        public Builder idempotencyKey(String key) {
            if (key != null && key.isEmpty()) {
                throw new IllegalArgumentException("idempotencyKey não pode ser vazia (use null para a SDK gerar uma).");
            }
            this.idempotencyKey = key;
            return this;
        }

        /**
         * Tempo limite por tentativa.
         *
         * @param timeout positivo; {@code null} = o do cliente
         * @return este builder
         */
        public Builder timeout(Duration timeout) {
            if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
                throw new IllegalArgumentException("timeout precisa ser positivo.");
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * Monta as opções.
         *
         * @return as opções
         */
        public RequestOptions build() {
            return new RequestOptions(idempotencyKey, timeout);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RequestOptions)) {
            return false;
        }
        RequestOptions other = (RequestOptions) o;
        return Objects.equals(idempotencyKey, other.idempotencyKey) && Objects.equals(timeout, other.timeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idempotencyKey, timeout);
    }

    @Override
    public String toString() {
        return "RequestOptions{idempotencyKey=" + idempotencyKey + ", timeout=" + timeout + "}";
    }
}
