package br.com.bernisoftware.bfocus;

import java.time.OffsetDateTime;

/** Interação registrada no histórico de um cliente — {@code InteractionOut}. */
public final class Interaction extends ApiObject {
    private final String id;
    private final String content;
    private final boolean internal;
    private final String authorKind;
    private final String authorName;
    private final OffsetDateTime createdAt;

    Interaction(Wire w) {
        super(w);
        id = w.string("id");
        content = w.string("content");
        internal = w.bool("is_internal", false);
        authorKind = w.string("author_kind");
        authorName = w.string("author_name");
        createdAt = w.date("created_at");
    }

    static Interaction from(Object json) {
        return new Interaction(Wire.of(json, "Interaction"));
    }

    /** @return id no bFocus (UUID) */
    public String getId() {
        return id;
    }

    /** @return conteúdo (HTML) */
    public String getContent() {
        return content;
    }

    /** @return nota interna (não visível ao cliente) */
    public boolean isInternal() {
        return internal;
    }

    /** @return {@code human} (assinada por um usuário) ou {@code system} */
    public String getAuthorKind() {
        return authorKind;
    }

    /** @return nome do autor, ou {@code null} */
    public String getAuthorName() {
        return authorName;
    }

    /** @return criada em, ou {@code null} */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
