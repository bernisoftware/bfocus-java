package br.com.bernisoftware.bfocus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado agregado de {@link KbArticlesResource#batchUpsert(java.util.Collection)} — {@code KBBatchOut}: um
 * resultado por artigo, na ordem enviada, e os contadores somados de todos os lotes.
 */
public final class KbBatchResult extends ApiObject {
    private final List<KbBatchItemResult> results;
    private final int created;
    private final int updated;
    private final int unchanged;
    private final int failed;

    KbBatchResult(Wire w) {
        super(w);
        results = w.list("results", KbBatchItemResult::from);
        created = w.integer("created");
        updated = w.integer("updated");
        unchanged = w.integer("unchanged");
        failed = w.integer("failed");
    }

    static KbBatchResult from(Object json) {
        return new KbBatchResult(Wire.of(json, "KbBatchResult"));
    }

    /** Junta os resultados de vários lotes (resultados concatenados na ordem, contadores somados). */
    static KbBatchResult aggregate(List<KbBatchResult> chunks) {
        List<Object> results = new ArrayList<>();
        long created = 0;
        long updated = 0;
        long unchanged = 0;
        long failed = 0;
        for (KbBatchResult chunk : chunks) {
            for (KbBatchItemResult item : chunk.results) {
                results.add(item.raw());
            }
            created += chunk.created;
            updated += chunk.updated;
            unchanged += chunk.unchanged;
            failed += chunk.failed;
        }
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("results", Collections.unmodifiableList(results));
        raw.put("created", created);
        raw.put("updated", updated);
        raw.put("unchanged", unchanged);
        raw.put("failed", failed);
        return from(Collections.unmodifiableMap(raw));
    }

    /** @return um resultado por artigo, na ordem enviada (lista imutável) */
    public List<KbBatchItemResult> getResults() {
        return results;
    }

    /** @return artigos criados */
    public int getCreated() {
        return created;
    }

    /** @return artigos alterados */
    public int getUpdated() {
        return updated;
    }

    /** @return artigos sem mudança */
    public int getUnchanged() {
        return unchanged;
    }

    /** @return artigos com erro (veja {@link KbBatchItemResult#getError()}) */
    public int getFailed() {
        return failed;
    }
}
