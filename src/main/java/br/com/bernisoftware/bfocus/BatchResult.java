package br.com.bernisoftware.bfocus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado de {@link CustomersResource#batch(java.util.Collection)} e {@link PeopleResource#batch(java.util.Collection)}
 * — {@code BatchOut}: um resultado por item, na ordem enviada, e os contadores. Um item com erro não desfaz os
 * outros.
 */
public final class BatchResult extends ApiObject {
    private final List<BatchItemResult> results;
    private final BatchSummary summary;

    BatchResult(Wire w) {
        super(w);
        results = w.list("results", BatchItemResult::from);
        summary = BatchSummary.from(w.object("summary"));
    }

    static BatchResult from(Object json) {
        return new BatchResult(Wire.of(json, "BatchResult"));
    }

    /** Resultado de um lote vazio (nenhuma requisição é feita). */
    static BatchResult empty() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("created", 0L);
        summary.put("updated", 0L);
        summary.put("unchanged", 0L);
        summary.put("error", 0L);
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("results", Collections.emptyList());
        raw.put("summary", Collections.unmodifiableMap(summary));
        return from(Collections.unmodifiableMap(raw));
    }

    /** @return um resultado por item, na ordem enviada (lista imutável) */
    public List<BatchItemResult> getResults() {
        return results;
    }

    /** @return os contadores ({@code summary}) */
    public BatchSummary getSummary() {
        return summary;
    }
}
