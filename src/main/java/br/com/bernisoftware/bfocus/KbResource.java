package br.com.bernisoftware.bfocus;

import java.util.List;
import java.util.Objects;

/**
 * Base de conhecimento — {@code client.kb()}. Escopos: {@code kb:read} / {@code kb:write}. Exige o módulo de
 * Atendimento (sem ele: {@link PermissionDeniedException} {@code MODULE_NOT_CONTRACTED}).
 */
public final class KbResource {
    private final Transport transport;
    private final KbArticlesResource articles;

    KbResource(Transport transport) {
        this.transport = transport;
        this.articles = new KbArticlesResource(transport);
    }

    /** @return artigos */
    public KbArticlesResource articles() {
        return articles;
    }

    /**
     * Busca na base de conhecimento ({@code GET /kb/search}) — a mesma busca que os agentes de IA usam.
     *
     * @param q texto buscado (1–500)
     * @return os artigos mais relevantes (padrão da API: 5)
     */
    public List<KbSearchHit> search(String q) {
        return search(q, null, null, null);
    }

    /**
     * Busca na base de conhecimento.
     *
     * @param q texto buscado (1–500)
     * @param product só artigos deste produto (e os globais); pode ser {@code null}
     * @param limit máximo de resultados (1–20); {@code null} = padrão da API (5)
     * @return os artigos mais relevantes
     */
    public List<KbSearchHit> search(String q, String product, Integer limit) {
        return search(q, product, limit, null);
    }

    /**
     * Busca na base de conhecimento — com opções da chamada.
     *
     * @param q texto buscado (1–500)
     * @param product só artigos deste produto (e os globais); pode ser {@code null}
     * @param limit máximo de resultados (1–20); pode ser {@code null}
     * @param options opções da chamada; pode ser {@code null}
     * @return os artigos mais relevantes
     */
    public List<KbSearchHit> search(String q, String product, Integer limit, RequestOptions options) {
        Objects.requireNonNull(q, "q");
        Query query = new Query().add("q", q).add("product", product).add("limit", limit);
        return transport.call("GET", "/kb/search", query, null, options, json -> Wire.list(json, KbSearchHit::from, "data"));
    }
}
