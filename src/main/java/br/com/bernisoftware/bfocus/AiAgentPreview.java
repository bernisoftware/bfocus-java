package br.com.bernisoftware.bfocus;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resposta de teste de um agente de IA — {@code AgentPreviewOut} ({@link AiAgentsResource#preview}). A spec
 * permite campos extras de diagnóstico: ficam em {@link #getExtras()}.
 *
 * <p>Valores JSON sem tipo fixo ({@code citations}, {@code sources}, {@code collected}, {@code missing}) vêm como
 * tipos padrão imutáveis: {@code Map}, {@code List}, {@code String}, {@code Long}/{@code BigDecimal},
 * {@code Boolean} ou {@code null}.
 */
public final class AiAgentPreview extends ApiObject {
    private static final Set<String> KNOWN = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "action", "answer_html", "escalated", "refused", "handoff_reason", "confidence", "topic", "guards",
            "citations", "sources", "collected", "missing")));

    private final String action;
    private final String answerHtml;
    private final boolean escalated;
    private final boolean refused;
    private final String handoffReason;
    private final Double confidence;
    private final String topic;
    private final List<String> guards;
    private final List<Object> citations;
    private final List<Map<String, Object>> sources;
    private final Map<String, Object> collected;
    private final List<Object> missing;
    private final Map<String, Object> extras;

    AiAgentPreview(Wire w) {
        super(w);
        action = w.string("action");
        answerHtml = w.string("answer_html");
        escalated = w.bool("escalated", false);
        refused = w.bool("refused", false);
        handoffReason = w.string("handoff_reason");
        confidence = w.decimal("confidence");
        topic = w.string("topic");
        guards = w.list("guards", Wire::stringItem);
        citations = w.list("citations", item -> item);
        sources = w.list("sources", Wire::objectItem);
        collected = w.object("collected");
        missing = w.list("missing", item -> item);
        extras = w.extras(KNOWN);
    }

    static AiAgentPreview from(Object json) {
        return new AiAgentPreview(Wire.of(json, "AiAgentPreview"));
    }

    /** @return o que o agente decidiu: {@code answer}, {@code handoff} (transferiria a um humano) ou {@code refuse} */
    public String getAction() {
        return action;
    }

    /** @return resposta em HTML, ou {@code null} */
    public String getAnswerHtml() {
        return answerHtml;
    }

    /** @return encaminharia para um humano */
    public boolean isEscalated() {
        return escalated;
    }

    /** @return recusou (fora do escopo) */
    public boolean isRefused() {
        return refused;
    }

    /** @return motivo do encaminhamento, ou {@code null} */
    public String getHandoffReason() {
        return handoffReason;
    }

    /** @return confiança (0–1), ou {@code null} */
    public Double getConfidence() {
        return confidence;
    }

    /** @return assunto identificado na mensagem, ou {@code null} */
    public String getTopic() {
        return topic;
    }

    /** @return salvaguardas acionadas */
    public List<String> getGuards() {
        return guards;
    }

    /** @return fontes citadas na resposta, como vieram */
    public List<Object> getCitations() {
        return citations;
    }

    /** @return fontes usadas (objetos, ex.: {@code {"type": "kb_article", "id": "…", "title": "…"}}) */
    public List<Map<String, Object>> getSources() {
        return sources;
    }

    /** @return dados já coletados do cliente (campo → valor) */
    public Map<String, Object> getCollected() {
        return collected;
    }

    /** @return dados que o agente ainda pediria */
    public List<Object> getMissing() {
        return missing;
    }

    /** @return demais campos do diagnóstico que a API enviar (mapa imutável, nunca {@code null}) */
    public Map<String, Object> getExtras() {
        return extras;
    }

    /**
     * Um campo extra do diagnóstico.
     *
     * @param name nome no JSON
     * @return o valor, ou {@code null}
     */
    public Object getExtra(String name) {
        return extras.get(name);
    }
}
