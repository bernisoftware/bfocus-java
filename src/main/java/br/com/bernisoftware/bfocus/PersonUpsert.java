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
 * = omitido; setter com {@code null} ou {@link Builder#clear(String...)} = vai como {@code null}. Para APAGAR o
 * e-mail ou o telefone (em pessoa, {@code null} é "não mexe"), use {@link Builder#erase(String...)}.
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
            .field("document", "document")
            .field("role", "role")
            .field("access", "access")
            .field("isPrimary", "is_primary")
            .field("extraEmails", "extra_emails")
            .field("extraPhones", "extra_phones")
            .field("customFields", "custom_fields")
            .field("erase", "clear");

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
         * CPF da pessoa, com ou sem máscara (a resposta traz só os 11 dígitos).
         *
         * <p>A PESSOA É ÚNICA: o mesmo CPF é sempre o mesmo cadastro, em qualquer produto. Id desconhecido + CPF de
         * uma ficha existente → a resposta vem com {@code merged_into} = id principal dela (o seu id vira identificador
         * extra). Id de uma ficha + CPF de OUTRA → as duas são mescladas na hora ({@code merged_into} = a que tinha o
         * CPF).
         *
         * <p>{@code null}/vazio NÃO apaga (nem via {@link #clear(String...)}; não é campo do {@link #erase(String...)}).
         * Erros: 422 {@code PERSON_DOCUMENT_INVALID} (CPF inválido, {@code ValidationException}) e 409
         * {@code PERSON_DOCUMENT_CONFLICT} (a ficha já tem OUTRO CPF — nunca troca sozinho, {@code ConflictException}).
         *
         * @param document o CPF ({@code null} = enviar {@code null}, que não apaga)
         * @return este builder
         */
        public Builder document(String document) {
            state.set("document", document);
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
         * Campos personalizados da pessoa. Ao contrário de {@link #extraEmails(List)}/{@link #extraPhones(List)},
         * a lista SUBSTITUI a lista inteira: mande o que o seu sistema tem hoje, porque campo que ficar de fora
         * é REMOVIDO (lista vazia apaga todos). Não chamar o setter não mexe em nada. A visibilidade é decidida
         * no bFocus e preservada entre sincronizações.
         *
         * @param customFields os campos ({@code null} = enviar {@code null})
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
         * Campos personalizados da pessoa (substituem a lista inteira).
         *
         * @param customFields os campos
         * @return este builder
         */
        public Builder customFields(CustomFieldInput... customFields) {
            return customFields(Arrays.asList(Objects.requireNonNull(customFields, "customFields")));
        }

        /**
         * Campos a <b>APAGAR</b> nesta pessoa (o campo {@code clear} da API pública): {@code "email"},
         * {@code "phone"} ou os dois.
         *
         * <p><b>Não é o {@link #clear(String...)}</b>, que manda o campo como {@code null} — e em PESSOA
         * {@code null} quer dizer "não mexe". Apagar é EXPLÍCITO de propósito: {@code null}, lista vazia e não
         * chamar o setter continuam significando "não mexe", e a SDK não traduz {@code null} em apagar.
         *
         * <p>Campo fora da lista aceita é RECUSADO pela API (422 {@code PERSON_CLEAR_FIELD_INVALID}), não
         * ignorado. E só se limpa a PRÓPRIA ficha: alcançando a pessoa por um identificador EXTRA, a API recusa
         * (409 {@code PERSON_CLEAR_NOT_OWN_RECORD}) — apagar contato de ficha alcançada por apelido seria apagar
         * dado de outro sistema.
         *
         * @param fields os campos a apagar ({@code null} = enviar {@code null})
         * @return este builder
         */
        public Builder erase(List<String> fields) {
            state.set("clear", strings(fields, "erase"));
            return this;
        }

        /**
         * Campos a APAGAR nesta pessoa (ver {@link #erase(List)}).
         *
         * @param fields os campos a apagar
         * @return este builder
         */
        public Builder erase(String... fields) {
            return erase(Arrays.asList(Objects.requireNonNull(fields, "erase")));
        }

        /**
         * Campos a enviar como {@code null}, pelo nome Java ou JSON.
         *
         * <p>Em PESSOA isto <b>não apaga</b>: {@code null} significa "não mexe" no e-mail e no telefone. Para
         * apagar de verdade, use {@link #erase(String...)}.
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
