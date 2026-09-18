package br.com.bernisoftware.bfocus;

import br.com.bernisoftware.bfocus.internal.Json;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Corpo de {@link CustomerInteractionsResource#create(String, InteractionCreate)} (imutável). Só vai no JSON o
 * que for informado.
 *
 * <pre>{@code
 * InteractionCreate.builder("Pedido 1042 faturado.").isInternal(false).authorEmail("carla@suaempresa.com").build();
 * }</pre>
 */
public final class InteractionCreate {
    private final Map<String, Object> body;

    private InteractionCreate(Map<String, Object> body) {
        this.body = body;
    }

    /**
     * Novo builder.
     *
     * @param content texto ou HTML (1–50000; texto puro vira parágrafos)
     * @return o builder
     */
    public static Builder builder(String content) {
        return new Builder(content);
    }

    Map<String, Object> body() {
        return body;
    }

    /**
     * O corpo JSON que a SDK vai enviar.
     *
     * @return o JSON compacto
     */
    public String toJson() {
        return Json.write(body);
    }

    @Override
    public String toString() {
        return "InteractionCreate" + toJson();
    }

    /** Builder de {@link InteractionCreate}. */
    public static final class Builder {
        private final String content;
        private Boolean internal;
        private String authorEmail;

        private Builder(String content) {
            this.content = Objects.requireNonNull(content, "content");
        }

        /**
         * Nota interna (padrão da API: {@code true}) ou registro visível ao cliente ({@code false}).
         *
         * @param isInternal interna
         * @return este builder
         */
        public Builder isInternal(boolean isInternal) {
            this.internal = isInternal;
            return this;
        }

        /**
         * E-mail de um usuário do bFocus que assina o registro (omitido = "sistema").
         *
         * @param authorEmail o e-mail; {@code null} = não enviar
         * @return este builder
         */
        public Builder authorEmail(String authorEmail) {
            this.authorEmail = authorEmail;
            return this;
        }

        /**
         * Monta o corpo.
         *
         * @return o corpo
         */
        public InteractionCreate build() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("content", content);
            if (internal != null) {
                body.put("is_internal", internal);
            }
            if (authorEmail != null) {
                body.put("author_email", authorEmail);
            }
            return new InteractionCreate(Collections.unmodifiableMap(body));
        }
    }
}
