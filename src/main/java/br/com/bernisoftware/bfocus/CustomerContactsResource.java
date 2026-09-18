package br.com.bernisoftware.bfocus;

import java.util.List;
import java.util.Objects;

/** Contatos de um cliente — {@code client.customers().contacts()}. */
public final class CustomerContactsResource {
    private final Transport transport;

    CustomerContactsResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * Lista os contatos do cliente ({@code GET /customers/{external_id}/contacts}).
     *
     * @param externalId id do cliente no seu sistema
     * @return os contatos (lista imutável)
     */
    public List<Contact> list(String externalId) {
        return list(externalId, null);
    }

    /**
     * Lista os contatos do cliente — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return os contatos
     */
    public List<Contact> list(String externalId, RequestOptions options) {
        return transport.call("GET", CustomersResource.path(externalId) + "/contacts", null, null, options,
                json -> Wire.list(json, Contact::from, "data"));
    }

    /**
     * Cria ou atualiza o contato ({@code PUT /customers/{external_id}/contacts/{contact_external_id}}).
     *
     * @param externalId id do cliente no seu sistema
     * @param contactExternalId id do contato no seu sistema
     * @param contact campos a gravar
     * @return o contato gravado
     * @throws ValidationException {@code VALIDATION_ERROR} (ex.: e-mail inválido)
     */
    public Contact upsert(String externalId, String contactExternalId, ContactUpsert contact) {
        return upsert(externalId, contactExternalId, contact, null);
    }

    /**
     * Cria ou atualiza o contato — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param contactExternalId id do contato no seu sistema
     * @param contact campos a gravar
     * @param options opções da chamada; pode ser {@code null}
     * @return o contato gravado
     */
    public Contact upsert(String externalId, String contactExternalId, ContactUpsert contact, RequestOptions options) {
        Objects.requireNonNull(contact, "contact");
        return transport.call("PUT", path(externalId, contactExternalId), null, contact.body(), options, Contact::from);
    }

    /**
     * Exclui o contato ({@code DELETE /customers/{external_id}/contacts/{contact_external_id}}).
     *
     * @param externalId id do cliente no seu sistema
     * @param contactExternalId id do contato no seu sistema
     * @return {@code {deleted: true}}
     */
    public DeleteResult delete(String externalId, String contactExternalId) {
        return delete(externalId, contactExternalId, null);
    }

    /**
     * Exclui o contato — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param contactExternalId id do contato no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return {@code {deleted: true}}
     */
    public DeleteResult delete(String externalId, String contactExternalId, RequestOptions options) {
        return transport.call("DELETE", path(externalId, contactExternalId), null, null, options, DeleteResult::from);
    }

    private static String path(String externalId, String contactExternalId) {
        return CustomersResource.path(externalId) + "/contacts/" + Paths.segment(contactExternalId, "contactExternalId");
    }
}
