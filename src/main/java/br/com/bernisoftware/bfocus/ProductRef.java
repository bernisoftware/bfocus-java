package br.com.bernisoftware.bfocus;

/** Referência resumida a um produto — {@code ProductRef}. */
public final class ProductRef extends ApiObject {
    private final String id;
    private final String slug;
    private final String name;
    private final boolean active;

    ProductRef(Wire w) {
        super(w);
        id = w.string("id");
        slug = w.string("slug");
        name = w.string("name");
        active = w.bool("is_active", false);
    }

    static ProductRef from(Object json) {
        return new ProductRef(Wire.of(json, "ProductRef"));
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

    /** @return ativo ({@code is_active}) */
    public boolean isActive() {
        return active;
    }
}
