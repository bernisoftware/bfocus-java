package br.com.bernisoftware.bfocus;

/** Resultado de um artigo no lote — {@code KBBatchResult}. */
public final class KbBatchItemResult extends ApiObject {
    private final String externalId;
    private final boolean ok;
    private final String action;
    private final String error;
    private final KbArticleSummary article;

    KbBatchItemResult(Wire w) {
        super(w);
        externalId = w.string("external_id");
        ok = w.bool("ok", false);
        action = w.string("action");
        error = w.string("error");
        article = w.nested("article", KbArticleSummary::from);
    }

    static KbBatchItemResult from(Object json) {
        return new KbBatchItemResult(Wire.of(json, "KbBatchItemResult"));
    }

    /** @return {@code external_id} do artigo */
    public String getExternalId() {
        return externalId;
    }

    /** @return deu certo */
    public boolean isOk() {
        return ok;
    }

    /** @return {@code created}, {@code updated} ou {@code unchanged}; {@code null} em erro */
    public String getAction() {
        return action;
    }

    /** @return código do erro (ex.: {@code KB_ARTICLE_TITLE_REQUIRED}); {@code null} quando deu certo */
    public String getError() {
        return error;
    }

    /** @return o artigo gravado, sem corpo; {@code null} em erro */
    public KbArticleSummary getArticle() {
        return article;
    }
}
