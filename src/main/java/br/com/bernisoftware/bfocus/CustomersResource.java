package br.com.bernisoftware.bfocus;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Clientes — {@code client.customers()}. Escopos: {@code customers:read} / {@code customers:write}.
 * Contatos, produtos vinculados, interações e identificadores extras ficam em {@link #contacts()},
 * {@link #products()}, {@link #interactions()} e {@link #identifiers()}. As pessoas dos clientes ficam em
 * {@link BfocusClient#people()}.
 */
public final class CustomersResource {
    private final Transport transport;
    private final CustomerContactsResource contacts;
    private final CustomerProductsResource products;
    private final CustomerInteractionsResource interactions;
    private final CustomerIdentifiersResource identifiers;

    CustomersResource(Transport transport) {
        this.transport = transport;
        this.contacts = new CustomerContactsResource(transport);
        this.products = new CustomerProductsResource(transport);
        this.interactions = new CustomerInteractionsResource(transport);
        this.identifiers = new CustomerIdentifiersResource(transport);
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

    /** @return identificadores extras dos clientes (ids de outros sistemas seus) */
    public CustomerIdentifiersResource identifiers() {
        return identifiers;
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

    /**
     * Cria ou atualiza até {@link BfocusClient#BATCH_MAX} (500) clientes numa requisição
     * ({@code POST /customers/batch}). A SDK NÃO divide: acima de 500 itens lança {@link IllegalArgumentException}
     * antes de qualquer requisição (o {@code index} de cada resultado é a posição no lote enviado). Lote vazio
     * devolve o resultado zerado sem ir à API. Um item com erro não desfaz os outros.
     *
     * @param items os clientes (até 500)
     * @return um resultado por item + {@code summary}
     * @throws IllegalArgumentException mais de 500 itens ou item {@code null}
     */
    public BatchResult batch(Collection<CustomerBatchItem> items) {
        return batch(items, null);
    }

    /**
     * Lote de clientes — com opções da chamada (um lote = uma chamada lógica, com uma {@code idempotencyKey}).
     *
     * @param items os clientes (até 500)
     * @param options opções da chamada; pode ser {@code null}
     * @return um resultado por item + {@code summary}
     */
    public BatchResult batch(Collection<CustomerBatchItem> items, RequestOptions options) {
        Map<String, Object> body = Batches.body("customers().batch", items);
        if (body == null) {
            return BatchResult.empty();
        }
        return transport.call("POST", "/customers/batch", null, body, options, BatchResult::from);
    }

    static String path(String externalId) {
        return "/customers/" + Paths.segment(externalId, "externalId");
    }
}
