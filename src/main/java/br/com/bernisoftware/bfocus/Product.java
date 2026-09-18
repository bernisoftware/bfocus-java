package br.com.bernisoftware.bfocus;

import java.time.OffsetDateTime;

/** Produto do catálogo — {@code ProductOut}. */
public final class Product extends ApiObject {
    private final String id;
    private final String slug;
    private final String name;
    private final String description;
    private final String color;
    private final String icon;
    private final boolean active;
    private final int sortOrder;
    private final String currentVersion;
    private final String aiLevel;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    Product(Wire w) {
        super(w);
        id = w.string("id");
        slug = w.string("slug");
        name = w.string("name");
        description = w.string("description");
        color = w.string("color");
        icon = w.string("icon");
        active = w.bool("is_active", false);
        sortOrder = w.integer("sort_order");
        currentVersion = w.string("current_version");
        aiLevel = w.string("ai_level");
        createdAt = w.date("created_at");
        updatedAt = w.date("updated_at");
    }

    static Product from(Object json) {
        return new Product(Wire.of(json, "Product"));
    }

    /** @return id no bFocus (UUID) */
    public String getId() {
        return id;
    }

    /** @return slug */
    public String getSlug() {
        return slug;
    }

    /** @return nome */
    public String getName() {
        return name;
    }

    /** @return descrição, ou {@code null} */
    public String getDescription() {
        return description;
    }

    /** @return cor (ex.: {@code #6366F1}), ou {@code null} */
    public String getColor() {
        return color;
    }

    /** @return ícone, ou {@code null} */
    public String getIcon() {
        return icon;
    }

    /** @return ativo ({@code false} = arquivado) */
    public boolean isActive() {
        return active;
    }

    /** @return ordem de exibição */
    public int getSortOrder() {
        return sortOrder;
    }

    /** @return versão atual (a maior publicada em release notes) */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /** @return nível de IA ({@code none}, {@code assisted} ou {@code autonomous}) */
    public String getAiLevel() {
        return aiLevel;
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
