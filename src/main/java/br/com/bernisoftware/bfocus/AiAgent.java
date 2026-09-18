package br.com.bernisoftware.bfocus;

import java.time.OffsetDateTime;

/** Agente de IA — {@code AgentOut}. */
public final class AiAgent extends ApiObject {
    private final String id;
    private final String name;
    private final ProductRef product;
    private final boolean active;
    private final String persona;
    private final String scope;
    private final String avatarUrl;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    AiAgent(Wire w) {
        super(w);
        id = w.string("id");
        name = w.string("name");
        product = w.nested("product", ProductRef::from);
        active = w.bool("active", false);
        persona = w.string("persona");
        scope = w.string("scope");
        avatarUrl = w.string("avatar_url");
        createdAt = w.date("created_at");
        updatedAt = w.date("updated_at");
    }

    static AiAgent from(Object json) {
        return new AiAgent(Wire.of(json, "AiAgent"));
    }

    /** @return id no bFocus (UUID) */
    public String getId() {
        return id;
    }

    /** @return nome */
    public String getName() {
        return name;
    }

    /** @return produto que o agente atende */
    public ProductRef getProduct() {
        return product;
    }

    /** @return ativo ({@code active}) */
    public boolean isActive() {
        return active;
    }

    /** @return persona, ou {@code null} */
    public String getPersona() {
        return persona;
    }

    /** @return o que o agente atende e o que não atende, ou {@code null} */
    public String getScope() {
        return scope;
    }

    /** @return URL do avatar, ou {@code null} */
    public String getAvatarUrl() {
        return avatarUrl;
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
