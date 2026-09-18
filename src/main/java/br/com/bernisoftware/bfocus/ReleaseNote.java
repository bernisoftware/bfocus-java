package br.com.bernisoftware.bfocus;

import java.time.OffsetDateTime;

/** Release note de um produto — {@code ReleaseNoteOut}. */
public final class ReleaseNote extends ApiObject {
    private final String id;
    private final String product;
    private final String version;
    private final String title;
    private final String descriptionHtml;
    private final String audience;
    private final boolean published;
    private final boolean requireAckInternal;
    private final boolean requireAckExternal;
    private final OffsetDateTime publishedAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    ReleaseNote(Wire w) {
        super(w);
        id = w.string("id");
        product = w.string("product");
        version = w.string("version");
        title = w.string("title");
        descriptionHtml = w.string("description_html");
        audience = w.string("audience");
        published = w.bool("is_published", false);
        requireAckInternal = w.bool("require_ack_internal", false);
        requireAckExternal = w.bool("require_ack_external", false);
        publishedAt = w.date("published_at");
        createdAt = w.date("created_at");
        updatedAt = w.date("updated_at");
    }

    static ReleaseNote from(Object json) {
        return new ReleaseNote(Wire.of(json, "ReleaseNote"));
    }

    /** @return id no bFocus (UUID) */
    public String getId() {
        return id;
    }

    /** @return slug do produto */
    public String getProduct() {
        return product;
    }

    /** @return versão SemVer */
    public String getVersion() {
        return version;
    }

    /** @return título */
    public String getTitle() {
        return title;
    }

    /** @return descrição em HTML */
    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    /** @return público: {@code internal}, {@code external} ou {@code both} */
    public String getAudience() {
        return audience;
    }

    /** @return publicada ({@code is_published}) */
    public boolean isPublished() {
        return published;
    }

    /** @return exige ciência da equipe interna */
    public boolean isRequireAckInternal() {
        return requireAckInternal;
    }

    /** @return exige ciência dos clientes (widget) */
    public boolean isRequireAckExternal() {
        return requireAckExternal;
    }

    /** @return publicada em, ou {@code null} */
    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    /** @return criada em, ou {@code null} */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /** @return alterada em, ou {@code null} */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
