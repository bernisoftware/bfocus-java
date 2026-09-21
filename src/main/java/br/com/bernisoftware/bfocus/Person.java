package br.com.bernisoftware.bfocus;

import java.util.List;

/**
 * Pessoa de um cliente — {@code PersonOut}: quem usa o sistema do cliente e abre chamados/conversas. O
 * {@code external_id} dela é o mesmo {@code user.externalId} do widget. Não é {@code final} só para
 * {@link PersonUpsertResult}; não dá para estender fora da SDK.
 */
public class Person extends ApiObject {
    private final String externalId;
    private final String name;
    private final String email;
    private final String phone;
    private final String document;
    private final String role;
    private final boolean access;
    private final boolean primary;
    private final String customerExternalId;
    private final List<CustomField> customFields;
    private final List<Identifier> identifiers;

    Person(Wire w) {
        super(w);
        externalId = w.string("external_id");
        name = w.string("name");
        email = w.string("email");
        phone = w.string("phone");
        document = w.string("document");
        role = w.string("role");
        access = w.bool("access", false);
        primary = w.bool("is_primary", false);
        customerExternalId = w.string("customer_external_id");
        customFields = w.list("custom_fields", CustomField::from);
        identifiers = w.list("identifiers", Identifier::from);
    }

    static Person from(Object json) {
        return new Person(Wire.of(json, "Person"));
    }

    /** @return id da pessoa no seu sistema, ou {@code null} (contato do cliente sem acesso, sem identificador) */
    public String getExternalId() {
        return externalId;
    }

    /** @return nome */
    public String getName() {
        return name;
    }

    /** @return e-mail, ou {@code null} */
    public String getEmail() {
        return email;
    }

    /** @return telefone, ou {@code null} */
    public String getPhone() {
        return phone;
    }

    /** @return CPF da pessoa, só os 11 dígitos, ou {@code null} (não informado) */
    public String getDocument() {
        return document;
    }

    /** @return cargo/função, ou {@code null} */
    public String getRole() {
        return role;
    }

    /** @return pode abrir o widget/portal do cliente ({@code access}) */
    public boolean hasAccess() {
        return access;
    }

    /** @return contato principal do cliente ({@code is_primary}) */
    public boolean isPrimary() {
        return primary;
    }

    /** @return {@code external_id} principal do cliente a que a pessoa pertence */
    public String getCustomerExternalId() {
        return customerExternalId;
    }

    /**
     * Campos personalizados da pessoa. A {@code visibility} de cada um é definida no bFocus.
     *
     * @return a lista (imutável, possivelmente vazia)
     */
    public List<CustomField> getCustomFields() {
        return customFields;
    }

    /** @return identificadores EXTRAS desta pessoa (o principal é {@link #getExternalId()}); nunca {@code null} */
    public List<Identifier> getIdentifiers() {
        return identifiers;
    }
}
