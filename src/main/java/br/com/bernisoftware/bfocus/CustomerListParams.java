package br.com.bernisoftware.bfocus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Filtros de {@link CustomersResource#list(CustomerListParams)} e {@link CustomersResource#listAll(CustomerListParams)}
 * (imutável). Parâmetro não informado não vai na query.
 *
 * <pre>{@code
 * CustomerListParams.builder().q("padaria").updatedSince(ultimaSincronizacao).pageSize(100).build();
 * }</pre>
 */
public final class CustomerListParams {
    private final String q;
    private final String updatedSince;
    private final Integer page;
    private final Integer pageSize;

    private CustomerListParams(Builder b) {
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

    /** @return busca por nome, documento, e-mail…, ou {@code null} */
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
        return new Query().add("q", q).add("updated_since", updatedSince).add("page", page).add("page_size", pageSize);
    }

    /** Builder de {@link CustomerListParams}. */
    public static final class Builder {
        private String q;
        private String updatedSince;
        private Integer page;
        private Integer pageSize;

        private Builder() {
        }

        /**
         * Busca por nome, documento, e-mail…
         *
         * @param q o texto
         * @return este builder
         */
        public Builder q(String q) {
            this.q = q;
            return this;
        }

        /**
         * Só os alterados a partir deste instante — ideal para sincronização incremental.
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
         * Itens por página (1–200; padrão da API: 50; padrão do {@code listAll}: 100).
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
        public CustomerListParams build() {
            return new CustomerListParams(this);
        }
    }
}
