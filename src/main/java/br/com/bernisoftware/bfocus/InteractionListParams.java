package br.com.bernisoftware.bfocus;

/**
 * Paginação de {@link CustomerInteractionsResource#list(String, InteractionListParams)} (imutável).
 */
public final class InteractionListParams {
    private final Integer page;
    private final Integer pageSize;

    private InteractionListParams(Integer page, Integer pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * Novo builder.
     *
     * @return o builder
     */
    public static Builder builder() {
        return new Builder();
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
        return new Query().add("page", page).add("page_size", pageSize);
    }

    /** Builder de {@link InteractionListParams}. */
    public static final class Builder {
        private Integer page;
        private Integer pageSize;

        private Builder() {
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
         * Itens por página (1–200; padrão do {@code listAll}: 100).
         *
         * @param pageSize o tamanho
         * @return este builder
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Monta os parâmetros.
         *
         * @return os parâmetros
         */
        public InteractionListParams build() {
            return new InteractionListParams(page, pageSize);
        }
    }
}
