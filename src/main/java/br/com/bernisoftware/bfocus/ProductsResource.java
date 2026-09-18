package br.com.bernisoftware.bfocus;

import java.util.List;
import java.util.Objects;

/** Catálogo de produtos — {@code client.products()}. Escopos: {@code products:read} / {@code products:write}. */
public final class ProductsResource {
    private final Transport transport;

    ProductsResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * Lista os produtos ativos ({@code GET /products}).
     *
     * @return os produtos (lista imutável)
     */
    public List<Product> list() {
        return list(null, null);
    }

    /**
     * Lista os produtos.
     *
     * @param includeInactive {@code true} inclui os arquivados
     * @return os produtos
     */
    public List<Product> list(boolean includeInactive) {
        return list(includeInactive, null);
    }

    /**
     * Lista os produtos — com opções da chamada.
     *
     * @param includeInactive {@code true} inclui os arquivados; {@code null} = padrão da API
     * @param options opções da chamada; pode ser {@code null}
     * @return os produtos
     */
    public List<Product> list(Boolean includeInactive, RequestOptions options) {
        return transport.call("GET", "/products", new Query().add("include_inactive", includeInactive), null, options,
                json -> Wire.list(json, Product::from, "data"));
    }

    /**
     * Busca o produto pelo slug ({@code GET /products/{slug}}).
     *
     * @param slug slug do produto
     * @return o produto
     */
    public Product get(String slug) {
        return get(slug, null);
    }

    /**
     * Busca o produto pelo slug — com opções da chamada.
     *
     * @param slug slug do produto
     * @param options opções da chamada; pode ser {@code null}
     * @return o produto
     */
    public Product get(String slug, RequestOptions options) {
        return transport.call("GET", path(slug), null, null, options, Product::from);
    }

    /**
     * Cria ou atualiza o produto ({@code PUT /products/{slug}}). Só os campos informados mudam.
     *
     * @param slug slug (minúsculas, números, {@code -} e {@code _})
     * @param product campos a gravar
     * @return o produto gravado
     */
    public Product upsert(String slug, ProductUpsert product) {
        return upsert(slug, product, null);
    }

    /**
     * Cria ou atualiza o produto — com opções da chamada.
     *
     * @param slug slug do produto
     * @param product campos a gravar
     * @param options opções da chamada; pode ser {@code null}
     * @return o produto gravado
     */
    public Product upsert(String slug, ProductUpsert product, RequestOptions options) {
        Objects.requireNonNull(product, "product");
        return transport.call("PUT", path(slug), null, product.body(), options, Product::from);
    }

    /**
     * Arquiva o produto ({@code DELETE /products/{slug}}) — não apaga; some das listas padrão.
     *
     * @param slug slug do produto
     * @return o produto arquivado
     */
    public Product archive(String slug) {
        return archive(slug, null);
    }

    /**
     * Arquiva o produto — com opções da chamada.
     *
     * @param slug slug do produto
     * @param options opções da chamada; pode ser {@code null}
     * @return o produto arquivado
     */
    public Product archive(String slug, RequestOptions options) {
        return transport.call("DELETE", path(slug), null, null, options, Product::from);
    }

    static String path(String slug) {
        return "/products/" + Paths.segment(slug, "slug");
    }
}
