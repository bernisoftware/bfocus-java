package br.com.bernisoftware.bfocus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cliente (empresa) — {@code CustomerOut}. Não é {@code final} só para {@link CustomerWithIdentifiers} (a mesma
 * resposta com os identificadores extras); não dá para estender fora da SDK.
 */
public class Customer extends ApiObject {
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
    private final String kind;
    private final String legalName;
    private final String stateRegistration;
    private final String municipalRegistration;
    private final String idDocument;
    private final String logoUrl;
    private final List<String> extraEmails;
    private final List<String> extraPhones;
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
        kind = w.string("kind");
        legalName = w.string("legal_name");
        stateRegistration = w.string("state_registration");
        municipalRegistration = w.string("municipal_registration");
        idDocument = w.string("id_document");
        logoUrl = w.string("logo_url");
        extraEmails = w.list("extra_emails", o -> o == null ? null : o.toString());
        extraPhones = w.list("extra_phones", o -> o == null ? null : o.toString());
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

    /**
     * @return tipo do CONTRATANTE: {@code pj} (empresa) ou {@code pf} (pessoa física); {@code null} quando não dá
     *         para saber. Cliente é a CONTA, não a pessoa: uma conta PF pode ter várias pessoas dentro
     */
    public String getKind() {
        return kind;
    }

    /** @return só PJ: razão social, quando difere do nome fantasia ({@link #getName()}); senão {@code null} */
    public String getLegalName() {
        return legalName;
    }

    /** @return só PJ: inscrição estadual (aceita {@code ISENTO}), ou {@code null} */
    public String getStateRegistration() {
        return stateRegistration;
    }

    /** @return só PJ: inscrição municipal, ou {@code null} */
    public String getMunicipalRegistration() {
        return municipalRegistration;
    }

    /** @return só PF: RG e órgão emissor (texto livre — varia por estado), ou {@code null} */
    public String getIdDocument() {
        return idDocument;
    }

    /** @return logotipo do cliente como a equipe subiu no bFocus, ou {@code null} */
    public String getLogoUrl() {
        return logoUrl;
    }

    /** @return e-mails adicionais (o principal é {@link #getEmail()}); lista imutável, nunca {@code null} */
    public List<String> getExtraEmails() {
        return extraEmails;
    }

    /** @return telefones adicionais (o principal é {@link #getPhone()}); lista imutável, nunca {@code null} */
    public List<String> getExtraPhones() {
        return extraPhones;
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
