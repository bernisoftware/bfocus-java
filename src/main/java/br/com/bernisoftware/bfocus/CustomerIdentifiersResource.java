package br.com.bernisoftware.bfocus;

import java.util.Collections;

/**
 * Identificadores extras dos clientes — {@code client.customers().identifiers()}: liga o id de OUTRO sistema seu
 * (CRM, loja…) ao mesmo cadastro, que passa a ser encontrado por qualquer um deles.
 */
public final class CustomerIdentifiersResource {
    private final Transport transport;

    CustomerIdentifiersResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * Liga {@code extraId} ao cliente ({@code PUT /customers/{external_id}/identifiers/{extra_id}}, sem corpo).
     * Idempotente.
     *
     * @param externalId id principal do cliente no seu sistema
     * @param extraId id do outro sistema
     * @return o cliente com os identificadores
     * @throws ConflictException {@code IDENTIFIER_IN_USE} — o id já é de outro cadastro
     */
    public CustomerWithIdentifiers add(String externalId, String extraId) {
        return add(externalId, extraId, null, null);
    }

    /**
     * Liga {@code extraId} ao cliente, com rótulo (corpo {@code {"label": …}}).
     *
     * @param externalId id principal do cliente no seu sistema
     * @param extraId id do outro sistema
     * @param label rótulo livre (ex.: nome do sistema); {@code null} = sem rótulo e sem corpo
     * @return o cliente com os identificadores
     */
    public CustomerWithIdentifiers add(String externalId, String extraId, String label) {
        return add(externalId, extraId, label, null);
    }

    /**
     * Liga {@code extraId} ao cliente — com opções da chamada.
     *
     * @param externalId id principal do cliente no seu sistema
     * @param extraId id do outro sistema
     * @param label rótulo livre; {@code null} = sem rótulo e sem corpo
     * @param options opções da chamada; pode ser {@code null}
     * @return o cliente com os identificadores
     */
    public CustomerWithIdentifiers add(String externalId, String extraId, String label, RequestOptions options) {
        Object body = label == null ? null : Collections.singletonMap("label", label);
        return transport.call("PUT", path(externalId, extraId), null, body, options, CustomerWithIdentifiers::from);
    }

    /**
     * Desliga {@code extraId} do cliente ({@code DELETE /customers/{external_id}/identifiers/{extra_id}}).
     *
     * @param externalId id principal do cliente no seu sistema
     * @param extraId id do outro sistema
     * @return o cliente com os identificadores que sobraram
     * @throws NotFoundException {@code IDENTIFIER_NOT_FOUND}
     */
    public CustomerWithIdentifiers remove(String externalId, String extraId) {
        return remove(externalId, extraId, null);
    }

    /**
     * Desliga {@code extraId} do cliente — com opções da chamada.
     *
     * @param externalId id principal do cliente no seu sistema
     * @param extraId id do outro sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return o cliente com os identificadores que sobraram
     */
    public CustomerWithIdentifiers remove(String externalId, String extraId, RequestOptions options) {
        return transport.call("DELETE", path(externalId, extraId), null, null, options, CustomerWithIdentifiers::from);
    }

    private static String path(String externalId, String extraId) {
        return CustomersResource.path(externalId) + "/identifiers/" + Paths.segment(extraId, "extraId");
    }
}
