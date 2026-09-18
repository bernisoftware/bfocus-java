package br.com.bernisoftware.bfocus;

import java.time.OffsetDateTime;

/** Contato (pessoa) de um cliente — {@code ContactOut}. */
public final class Contact extends ApiObject {
    private final String id;
    private final String externalId;
    private final String name;
    private final String role;
    private final String email;
    private final String phone;
    private final String notes;
    private final boolean primary;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    Contact(Wire w) {
        super(w);
        id = w.string("id");
        externalId = w.string("external_id");
        name = w.string("name");
        role = w.string("role");
        email = w.string("email");
        phone = w.string("phone");
        notes = w.string("notes");
        primary = w.bool("is_primary", false);
        createdAt = w.date("created_at");
        updatedAt = w.date("updated_at");
    }

    static Contact from(Object json) {
        return new Contact(Wire.of(json, "Contact"));
    }

    /** @return id no bFocus (UUID) */
    public String getId() {
        return id;
    }

    /** @return id do contato no seu sistema, ou {@code null} */
    public String getExternalId() {
        return externalId;
    }

    /** @return nome */
    public String getName() {
        return name;
    }

    /** @return cargo/função, ou {@code null} */
    public String getRole() {
        return role;
    }

    /** @return e-mail, ou {@code null} */
    public String getEmail() {
        return email;
    }

    /** @return telefone, ou {@code null} */
    public String getPhone() {
        return phone;
    }

    /** @return observações, ou {@code null} */
    public String getNotes() {
        return notes;
    }

    /** @return contato principal do cliente ({@code is_primary}) */
    public boolean isPrimary() {
        return primary;
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
