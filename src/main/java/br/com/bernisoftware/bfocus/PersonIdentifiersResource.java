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
     * Todos os identificadores da pessoa ({@code GET /people/{person_external_id}/identifiers}): o principal
     * ({@link PersonIdentifiers#getExternalId()}) e os extras. Aceita no caminho o principal OU qualquer um dos
     * extras. Escopo {@code customers:read}.
     *
     * <p>É a fonte de verdade para RECONCILIAR: {@link PeopleResource#list(String)} mostra só o identificador
     * principal, então um id que virou extra some de lá sem ter sumido do cadastro — e, sem esta leitura, era
     * preciso ESCREVER (tentar um {@link #add(String, String)}) para descobrir o que tinha acontecido.
     *
     * @param personExternalId id da pessoa no seu sistema — o principal ou qualquer um dos extras
     * @return o identificador principal + todos os extras
     * @throws NotFoundException {@code PERSON_NOT_FOUND}
     */
    public PersonIdentifiers list(String personExternalId) {
        return list(personExternalId, null);
    }

    /**
     * Todos os identificadores da pessoa — com opções da chamada.
     *
     * @param personExternalId id da pessoa no seu sistema — o principal ou qualquer um dos extras
     * @param options opções da chamada; pode ser {@code null}
     * @return o identificador principal + todos os extras
     */
    public PersonIdentifiers list(String personExternalId, RequestOptions options) {
        return transport.call("GET", "/people/" + Paths.segment(personExternalId, "personExternalId") + "/identifiers",
                null, null, options, PersonIdentifiers::from);
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
