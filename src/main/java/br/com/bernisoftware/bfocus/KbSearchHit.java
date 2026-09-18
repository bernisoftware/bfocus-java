package br.com.bernisoftware.bfocus;

/** Resultado da busca na base de conhecimento — {@code KBSearchHit}. */
public final class KbSearchHit extends ApiObject {
    private final String id;
    private final String externalId;
    private final String title;
    private final String excerpt;

    KbSearchHit(Wire w) {
        super(w);
        id = w.string("id");
        externalId = w.string("external_id");
        title = w.string("title");
        excerpt = w.string("excerpt");
    }

    static KbSearchHit from(Object json) {
        return new KbSearchHit(Wire.of(json, "KbSearchHit"));
    }

    /** @return id no bFocus (UUID) */
    public String getId() {
        return id;
    }

    /** @return id do artigo no seu sistema, ou {@code null} */
    public String getExternalId() {
        return externalId;
    }

    /** @return título */
    public String getTitle() {
        return title;
    }

    /** @return resumo */
    public String getExcerpt() {
        return excerpt;
    }
}
