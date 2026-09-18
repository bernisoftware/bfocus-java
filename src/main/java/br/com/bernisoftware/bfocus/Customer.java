package br.com.bernisoftware.bfocus;

import java.time.OffsetDateTime;
import java.util.List;

/** Cliente (empresa) — {@code CustomerOut}. */
public final class Customer extends ApiObject {
    private final String id;
    private final String externalId;
    private final String name;
    private final String document;
    private final String email;
    private final String phone;
    private final String website;
    private final String notes;
    private final List<CustomField> customFields;
    private final boolean active;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    Customer(Wire w) {
        super(w);
        id = w.string("id");
        externalId = w.string("external_id");
        name = w.string("name");
        document = w.string("document");
        email = w.string("email");
        phone = w.string("phone");
        website = w.string("website");
        notes = w.string("notes");
        customFields = w.list("custom_fields", CustomField::from);
        active = w.bool("is_active", false);
        createdAt = w.date("created_at");
        updatedAt = w.date("updated_at");
    }

    static Customer from(Object json) {
        return new Customer(Wire.of(json, "Customer"));
    }

    /** @return id no bFocus (UUID) */
    public String getId() {
        return id;
    }

    /** @return id do cliente no SEU sistema ({@code external_id}) */
    public String getExternalId() {
        return externalId;
    }

    /** @return nome */
    public String getName() {
        return name;
    }

    /** @return CPF/CNPJ ou outro documento, ou {@code null} */
    public String getDocument() {
        return document;
    }

    /** @return e-mail, ou {@code null} */
    public String getEmail() {
        return email;
    }

    /** @return telefone, ou {@code null} */
    public String getPhone() {
        return phone;
    }

    /** @return site, ou {@code null} */
    public String getWebsite() {
        return website;
    }

    /** @return observações, ou {@code null} */
    public String getNotes() {
        return notes;
    }

    /** @return campos personalizados (lista imutável, nunca {@code null}) */
    public List<CustomField> getCustomFields() {
        return customFields;
    }

    /** @return ativo ({@code is_active}) */
    public boolean isActive() {
        return active;
    }

    /** @return criado em, ou {@code null} */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /** @return alterado em, ou {@code null} */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
