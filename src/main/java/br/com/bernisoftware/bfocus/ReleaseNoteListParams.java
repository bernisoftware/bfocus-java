package br.com.bernisoftware.bfocus;

/**
 * Filtros de {@link ReleaseNotesResource#list(String, ReleaseNoteListParams)} (imutável).
 */
public final class ReleaseNoteListParams {
    private final Boolean published;
    private final Integer page;
    private final Integer pageSize;

    private ReleaseNoteListParams(Boolean published, Integer page, Integer pageSize) {
        this.published = published;
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

    /** @return {@code true} só publicadas; {@code false} só rascunhos; {@code null} todas */
    public Boolean getPublished() {
        return published;
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
        return new Query().add("published", published).add("page", page).add("page_size", pageSize);
    }

    /** Builder de {@link ReleaseNoteListParams}. */
    public static final class Builder {
        private Boolean published;
        private Integer page;
        private Integer pageSize;

        private Builder() {
        }

        /**
         * {@code true} só publicadas; {@code false} só rascunhos; {@code null} (padrão) todas.
         *
         * @param published o filtro
         * @return este builder
         */
        public Builder published(Boolean published) {
            this.published = published;
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
         * Monta os filtros.
         *
         * @return os filtros
         */
        public ReleaseNoteListParams build() {
            return new ReleaseNoteListParams(published, page, pageSize);
        }
    }
}
