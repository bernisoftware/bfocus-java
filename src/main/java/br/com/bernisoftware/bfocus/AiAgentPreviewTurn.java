package br.com.bernisoftware.bfocus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Turno anterior da conversa em {@link AiAgentsResource#preview(String, String, java.util.List)} (imutável). */
public final class AiAgentPreviewTurn {
    private final String role;
    private final String content;

    /**
     * Cria o turno.
     *
     * @param role {@code customer} ou {@code bot}
     * @param content texto (até 4000)
     */
    public AiAgentPreviewTurn(String role, String content) {
        this.role = Objects.requireNonNull(role, "role");
        this.content = Objects.requireNonNull(content, "content");
    }

    /**
     * Turno do cliente.
     *
     * @param content texto
     * @return o turno
     */
    public static AiAgentPreviewTurn customer(String content) {
        return new AiAgentPreviewTurn("customer", content);
    }

    /**
     * Turno do agente.
     *
     * @param content texto
     * @return o turno
     */
    public static AiAgentPreviewTurn bot(String content) {
        return new AiAgentPreviewTurn("bot", content);
    }

    /** @return {@code customer} ou {@code bot} */
    public String getRole() {
        return role;
    }

    /** @return o texto */
    public String getContent() {
        return content;
    }

    Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("role", role);
        map.put("content", content);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AiAgentPreviewTurn && role.equals(((AiAgentPreviewTurn) o).role)
                && content.equals(((AiAgentPreviewTurn) o).content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }

    @Override
    public String toString() {
        return "AiAgentPreviewTurn{role=" + role + ", content=" + content + "}";
    }
}
