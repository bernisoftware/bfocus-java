package br.com.bernisoftware.bfocus;

import java.util.List;

/** Produtos vinculados a um cliente — {@code client.customers().products()}. */
public final class CustomerProductsResource {
    private final Transport transport;

    CustomerProductsResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * Lista os produtos do cliente ({@code GET /customers/{external_id}/products}).
     *
     * @param externalId id do cliente no seu sistema
     * @return os produtos vinculados (lista imutável)
     */
    public List<ProductRef> list(String externalId) {
        return list(externalId, null);
    }

    /**
     * Lista os produtos do cliente — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return os produtos vinculados
     */
    public List<ProductRef> list(String externalId, RequestOptions options) {
        return transport.call("GET", CustomersResource.path(externalId) + "/products", null, null, options,
                json -> Wire.list(json, ProductRef::from, "data"));
    }

    /**
     * Vincula um produto ao cliente ({@code PUT /customers/{external_id}/products/{slug}}). Idempotente.
     *
     * @param externalId id do cliente no seu sistema
     * @param productSlug slug do produto
     * @return o produto vinculado
     */
    public ProductRef attach(String externalId, String productSlug) {
        return attach(externalId, productSlug, null);
    }

    /**
     * Vincula um produto ao cliente — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param productSlug slug do produto
     * @param options opções da chamada; pode ser {@code null}
     * @return o produto vinculado
     */
    public ProductRef attach(String externalId, String productSlug, RequestOptions options) {
        return transport.call("PUT", path(externalId, productSlug), null, null, options, ProductRef::from);
    }

    /**
     * Desvincula o produto do cliente ({@code DELETE /customers/{external_id}/products/{slug}}).
     *
     * @param externalId id do cliente no seu sistema
     * @param productSlug slug do produto
     * @return {@code {deleted: true}}
     * @throws NotFoundException {@code PRODUCT_NOT_LINKED}
     */
    public DeleteResult detach(String externalId, String productSlug) {
        return detach(externalId, productSlug, null);
    }

    /**
     * Desvincula o produto do cliente — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param productSlug slug do produto
     * @param options opções da chamada; pode ser {@code null}
     * @return {@code {deleted: true}}
     */
    public DeleteResult detach(String externalId, String productSlug, RequestOptions options) {
        return transport.call("DELETE", path(externalId, productSlug), null, null, options, DeleteResult::from);
    }

    private static String path(String externalId, String productSlug) {
        return CustomersResource.path(externalId) + "/products/" + Paths.segment(productSlug, "productSlug");
    }
}
