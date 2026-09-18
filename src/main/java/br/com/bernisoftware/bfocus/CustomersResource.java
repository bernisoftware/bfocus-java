package br.com.bernisoftware.bfocus;

import java.util.Objects;

/**
 * Clientes — {@code client.customers()}. Escopos: {@code customers:read} / {@code customers:write}.
 * Contatos, produtos vinculados e interações ficam em {@link #contacts()}, {@link #products()} e
 * {@link #interactions()}.
 */
public final class CustomersResource {
    private final Transport transport;
    private final CustomerContactsResource contacts;
    private final CustomerProductsResource products;
    private final CustomerInteractionsResource interactions;

    CustomersResource(Transport transport) {
        this.transport = transport;
        this.contacts = new CustomerContactsResource(transport);
        this.products = new CustomerProductsResource(transport);
        this.interactions = new CustomerInteractionsResource(transport);
    }

    /** @return contatos dos clientes */
    public CustomerContactsResource contacts() {
        return contacts;
    }

    /** @return produtos vinculados aos clientes */
    public CustomerProductsResource products() {
        return products;
    }

    /** @return histórico de interações dos clientes */
    public CustomerInteractionsResource interactions() {
        return interactions;
    }

    /**
     * Cria ou atualiza o cliente {@code externalId} ({@code PUT /customers/{external_id}}). Só os campos
     * informados mudam (veja {@link PatchRequest}).
     *
     * @param externalId id do cliente no seu sistema (qualquer texto; é codificado no caminho)
     * @param customer campos a gravar
     * @return o cliente gravado
     */
    public Customer upsert(String externalId, CustomerUpsert customer) {
        return upsert(externalId, customer, null);
    }

    /**
     * Cria ou atualiza o cliente — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param customer campos a gravar
     * @param options opções da chamada (ex.: {@code idempotencyKey}); pode ser {@code null}
     * @return o cliente gravado
     */
    public Customer upsert(String externalId, CustomerUpsert customer, RequestOptions options) {
        Objects.requireNonNull(customer, "customer");
        return transport.call("PUT", path(externalId), null, customer.body(), options, Customer::from);
    }

    /**
     * Busca o cliente pelo seu {@code external_id} ({@code GET /customers/{external_id}}).
     *
     * @param externalId id do cliente no seu sistema
     * @return o cliente
     * @throws NotFoundException {@code CUSTOMER_NOT_FOUND}
     */
    public Customer get(String externalId) {
        return get(externalId, null);
    }

    /**
     * Busca o cliente — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return o cliente
     */
    public Customer get(String externalId, RequestOptions options) {
        return transport.call("GET", path(externalId), null, null, options, Customer::from);
    }

    /**
     * Primeira página de clientes, com os padrões da API ({@code GET /customers}).
     *
     * @return a página
     */
    public Page<Customer> list() {
        return list(null, null);
    }

    /**
     * Uma página de clientes ({@code GET /customers}).
     *
     * @param params filtros e paginação; pode ser {@code null}
     * @return a página
     */
    public Page<Customer> list(CustomerListParams params) {
        return list(params, null);
    }

    /**
     * Uma página de clientes — com opções da chamada.
     *
     * @param params filtros e paginação; pode ser {@code null}
     * @param options opções da chamada; pode ser {@code null}
     * @return a página
     */
    public Page<Customer> list(CustomerListParams params, RequestOptions options) {
        CustomerListParams p = params == null ? CustomerListParams.builder().build() : params;
        return transport.page("/customers", p.query(p.getPage(), p.getPageSize()), options, Customer::from);
    }

    /**
     * TODOS os clientes, página a página sob demanda (100 por página).
     *
     * @return iterável preguiçoso
     */
    public PagedIterable<Customer> listAll() {
        return listAll(null, null);
    }

    /**
     * TODOS os clientes que batem com os filtros, página a página sob demanda. Com {@code updatedSince}, é a
     * sincronização incremental.
     *
     * @param params filtros ({@code page} não é aceito; {@code pageSize} padrão 100); pode ser {@code null}
     * @return iterável preguiçoso
     */
    public PagedIterable<Customer> listAll(CustomerListParams params) {
        return listAll(params, null);
    }

    /**
     * TODOS os clientes — com opções aplicadas a cada página.
     *
     * @param params filtros; pode ser {@code null}
     * @param options opções de cada chamada; pode ser {@code null}
     * @return iterável preguiçoso
     */
    public PagedIterable<Customer> listAll(CustomerListParams params, RequestOptions options) {
        CustomerListParams p = params == null ? CustomerListParams.builder().build() : params;
        int size = Paths.pageSizeForAll(p.getPage(), p.getPageSize());
        return new PagedIterable<>(page -> transport.page("/customers", p.query(page, size), options, Customer::from));
    }

    /**
     * Exclui o cliente ({@code DELETE /customers/{external_id}}).
     *
     * @param externalId id do cliente no seu sistema
     * @return {@code {deleted: true}}
     */
    public DeleteResult delete(String externalId) {
        return delete(externalId, null);
    }

    /**
     * Exclui o cliente — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return {@code {deleted: true}}
     */
    public DeleteResult delete(String externalId, RequestOptions options) {
        return transport.call("DELETE", path(externalId), null, null, options, DeleteResult::from);
    }

    static String path(String externalId) {
        return "/customers/" + Paths.segment(externalId, "externalId");
    }
}
