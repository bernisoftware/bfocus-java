package br.com.bernisoftware.bfocus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Filtros de {@link KbArticlesResource#list(KbArticleListParams)} e {@link KbArticlesResource#listAll(KbArticleListParams)}
 * (imutável).
 */
public final class KbArticleListParams {
    private final String product;
    private final String status;
    private final String q;
    private final String updatedSince;
    private final Integer page;
    private final Integer pageSize;

    private KbArticleListParams(Builder b) {
        this.product = b.product;
        this.status = b.status;
        this.q = b.q;
        this.updatedSince = b.updatedSince;
        this.page = b.page;
        this.pageSize = b.pageSize;
    }

    /**
     * Novo builder.
     *
     * @return o builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** @return slug do produto, ou {@code null} */
    public String getProduct() {
        return product;
    }

    /** @return {@code draft}/{@code published}, ou {@code null} */
    public String getStatus() {
        return status;
    }

    /** @return busca no título e no texto, ou {@code null} */
    public String getQ() {
        return q;
    }

    /** @return {@code updated_since} como vai na query (ISO 8601 UTC), ou {@code null} */
    public String getUpdatedSince() {
        return updatedSince;
    }

    /** @return página, ou {@code null} */
    public Integer getPage() {
        return page;
    }

    /** @return itens por página, ou {@code null} */
    public Integer getPageSize() {
        return pageSize;
    }

    Query query(Integer page, Integer pageSize) {
        return new Query().add("product", product).add("status", status).add("q", q)
                .add("updated_since", updatedSince).add("page", page).add("page_size", pageSize);
    }

    /** Builder de {@link KbArticleListParams}. */
    public static final class Builder {
        private String product;
        private String status;
        private String q;
        private String updatedSince;
        private Integer page;
        private Integer pageSize;

        private Builder() {
        }

        /**
         * Só os artigos deste produto.
         *
         * @param product o slug
         * @return este builder
         */
        public Builder product(String product) {
            this.product = product;
            return this;
        }

        /**
         * {@code draft} ou {@code published}.
         *
         * @param status o status
         * @return este builder
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Busca no título e no texto.
         *
         * @param q o texto
         * @return este builder
         */
        public Builder q(String q) {
            this.q = q;
            return this;
        }

        /**
         * Só os alterados a partir deste instante.
         *
         * @param updatedSince o instante (enviado em UTC, com {@code Z})
         * @return este builder
         */
        public Builder updatedSince(Instant updatedSince) {
            this.updatedSince = Paths.date(updatedSince);
            return this;
        }

        /**
         * Só os alterados a partir deste instante.
         *
         * @param updatedSince o instante (convertido para UTC)
         * @return este builder
         */
        public Builder updatedSince(OffsetDateTime updatedSince) {
            this.updatedSince = Paths.date(updatedSince);
            return this;
        }

        /**
         * Só os alterados a partir deste instante.
         *
         * @param updatedSince o instante (convertido para UTC)
         * @return este builder
         */
        public Builder updatedSince(ZonedDateTime updatedSince) {
            this.updatedSince = Paths.date(updatedSince);
            return this;
        }

        /**
         * Só os alterados a partir deste instante, em texto ISO 8601 (vai como veio).
         *
         * @param updatedSince ex.: {@code 2026-09-01T00:00:00Z}
         * @return este builder
         */
        public Builder updatedSince(String updatedSince) {
            this.updatedSince = updatedSince;
            return this;
        }

        /**
         * Página (a partir de 1). Não use com {@code listAll}.
         *
         * @param page a página
         * @return este builder
         */
        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        /**
         * Itens por página (1–100; padrão do {@code listAll}: 100).
         *
         * @param pageSize o tamanho
         * @return este builder
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Monta os filtros.
         *
         * @return os filtros
         */
        public KbArticleListParams build() {
            return new KbArticleListParams(this);
        }
    }
}
