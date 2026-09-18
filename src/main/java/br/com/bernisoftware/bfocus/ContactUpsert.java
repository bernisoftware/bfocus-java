package br.com.bernisoftware.bfocus;

import java.util.Map;

/**
 * Corpo de {@link CustomerContactsResource#upsert(String, String, ContactUpsert)}. Parcial — veja
 * {@link PatchRequest}: setter não chamado = omitido; setter com {@code null} ou {@link Builder#clear(String...)}
 * = limpa.
 */
public final class ContactUpsert extends PatchRequest {
    static final PatchState.Spec SPEC = new PatchState.Spec("ContactUpsert")
            .field("name", "name")
            .field("role", "role")
            .field("email", "email")
            .field("phone", "phone")
            .field("notes", "notes")
            .field("isPrimary", "is_primary");

    private ContactUpsert(Map<String, Object> body) {
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

    /** Builder de {@link ContactUpsert}. */
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
         * Cargo/função (até 120).
         *
         * @param role o cargo ({@code null} = limpar)
         * @return este builder
         */
        public Builder role(String role) {
            state.set("role", role);
            return this;
        }

        /**
         * E-mail válido.
         *
         * @param email o e-mail ({@code null} = limpar)
         * @return este builder
         */
        public Builder email(String email) {
            state.set("email", email);
            return this;
        }

        /**
         * Telefone (até 50).
         *
         * @param phone o telefone ({@code null} = limpar)
         * @return este builder
         */
        public Builder phone(String phone) {
            state.set("phone", phone);
            return this;
        }

        /**
         * Observações.
         *
         * @param notes as observações ({@code null} = limpar)
         * @return este builder
         */
        public Builder notes(String notes) {
            state.set("notes", notes);
            return this;
        }

        /**
         * Contato principal do cliente.
         *
         * @param isPrimary {@code true}/{@code false} ({@code null} = limpar)
         * @return este builder
         */
        public Builder isPrimary(Boolean isPrimary) {
            state.set("is_primary", isPrimary);
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
        public ContactUpsert build() {
            return new ContactUpsert(state.snapshot());
        }
    }
}
