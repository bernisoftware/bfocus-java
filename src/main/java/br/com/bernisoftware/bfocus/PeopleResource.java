package br.com.bernisoftware.bfocus;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pessoas dos clientes — {@code client.people()} (desde 0.2.0). Pessoa = quem usa o sistema do cliente e abre
 * chamados/conversas; o {@code external_id} dela é o mesmo {@code user.externalId} do widget (por isso não pode ter
 * {@code :} se for assinado). Escopos: {@code customers:read} / {@code customers:write}. Identificadores extras
 * ficam em {@link #identifiers()}.
 */
public final class PeopleResource {
    private final Transport transport;
    private final PersonIdentifiersResource identifiers;

    PeopleResource(Transport transport) {
        this.transport = transport;
        this.identifiers = new PersonIdentifiersResource(transport);
    }

    /** @return identificadores extras das pessoas */
    public PersonIdentifiersResource identifiers() {
        return identifiers;
    }

    /**
     * Cria ou atualiza a pessoa ({@code PUT /customers/{external_id}/people/{person_external_id}}, corpo
     * {@code {"person": {…}}}). Só os campos informados mudam. O e-mail (ou telefone) acha a pessoa que já chegou
     * por e-mail ou por outro sistema — ela é adotada, nunca duplicada; a mesma pessoa em outro cliente é
     * LIGADA a ele também (cadastro único em N clientes).
     *
     * <p>{@link PersonUpsert.Builder#customFields(java.util.List)} é a exceção ao "só o que vier muda": a lista
     * enviada SUBSTITUI a lista inteira de campos personalizados da pessoa — campo que ficar de fora é removido.
     *
     * <p>{@link PersonUpsert.Builder#erase(String...)} APAGA contato ({@code "email"}, {@code "phone"} ou os dois).
     * Não confunda com {@link PersonUpsert.Builder#clear(String...)}, que manda {@code null} — e em pessoa
     * {@code null} quer dizer "não mexe".
     *
     * @param customerExternalId id do cliente (empresa) no seu sistema
     * @param personExternalId id da pessoa no seu sistema
     * @param person campos a gravar
     * @return a pessoa gravada, com {@code status} ({@code created}/{@code updated}/{@code unchanged})
     * @throws ConflictException ex.: {@code PERSON_EMAIL_STAFF} (o e-mail é de alguém da sua equipe),
     *     {@code PERSON_EMAIL_TAKEN}/{@code PERSON_PHONE_TAKEN} (o {@code getData()} diz de quem é o contato) ou
     *     {@code PERSON_CONTACT_OTHER_CUSTOMER} (recusa definitiva: a pessoa é de outro cliente); ou
     *     {@code PERSON_CLEAR_NOT_OWN_RECORD}, quando um {@code erase} chega por um identificador extra
     * @throws ValidationException {@code PERSON_CLEAR_FIELD_INVALID} — campo fora da lista aceita em {@code erase}
     */
    public PersonUpsertResult upsert(String customerExternalId, String personExternalId, PersonUpsert person) {
        return upsert(customerExternalId, personExternalId, person, null);
    }

    /**
     * Cria ou atualiza a pessoa — com opções da chamada.
     *
     * @param customerExternalId id do cliente (empresa) no seu sistema
     * @param personExternalId id da pessoa no seu sistema
     * @param person campos a gravar
     * @param options opções da chamada (ex.: {@code idempotencyKey}); pode ser {@code null}
     * @return a pessoa gravada, com {@code status}
     */
    public PersonUpsertResult upsert(String customerExternalId, String personExternalId, PersonUpsert person,
            RequestOptions options) {
        Objects.requireNonNull(person, "person");
        Map<String, Object> body = Collections.singletonMap("person", person.body());
        return transport.call("PUT", path(customerExternalId, personExternalId), null, body, options, PersonUpsertResult::from);
    }

    /**
     * Lista as pessoas do cliente ({@code GET /customers/{external_id}/people}).
     *
     * @param customerExternalId id do cliente no seu sistema
     * @return as pessoas (lista imutável)
     */
    public List<Person> list(String customerExternalId) {
        return list(customerExternalId, null);
    }

    /**
     * Lista as pessoas do cliente — com opções da chamada.
     *
     * @param customerExternalId id do cliente no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return as pessoas
     */
    public List<Person> list(String customerExternalId, RequestOptions options) {
        return transport.call("GET", CustomersResource.path(customerExternalId) + "/people", null, null, options,
                json -> Wire.list(json, Person::from, "data"));
    }

    /**
     * Retira o acesso da pessoa ({@code DELETE /customers/{external_id}/people/{person_external_id}}). A pessoa
     * continua no histórico (chamados, conversas); um {@code upsert} com {@code access(true)} devolve o acesso.
     *
     * @param customerExternalId id do cliente no seu sistema
     * @param personExternalId id da pessoa no seu sistema
     * @return a pessoa, com {@code access = false} (e {@code isUnlinked()} quando ela segue ativa em outros clientes)
     * @throws NotFoundException {@code PERSON_NOT_FOUND}
     */
    public PersonRevokeResult delete(String customerExternalId, String personExternalId) {
        return delete(customerExternalId, personExternalId, null);
    }

    /**
     * Retira o acesso da pessoa — com opções da chamada.
     *
     * @param customerExternalId id do cliente no seu sistema
     * @param personExternalId id da pessoa no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return a pessoa, com {@code access = false} (e {@code isUnlinked()} quando ela segue ativa em outros clientes)
     */
    public PersonRevokeResult delete(String customerExternalId, String personExternalId, RequestOptions options) {
        return transport.call("DELETE", path(customerExternalId, personExternalId), null, null, options,
                PersonRevokeResult::from);
    }

    /**
     * Cria ou atualiza até {@link BfocusClient#BATCH_MAX} (500) pessoas numa requisição ({@code POST /people/batch}).
     * A SDK NÃO divide: acima de 500 itens lança {@link IllegalArgumentException} antes de qualquer requisição (o
     * {@code index} de cada resultado é a posição no lote enviado). Lote vazio devolve o resultado zerado sem ir à
     * API. Um item com erro não desfaz os outros.
     *
     * @param items as pessoas (até 500)
     * @return um resultado por item + {@code summary}
     * @throws IllegalArgumentException mais de 500 itens ou item {@code null}
     */
    public BatchResult batch(Collection<PersonBatchItem> items) {
        return batch(items, null);
    }

    /**
     * Lote de pessoas — com opções da chamada (um lote = uma chamada lógica, com uma {@code idempotencyKey}).
     *
     * @param items as pessoas (até 500)
     * @param options opções da chamada; pode ser {@code null}
     * @return um resultado por item + {@code summary}
     */
    public BatchResult batch(Collection<PersonBatchItem> items, RequestOptions options) {
        Map<String, Object> body = Batches.body("people().batch", items);
        if (body == null) {
            return BatchResult.empty();
        }
        return transport.call("POST", "/people/batch", null, body, options, BatchResult::from);
    }

    private static String path(String customerExternalId, String personExternalId) {
        return CustomersResource.path(customerExternalId) + "/people/" + Paths.segment(personExternalId, "personExternalId");
    }
}
