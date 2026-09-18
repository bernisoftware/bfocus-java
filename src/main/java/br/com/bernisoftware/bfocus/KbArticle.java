package br.com.bernisoftware.bfocus;

/** Artigo completo — {@code KBArticleOut}: o resumo ({@link KbArticleSummary}) + {@link #getBodyHtml()}. */
public final class KbArticle extends KbArticleSummary {
    private final String bodyHtml;

    KbArticle(Wire w) {
        super(w);
        bodyHtml = w.string("body_html");
    }

    static KbArticle from(Object json) {
        return new KbArticle(Wire.of(json, "KbArticle"));
    }

    /** @return corpo em HTML (sanitizado) */
    public String getBodyHtml() {
        return bodyHtml;
    }
}
