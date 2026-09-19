package br.com.bernisoftware.bfocus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Campos de uma pessoa, para {@link PeopleResource#upsert(String, String, PersonUpsert)} (a SDK envia
 * {@code {"person": {…}}}) e para {@link PersonBatchItem}. Parcial — veja {@link PatchRequest}: setter não chamado
 * = omitido; setter com {@code null} ou {@link Builder#clear(String...)} = vai como {@code null}.
 *
 * <pre>{@code
 * PersonUpsert.builder()
 *         .name("Paula Reis")
 *         .email("paula@padaria.example")
 *         .role("Financeiro")
 *         .isPrimary(true)
 *         .build();
 * }</pre>
 */
public final class PersonUpsert extends PatchRequest {
    static final PatchState.Spec SPEC = new PatchState.Spec("PersonUpsert")
            .field("name", "name")
            .field("email", "email")
            .field("phone", "phone")
            .field("role", "role")
            .field("access", "access")
            .field("isPrimary", "is_primary")
            .field("extraEmails", "extra_emails")
            .field("extraPhones", "extra_phones");

    private PersonUpsert(Map<String, Object> body) {
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

    /** Builder de {@link PersonUpsert}. */
    public static final class Builder {
        private final PatchState state = new PatchState(SPEC);

        private Builder() {
        }

        /**
         * Nome (obrigatório ao criar).
         *
         * @param name o nome ({@code null} = enviar {@code null})
         * @return este builder
         */
        public Builder name(String name) {
            state.set("name", name);
            return this;
        }

        /**
         * E-mail. Também identifica a pessoa que já existe (chegou por e-mail ou por outro sistema): ela é
         * adotada, nunca duplicada.
         *
         * @param email o e-mail ({@code null} = enviar {@code null})
         * @return este builder
         */
        public Builder email(String email) {
            state.set("email", email);
            return this;
        }

        /**
         * Telefone. Como o e-mail, acha a pessoa que já existe.
         *
         * @param phone o telefone ({@code null} = enviar {@code null})
         * @return este builder
         */
        public Builder phone(String phone) {
            state.set("phone", phone);
            return this;
        }

        /**
         * Cargo/função.
         *
         * @param role o cargo ({@code null} = enviar {@code null})
         * @return este builder
         */
        public Builder role(String role) {
            state.set("role", role);
            return this;
        }

        /**
         * Acesso ao widget/portal do cliente (padrão ao criar: {@code true}). {@code true} devolve o acesso retirado
         * por {@link PeopleResource#delete(String, String)}.
         *
         * @param access {@code true}/{@code false} ({@code null} = enviar {@code null})
         * @return este builder
         */
        public Builder access(Boolean access) {
            state.set("access", access);
            return this;
        }

        /**
         * Contato principal do cliente.
         *
         * @param isPrimary {@code true}/{@code false} ({@code null} = enviar {@code null})
         * @return este builder
         */
        public Builder isPrimary(Boolean isPrimary) {
            state.set("is_primary", isPrimary);
            return this;
        }

        /**
         * E-mails adicionais (somam aos que a pessoa já tem).
         *
         * @param extraEmails os e-mails ({@code null} = enviar {@code null})
         * @return este builder
         */
        public Builder extraEmails(List<String> extraEmails) {
            state.set("extra_emails", strings(extraEmails, "extraEmails"));
            return this;
        }

        /**
         * E-mails adicionais (somam aos que a pessoa já tem).
         *
         * @param extraEmails os e-mails
         * @return este builder
         */
        public Builder extraEmails(String... extraEmails) {
            return extraEmails(Arrays.asList(Objects.requireNonNull(extraEmails, "extraEmails")));
        }

        /**
         * Telefones adicionais (somam aos que a pessoa já tem).
         *
         * @param extraPhones os telefones ({@code null} = enviar {@code null})
         * @return este builder
         */
        public Builder extraPhones(List<String> extraPhones) {
            state.set("extra_phones", strings(extraPhones, "extraPhones"));
            return this;
        }

        /**
         * Telefones adicionais (somam aos que a pessoa já tem).
         *
         * @param extraPhones os telefones
         * @return este builder
         */
        public Builder extraPhones(String... extraPhones) {
            return extraPhones(Arrays.asList(Objects.requireNonNull(extraPhones, "extraPhones")));
        }

        /**
         * Campos a enviar como {@code null}, pelo nome Java ou JSON.
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
         * Monta os campos.
         *
         * @return os campos (imutáveis)
         */
        public PersonUpsert build() {
            return new PersonUpsert(state.snapshot());
        }

        private static List<String> strings(List<String> values, String name) {
            if (values == null) {
                return null;
            }
            List<String> copy = new ArrayList<>(values.size());
            for (String value : values) {
                copy.add(Objects.requireNonNull(value, name + " contém null"));
            }
            return Collections.unmodifiableList(copy);
        }
    }
}
