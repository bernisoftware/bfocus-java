package br.com.bernisoftware.bfocus;

import java.util.Objects;

/** Histórico de interações de um cliente — {@code client.customers().interactions()}. */
public final class CustomerInteractionsResource {
    private final Transport transport;

    CustomerInteractionsResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * Primeira página das interações do cliente ({@code GET /customers/{external_id}/interactions}).
     *
     * @param externalId id do cliente no seu sistema
     * @return a página
     */
    public Page<Interaction> list(String externalId) {
        return list(externalId, null, null);
    }

    /**
     * Uma página das interações do cliente.
     *
     * @param externalId id do cliente no seu sistema
     * @param params paginação; pode ser {@code null}
     * @return a página
     */
    public Page<Interaction> list(String externalId, InteractionListParams params) {
        return list(externalId, params, null);
    }

    /**
     * Uma página das interações do cliente — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param params paginação; pode ser {@code null}
     * @param options opções da chamada; pode ser {@code null}
     * @return a página
     */
    public Page<Interaction> list(String externalId, InteractionListParams params, RequestOptions options) {
        InteractionListParams p = params == null ? InteractionListParams.builder().build() : params;
        return transport.page(path(externalId), p.query(p.getPage(), p.getPageSize()), options, Interaction::from);
    }

    /**
     * TODAS as interações do cliente, página a página sob demanda (100 por página).
     *
     * @param externalId id do cliente no seu sistema
     * @return iterável preguiçoso
     */
    public PagedIterable<Interaction> listAll(String externalId) {
        return listAll(externalId, null, null);
    }

    /**
     * TODAS as interações do cliente.
     *
     * @param externalId id do cliente no seu sistema
     * @param params paginação ({@code page} não é aceito; {@code pageSize} padrão 100); pode ser {@code null}
     * @return iterável preguiçoso
     */
    public PagedIterable<Interaction> listAll(String externalId, InteractionListParams params) {
        return listAll(externalId, params, null);
    }

    /**
     * TODAS as interações do cliente — com opções aplicadas a cada página.
     *
     * @param externalId id do cliente no seu sistema
     * @param params paginação; pode ser {@code null}
     * @param options opções de cada chamada; pode ser {@code null}
     * @return iterável preguiçoso
     */
    public PagedIterable<Interaction> listAll(String externalId, InteractionListParams params, RequestOptions options) {
        InteractionListParams p = params == null ? InteractionListParams.builder().build() : params;
        int size = Paths.pageSizeForAll(p.getPage(), p.getPageSize());
        String path = path(externalId);
        return new PagedIterable<>(page -> transport.page(path, p.query(page, size), options, Interaction::from));
    }

    /**
     * Registra uma interação (nota interna) no histórico do cliente
     * ({@code POST /customers/{external_id}/interactions}).
     *
     * @param externalId id do cliente no seu sistema
     * @param content texto ou HTML (1–50000)
     * @return a interação criada
     */
    public Interaction create(String externalId, String content) {
        return create(externalId, InteractionCreate.builder(content).build(), null);
    }

    /**
     * Registra uma interação no histórico do cliente — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param content texto ou HTML (1–50000)
     * @param options opções da chamada (ex.: {@code idempotencyKey}); pode ser {@code null}
     * @return a interação criada
     */
    public Interaction create(String externalId, String content, RequestOptions options) {
        return create(externalId, InteractionCreate.builder(content).build(), options);
    }

    /**
     * Registra uma interação com todos os campos ({@code isInternal}, {@code authorEmail}).
     *
     * @param externalId id do cliente no seu sistema
     * @param interaction a interação
     * @return a interação criada
     */
    public Interaction create(String externalId, InteractionCreate interaction) {
        return create(externalId, interaction, null);
    }

    /**
     * Registra uma interação com todos os campos — com opções da chamada.
     *
     * @param externalId id do cliente no seu sistema
     * @param interaction a interação
     * @param options opções da chamada; pode ser {@code null}
     * @return a interação criada
     */
    public Interaction create(String externalId, InteractionCreate interaction, RequestOptions options) {
        Objects.requireNonNull(interaction, "interaction");
        return transport.call("POST", path(externalId), null, interaction.body(), options, Interaction::from);
    }

    private static String path(String externalId) {
        return CustomersResource.path(externalId) + "/interactions";
    }
}
