package br.com.bernisoftware.bfocus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Corpo de {@link CustomersResource#upsert(String, CustomerUpsert)} ({@code PUT /customers/{external_id}}).
 * Parcial — veja {@link PatchRequest}: setter não chamado = omitido; setter com {@code null} ou
 * {@link Builder#clear(String...)} = limpa.
 *
 * <pre>{@code
 * CustomerUpsert.builder()
 *         .name("Padaria Estrela")
 *         .email("contato@padaria.example")
 *         .clear("phone")
 *         .build();
 * }</pre>
 */
public final class CustomerUpsert extends PatchRequest {
    static final PatchState.Spec SPEC = new PatchState.Spec("CustomerUpsert")
            .field("name", "name")
            .field("document", "document")
            .field("email", "email")
            .field("phone", "phone")
            .field("website", "website")
            .field("notes", "notes")
            .field("customFields", "custom_fields");

    private CustomerUpsert(Map<String, Object> body) {
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

    /** Builder de {@link CustomerUpsert}. */
    public static final class Builder {
        private final PatchState state = new PatchState(SPEC);

        private Builder() {
        }

        /**
         * Nome (até 500; obrigatório ao criar).
         *
         * @param name o nome ({@code null} = limpar)
         * @return este builder
         */
        public Builder name(String name) {
            state.set("name", name);
            return this;
        }

        /**
         * CPF/CNPJ ou outro documento (até 50).
         *
         * @param document o documento ({@code null} = limpar)
         * @return este builder
         */
        public Builder document(String document) {
            state.set("document", document);
            return this;
        }

        /**
         * E-mail (até 255).
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
         * Site (até 500).
         *
         * @param website o site ({@code null} = limpar)
         * @return este builder
         */
        public Builder website(String website) {
            state.set("website", website);
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
         * Campos personalizados. Quando enviada, a lista SUBSTITUI a atual (lista vazia apaga todos).
         *
         * @param customFields os campos
         * @return este builder
         */
        public Builder customFields(List<CustomFieldInput> customFields) {
            if (customFields == null) {
                state.set("custom_fields", null);
                return this;
            }
            List<Object> maps = new ArrayList<>(customFields.size());
            for (CustomFieldInput field : customFields) {
                maps.add(Objects.requireNonNull(field, "customFields contém null").toMap());
            }
            state.set("custom_fields", Collections.unmodifiableList(maps));
            return this;
        }

        /**
         * Campos personalizados (substituem a lista atual).
         *
         * @param customFields os campos
         * @return este builder
         */
        public Builder customFields(CustomFieldInput... customFields) {
            return customFields(Arrays.asList(Objects.requireNonNull(customFields, "customFields")));
        }

        /**
         * Campos a LIMPAR (enviados como {@code null}), pelo nome Java ({@code "customFields"}) ou JSON
         * ({@code "custom_fields"}).
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
        public CustomerUpsert build() {
            return new CustomerUpsert(state.snapshot());
        }
    }
}
