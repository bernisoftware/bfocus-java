package br.com.bernisoftware.bfocus;

import java.util.Collections;

/**
 * Identificadores extras das pessoas — {@code client.people().identifiers()}: liga o id de OUTRO sistema seu à
 * mesma pessoa.
 */
public final class PersonIdentifiersResource {
    private final Transport transport;

    PersonIdentifiersResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * Liga {@code extraId} à pessoa ({@code PUT /people/{person_external_id}/identifiers/{extra_id}}, sem corpo).
     * Idempotente.
     *
     * @param personExternalId id principal da pessoa no seu sistema
     * @param extraId id do outro sistema
     * @return os identificadores da pessoa
     * @throws ConflictException {@code IDENTIFIER_IN_USE} — o id já é de outro cadastro
     */
    public PersonIdentifiers add(String personExternalId, String extraId) {
        return add(personExternalId, extraId, null, null);
    }

    /**
     * Liga {@code extraId} à pessoa, com rótulo (corpo {@code {"label": …}}).
     *
     * @param personExternalId id principal da pessoa no seu sistema
     * @param extraId id do outro sistema
     * @param label rótulo livre (ex.: nome do sistema); {@code null} = sem rótulo e sem corpo
     * @return os identificadores da pessoa
     */
    public PersonIdentifiers add(String personExternalId, String extraId, String label) {
        return add(personExternalId, extraId, label, null);
    }

    /**
     * Liga {@code extraId} à pessoa — com opções da chamada.
     *
     * @param personExternalId id principal da pessoa no seu sistema
     * @param extraId id do outro sistema
     * @param label rótulo livre; {@code null} = sem rótulo e sem corpo
     * @param options opções da chamada; pode ser {@code null}
     * @return os identificadores da pessoa
     */
    public PersonIdentifiers add(String personExternalId, String extraId, String label, RequestOptions options) {
        Object body = label == null ? null : Collections.singletonMap("label", label);
        return transport.call("PUT", path(personExternalId, extraId), null, body, options, PersonIdentifiers::from);
    }

    /**
     * Desliga {@code extraId} da pessoa ({@code DELETE /people/{person_external_id}/identifiers/{extra_id}}).
     *
     * @param personExternalId id principal da pessoa no seu sistema
     * @param extraId id do outro sistema
     * @return os identificadores que sobraram
     * @throws NotFoundException {@code IDENTIFIER_NOT_FOUND}
     */
    public PersonIdentifiers remove(String personExternalId, String extraId) {
        return remove(personExternalId, extraId, null);
    }

    /**
     * Desliga {@code extraId} da pessoa — com opções da chamada.
     *
     * @param personExternalId id principal da pessoa no seu sistema
     * @param extraId id do outro sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return os identificadores que sobraram
     */
    public PersonIdentifiers remove(String personExternalId, String extraId, RequestOptions options) {
        return transport.call("DELETE", path(personExternalId, extraId), null, null, options, PersonIdentifiers::from);
    }

    private static String path(String personExternalId, String extraId) {
        return "/people/" + Paths.segment(personExternalId, "personExternalId") + "/identifiers/"
                + Paths.segment(extraId, "extraId");
    }
}
