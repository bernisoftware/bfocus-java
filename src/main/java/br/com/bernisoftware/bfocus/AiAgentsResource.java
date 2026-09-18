package br.com.bernisoftware.bfocus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agentes de IA — {@code client.aiAgents()}. Escopos: {@code ai_agents:read} / {@code ai_agents:preview}. Exige o
 * módulo de Atendimento (sem ele: {@link PermissionDeniedException} {@code MODULE_NOT_CONTRACTED}).
 */
public final class AiAgentsResource {
    private final Transport transport;

    AiAgentsResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * Lista os agentes de IA ({@code GET /ai-agents}).
     *
     * @return os agentes (lista imutável)
     */
    public List<AiAgent> list() {
        return list(null);
    }

    /**
     * Lista os agentes de IA — com opções da chamada.
     *
     * @param options opções da chamada; pode ser {@code null}
     * @return os agentes
     */
    public List<AiAgent> list(RequestOptions options) {
        return transport.call("GET", "/ai-agents", null, null, options, json -> Wire.list(json, AiAgent::from, "data"));
    }

    /**
     * Busca o agente ({@code GET /ai-agents/{agent_id}}).
     *
     * @param agentId id do agente (UUID)
     * @return o agente
     */
    public AiAgent get(String agentId) {
        return get(agentId, null);
    }

    /**
     * Busca o agente — com opções da chamada.
     *
     * @param agentId id do agente (UUID)
     * @param options opções da chamada; pode ser {@code null}
     * @return o agente
     */
    public AiAgent get(String agentId, RequestOptions options) {
        return transport.call("GET", path(agentId), null, null, options, AiAgent::from);
    }

    /**
     * Testa a resposta do agente a uma mensagem, sem abrir atendimento
     * ({@code POST /ai-agents/{agent_id}/preview}). Consome IA da conta.
     *
     * @param agentId id do agente (UUID)
     * @param message mensagem do cliente (1–4000)
     * @return a resposta e o diagnóstico do agente
     * @throws ConflictException {@code AI_DISABLED}
     */
    public AiAgentPreview preview(String agentId, String message) {
        return preview(agentId, message, null, null);
    }

    /**
     * Testa a resposta do agente, com os turnos anteriores da conversa.
     *
     * @param agentId id do agente (UUID)
     * @param message mensagem do cliente (1–4000)
     * @param history turnos anteriores (até 20); pode ser {@code null}
     * @return a resposta e o diagnóstico do agente
     */
    public AiAgentPreview preview(String agentId, String message, List<AiAgentPreviewTurn> history) {
        return preview(agentId, message, history, null);
    }

    /**
     * Testa a resposta do agente — com opções da chamada.
     *
     * @param agentId id do agente (UUID)
     * @param message mensagem do cliente (1–4000)
     * @param history turnos anteriores (até 20); pode ser {@code null}
     * @param options opções da chamada; pode ser {@code null}
     * @return a resposta e o diagnóstico do agente
     */
    public AiAgentPreview preview(String agentId, String message, List<AiAgentPreviewTurn> history, RequestOptions options) {
        Objects.requireNonNull(message, "message");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        if (history != null) {
            List<Object> turns = new ArrayList<>(history.size());
            for (AiAgentPreviewTurn turn : history) {
                turns.add(Objects.requireNonNull(turn, "history contém null").toMap());
            }
            body.put("history", turns);
        }
        return transport.call("POST", path(agentId) + "/preview", null, body, options, AiAgentPreview::from);
    }

    private static String path(String agentId) {
        return "/ai-agents/" + Paths.segment(agentId, "agentId");
    }
}
