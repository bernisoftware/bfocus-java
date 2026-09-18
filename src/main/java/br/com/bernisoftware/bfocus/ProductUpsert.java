package br.com.bernisoftware.bfocus;

import java.util.Map;

/**
 * Corpo de {@link ProductsResource#upsert(String, ProductUpsert)}. Parcial — veja {@link PatchRequest}: setter
 * não chamado = omitido; setter com {@code null} ou {@link Builder#clear(String...)} = limpa.
 */
public final class ProductUpsert extends PatchRequest {
    static final PatchState.Spec SPEC = new PatchState.Spec("ProductUpsert")
            .field("name", "name")
            .field("description", "description")
            .field("color", "color")
            .field("icon", "icon")
            .field("isActive", "is_active")
            .field("sortOrder", "sort_order");

    private ProductUpsert(Map<String, Object> body) {
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

    /** Builder de {@link ProductUpsert}. */
    public static final class Builder {
        private final PatchState state = new PatchState(SPEC);

        private Builder() {
        }

        /**
         * Nome (1–255; obrigatório ao criar).
         *
         * @param name o nome ({@code null} = limpar)
         * @return este builder
         */
        public Builder name(String name) {
            state.set("name", name);
            return this;
        }

        /**
         * Descrição.
         *
         * @param description a descrição ({@code null} = limpar)
         * @return este builder
         */
        public Builder description(String description) {
            state.set("description", description);
            return this;
        }

        /**
         * Cor (ex.: {@code #6366F1}, até 16).
         *
         * @param color a cor ({@code null} = limpar)
         * @return este builder
         */
        public Builder color(String color) {
            state.set("color", color);
            return this;
        }

        /**
         * Ícone (até 64).
         *
         * @param icon o ícone ({@code null} = limpar)
         * @return este builder
         */
        public Builder icon(String icon) {
            state.set("icon", icon);
            return this;
        }

        /**
         * Ativo ({@code false} arquiva).
         *
         * @param isActive ativo ({@code null} = limpar)
         * @return este builder
         */
        public Builder isActive(Boolean isActive) {
            state.set("is_active", isActive);
            return this;
        }

        /**
         * Ordem de exibição.
         *
         * @param sortOrder a ordem ({@code null} = limpar)
         * @return este builder
         */
        public Builder sortOrder(Integer sortOrder) {
            state.set("sort_order", sortOrder);
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
        public ProductUpsert build() {
            return new ProductUpsert(state.snapshot());
        }
    }
}
