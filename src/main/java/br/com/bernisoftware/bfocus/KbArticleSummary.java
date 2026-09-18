package br.com.bernisoftware.bfocus;

import java.time.OffsetDateTime;

/**
 * Artigo da base de conhecimento SEM o corpo — {@code KBArticleSummary}: como vem nas listagens e no resultado do
 * lote. O artigo completo (get, upsert, publish, unpublish) é {@link KbArticle}, que acrescenta
 * {@link KbArticle#getBodyHtml()}.
 */
public class KbArticleSummary extends ApiObject {
    private final String id;
    private final String externalId;
    private final String product;
    private final String title;
    private final String excerpt;
    private final String status;
    private final String origin;
    private final OffsetDateTime publishedAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    KbArticleSummary(Wire w) {
        super(w);
        id = w.string("id");
        externalId = w.string("external_id");
        product = w.string("product");
        title = w.string("title");
        excerpt = w.string("excerpt");
        status = w.string("status");
        origin = w.string("origin");
        publishedAt = w.date("published_at");
        createdAt = w.date("created_at");
        updatedAt = w.date("updated_at");
    }

    static KbArticleSummary from(Object json) {
        return new KbArticleSummary(Wire.of(json, "KbArticleSummary"));
    }

    /** @return id no bFocus (UUID) */
    public String getId() {
        return id;
    }

    /** @return id do artigo no seu sistema, ou {@code null} */
    public String getExternalId() {
        return externalId;
    }

    /** @return slug do produto, ou {@code null} = artigo global (vale para todos os produtos) */
    public String getProduct() {
        return product;
    }

    /** @return título */
    public String getTitle() {
        return title;
    }

    /** @return resumo em texto */
    public String getExcerpt() {
        return excerpt;
    }

    /** @return {@code draft} ou {@code published} */
    public String getStatus() {
        return status;
    }

    /** @return origem ({@code manual}…) */
    public String getOrigin() {
        return origin;
    }

    /** @return publicado em, ou {@code null} */
    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    /** @return criado em, ou {@code null} */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /** @return alterado em, ou {@code null} */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
