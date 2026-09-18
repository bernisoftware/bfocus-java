package br.com.bernisoftware.bfocus;

import java.util.Map;

/**
 * Corpo de {@link KbArticlesResource#upsert(String, KbArticleUpsert)}. Parcial — veja {@link PatchRequest}:
 * setter não chamado = omitido; setter com {@code null} ou {@link Builder#clear(String...)} = limpa. Em especial,
 * {@code product(null)} (ou {@code clear("product")}) torna o artigo GLOBAL (vale para todos os produtos).
 */
public final class KbArticleUpsert extends PatchRequest {
    static final PatchState.Spec SPEC = new PatchState.Spec("KbArticleUpsert")
            .field("title", "title")
            .field("bodyHtml", "body_html")
            .field("bodyMarkdown", "body_markdown")
            .field("product", "product")
            .field("status", "status");

    private KbArticleUpsert(Map<String, Object> body) {
        super(body);
    }

    /**
     * Novo builder.
     *
     * @return o builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder de {@link KbArticleUpsert}. */
    public static final class Builder {
        private final PatchState state = new PatchState(SPEC);

        private Builder() {
        }

        /**
         * Título (1–200; obrigatório ao criar).
         *
         * @param title o título ({@code null} = limpar)
         * @return este builder
         */
        public Builder title(String title) {
            state.set("title", title);
            return this;
        }

        /**
         * Corpo em HTML (sanitizado; use este OU {@link #bodyMarkdown(String)}).
         *
         * @param bodyHtml o HTML ({@code null} = limpar)
         * @return este builder
         */
        public Builder bodyHtml(String bodyHtml) {
            state.set("body_html", bodyHtml);
            return this;
        }

        /**
         * Corpo em Markdown (a API converte para HTML).
         *
         * @param bodyMarkdown o Markdown ({@code null} = limpar)
         * @return este builder
         */
        public Builder bodyMarkdown(String bodyMarkdown) {
            state.set("body_markdown", bodyMarkdown);
            return this;
        }

        /**
         * Slug do produto. {@code null} = artigo global (vale para todos os produtos).
         *
         * @param product o slug, ou {@code null} para tornar o artigo global
         * @return este builder
         */
        public Builder product(String product) {
            state.set("product", product);
            return this;
        }

        /**
         * {@code draft} ou {@code published} (omitido = mantém; rascunho ao criar). Só publicado alimenta o agente
         * de IA.
         *
         * @param status o status ({@code null} = limpar)
         * @return este builder
         */
        public Builder status(String status) {
            state.set("status", status);
            return this;
        }

        /**
         * Campos a LIMPAR (enviados como {@code null}), pelo nome Java ou JSON.
         *
         * @param fields os campos
         * @return este builder
         * @throws IllegalArgumentException nome desconhecido
         */
        public Builder clear(String... fields) {
            state.clear(fields);
            return this;
        }

        /**
         * Monta o corpo.
         *
         * @return o corpo (imutável)
         */
        public KbArticleUpsert build() {
            return new KbArticleUpsert(state.snapshot());
        }
    }
}
