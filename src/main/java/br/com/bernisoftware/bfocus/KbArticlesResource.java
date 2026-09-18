package br.com.bernisoftware.bfocus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Artigos da base de conhecimento — {@code client.kb().articles()}. */
public final class KbArticlesResource {
    /** Máximo de artigos por requisição de lote (limite da API). */
    public static final int BATCH_SIZE = 100;

    private final Transport transport;

    KbArticlesResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * Primeira página de artigos ({@code GET /kb/articles}). Os itens vêm sem corpo ({@link KbArticleSummary}).
     *
     * @return a página
     */
    public Page<KbArticleSummary> list() {
        return list(null, null);
    }

    /**
     * Uma página de artigos, sem corpo.
     *
     * @param params filtros e paginação; pode ser {@code null}
     * @return a página
     */
    public Page<KbArticleSummary> list(KbArticleListParams params) {
        return list(params, null);
    }

    /**
     * Uma página de artigos, sem corpo — com opções da chamada.
     *
     * @param params filtros e paginação; pode ser {@code null}
     * @param options opções da chamada; pode ser {@code null}
     * @return a página
     */
    public Page<KbArticleSummary> list(KbArticleListParams params, RequestOptions options) {
        KbArticleListParams p = params == null ? KbArticleListParams.builder().build() : params;
        return transport.page("/kb/articles", p.query(p.getPage(), p.getPageSize()), options, KbArticleSummary::from);
    }

    /**
     * TODOS os artigos, sem corpo, página a página sob demanda (100 por página).
     *
     * @return iterável preguiçoso
     */
    public PagedIterable<KbArticleSummary> listAll() {
        return listAll(null, null);
    }

    /**
     * TODOS os artigos que batem com os filtros, sem corpo.
     *
     * @param params filtros ({@code page} não é aceito; {@code pageSize} padrão 100); pode ser {@code null}
     * @return iterável preguiçoso
     */
    public PagedIterable<KbArticleSummary> listAll(KbArticleListParams params) {
        return listAll(params, null);
    }

    /**
     * TODOS os artigos — com opções aplicadas a cada página.
     *
     * @param params filtros; pode ser {@code null}
     * @param options opções de cada chamada; pode ser {@code null}
     * @return iterável preguiçoso
     */
    public PagedIterable<KbArticleSummary> listAll(KbArticleListParams params, RequestOptions options) {
        KbArticleListParams p = params == null ? KbArticleListParams.builder().build() : params;
        int size = Paths.pageSizeForAll(p.getPage(), p.getPageSize());
        return new PagedIterable<>(page -> transport.page("/kb/articles", p.query(page, size), options, KbArticleSummary::from));
    }

    /**
     * Busca o artigo pelo seu {@code external_id} ({@code GET /kb/articles/{external_id}}).
     *
     * @param externalId id do artigo no seu sistema (sem {@code /})
     * @return o artigo completo, com {@link KbArticle#getBodyHtml()}
     */
    public KbArticle get(String externalId) {
        return get(externalId, null);
    }

    /**
     * Busca o artigo — com opções da chamada.
     *
     * @param externalId id do artigo no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return o artigo completo
     */
    public KbArticle get(String externalId, RequestOptions options) {
        return transport.call("GET", path(externalId), null, null, options, KbArticle::from);
    }

    /**
     * Cria ou atualiza o artigo ({@code PUT /kb/articles/{external_id}}). Só os campos informados mudam;
     * {@code product(null)} torna o artigo global.
     *
     * @param externalId id do artigo no seu sistema (sem {@code /}; use {@code :} para hierarquia)
     * @param article campos a gravar
     * @return o artigo gravado
     * @throws ConflictException {@code KB_ARTICLE_EMPTY} (corpo vazio)
     */
    public KbArticle upsert(String externalId, KbArticleUpsert article) {
        return upsert(externalId, article, null);
    }

    /**
     * Cria ou atualiza o artigo — com opções da chamada.
     *
     * @param externalId id do artigo no seu sistema
     * @param article campos a gravar
     * @param options opções da chamada; pode ser {@code null}
     * @return o artigo gravado
     */
    public KbArticle upsert(String externalId, KbArticleUpsert article, RequestOptions options) {
        Objects.requireNonNull(article, "article");
        return transport.call("PUT", path(externalId), null, article.body(), options, KbArticle::from);
    }

    /**
     * Cria ou atualiza QUALQUER quantidade de artigos ({@code POST /kb/articles/batch}): a SDK divide em lotes de
     * {@link #BATCH_SIZE}, envia em sequência e devolve UM resultado agregado (resultados na ordem enviada,
     * contadores somados). Lista vazia devolve o resultado zerado sem requisição. Falha de um artigo não derruba o
     * lote (veja {@link KbBatchItemResult#getError()}); um erro HTTP interrompe os lotes seguintes (os anteriores
     * já foram gravados — rodar de novo é seguro).
     *
     * @param articles os artigos
     * @return o resultado agregado
     */
    public KbBatchResult batchUpsert(Collection<KbBatchArticle> articles) {
        return batchUpsert(articles, null);
    }

    /**
     * Lote de artigos — com opções. Com {@code idempotencyKey} própria, o 1º lote usa a chave como veio e os
     * seguintes {@code <chave>:2}, {@code <chave>:3}…; sem ela, cada lote gera a sua.
     *
     * @param articles os artigos
     * @param options opções de cada lote; pode ser {@code null}
     * @return o resultado agregado
     */
    public KbBatchResult batchUpsert(Collection<KbBatchArticle> articles, RequestOptions options) {
        Objects.requireNonNull(articles, "articles");
        List<KbBatchArticle> list = new ArrayList<>(articles);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null) {
                throw new IllegalArgumentException("O artigo #" + i + " do lote é null.");
            }
        }
        List<KbBatchResult> chunks = new ArrayList<>();
        String ownKey = options == null ? null : options.getIdempotencyKey();
        for (int start = 0, chunk = 0; start < list.size(); start += BATCH_SIZE, chunk++) {
            List<Object> items = new ArrayList<>();
            for (KbBatchArticle article : list.subList(start, Math.min(start + BATCH_SIZE, list.size()))) {
                items.add(article.body());
            }
            Map<String, Object> body = Collections.singletonMap("articles", items);
            RequestOptions chunkOptions = options;
            if (chunk > 0 && ownKey != null) {
                chunkOptions = options.withIdempotencyKey(ownKey + ":" + (chunk + 1));
            }
            chunks.add(transport.call("POST", "/kb/articles/batch", null, body, chunkOptions, KbBatchResult::from));
        }
        return KbBatchResult.aggregate(chunks);
    }

    /**
     * Publica o artigo ({@code POST /kb/articles/{external_id}/publish}).
     *
     * @param externalId id do artigo no seu sistema
     * @return o artigo publicado
     */
    public KbArticle publish(String externalId) {
        return publish(externalId, null);
    }

    /**
     * Publica o artigo — com opções da chamada.
     *
     * @param externalId id do artigo no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return o artigo publicado
     */
    public KbArticle publish(String externalId, RequestOptions options) {
        return transport.call("POST", path(externalId) + "/publish", null, null, options, KbArticle::from);
    }

    /**
     * Volta o artigo para rascunho ({@code POST /kb/articles/{external_id}/unpublish}).
     *
     * @param externalId id do artigo no seu sistema
     * @return o artigo em rascunho
     */
    public KbArticle unpublish(String externalId) {
        return unpublish(externalId, null);
    }

    /**
     * Volta o artigo para rascunho — com opções da chamada.
     *
     * @param externalId id do artigo no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return o artigo em rascunho
     */
    public KbArticle unpublish(String externalId, RequestOptions options) {
        return transport.call("POST", path(externalId) + "/unpublish", null, null, options, KbArticle::from);
    }

    /**
     * Exclui o artigo ({@code DELETE /kb/articles/{external_id}}).
     *
     * @param externalId id do artigo no seu sistema
     * @return {@code {deleted: true}}
     */
    public DeleteResult delete(String externalId) {
        return delete(externalId, null);
    }

    /**
     * Exclui o artigo — com opções da chamada.
     *
     * @param externalId id do artigo no seu sistema
     * @param options opções da chamada; pode ser {@code null}
     * @return {@code {deleted: true}}
     */
    public DeleteResult delete(String externalId, RequestOptions options) {
        return transport.call("DELETE", path(externalId), null, null, options, DeleteResult::from);
    }

    private static String path(String externalId) {
        return "/kb/articles/" + Paths.kbExternalId(externalId, "externalId");
    }
}
