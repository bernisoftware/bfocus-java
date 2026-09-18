package br.com.bernisoftware.bfocus;

import java.util.Map;
import java.util.Objects;

/**
 * Item de {@link KbArticlesResource#batchUpsert(java.util.Collection)}: os campos de {@link KbArticleUpsert} com
 * o {@code external_id} do artigo (obrigatório; sem {@code /} — use {@code :} para hierarquia). Parcial como os
 * demais upserts — veja {@link PatchRequest}.
 *
 * <pre>{@code
 * KbBatchArticle.builder("git:guia:instalacao").title("Instalação").bodyMarkdown(md).status("published").build();
 * }</pre>
 */
public final class KbBatchArticle extends PatchRequest {
    static final PatchState.Spec SPEC = new PatchState.Spec("KbBatchArticle")
            .fixed("externalId", "external_id")
            .field("title", "title")
            .field("bodyHtml", "body_html")
            .field("bodyMarkdown", "body_markdown")
            .field("product", "product")
            .field("status", "status");

    private final String externalId;

    private KbBatchArticle(String externalId, Map<String, Object> body) {
        super(body);
        this.externalId = externalId;
    }

    /**
     * Novo builder para o artigo {@code externalId}.
     *
     * @param externalId id do artigo no seu sistema (sem {@code /}; ex.: {@code git:guia:instalacao})
     * @return o builder
     * @throws IllegalArgumentException id vazio ou com {@code /}
     */
    public static Builder builder(String externalId) {
        return new Builder(externalId);
    }

    /** @return o {@code external_id} do artigo */
    public String getExternalId() {
        return externalId;
    }

    /** Builder de {@link KbBatchArticle}. */
    public static final class Builder {
        private final String externalId;
        private final PatchState state = new PatchState(SPEC);

        private Builder(String externalId) {
            Objects.requireNonNull(externalId, "externalId");
            if (externalId.isEmpty()) {
                throw new IllegalArgumentException("O external_id do artigo não pode ser vazio.");
            }
            if (externalId.indexOf('/') >= 0) {
                throw new IllegalArgumentException("O external_id '" + externalId + "' não aceita '/' — use ':' para hierarquia (ex.: 'git:guia:instalacao').");
            }
            this.externalId = externalId;
            state.set("external_id", externalId);
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
         * Corpo em HTML (use este OU {@link #bodyMarkdown(String)}).
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
         * {@code draft} ou {@code published}.
         *
         * @param status o status ({@code null} = limpar)
         * @return este builder
         */
        public Builder status(String status) {
            state.set("status", status);
            return this;
        }

        /**
         * Campos a LIMPAR (enviados como {@code null}), pelo nome Java ou JSON. {@code external_id} não pode ser
         * limpo.
         *
         * @param fields os campos
         * @return este builder
         * @throws IllegalArgumentException nome desconhecido ou campo que não aceita null
         */
        public Builder clear(String... fields) {
            state.clear(fields);
            return this;
        }

        /**
         * Monta o item.
         *
         * @return o item (imutável)
         */
        public KbBatchArticle build() {
            return new KbBatchArticle(externalId, state.snapshot());
        }
    }
}
