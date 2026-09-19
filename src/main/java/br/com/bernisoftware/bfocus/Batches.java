package br.com.bernisoftware.bfocus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Lotes de {@code customers().batch} e {@code people().batch}: validação (até 500, sem null) e corpo. */
final class Batches {
    private Batches() {
    }

    /**
     * Valida o lote ANTES de qualquer requisição e devolve o corpo {@code {"items": [...]}}, ou {@code null} para
     * lote vazio (a chamada devolve {@link BatchResult#empty()} sem ir à rede).
     */
    static Map<String, Object> body(String op, Collection<? extends PatchRequest> items) {
        Objects.requireNonNull(items, "items");
        if (items.size() > BfocusClient.BATCH_MAX) {
            throw new IllegalArgumentException(op + " aceita até " + BfocusClient.BATCH_MAX + " itens por chamada (recebeu "
                    + items.size() + "); divida em lotes de " + BfocusClient.BATCH_MAX + ".");
        }
        List<Object> out = new ArrayList<>(items.size());
        int i = 0;
        for (PatchRequest item : items) {
            if (item == null) {
                throw new IllegalArgumentException("O item #" + i + " do lote de " + op + " é null.");
            }
            out.add(item.body());
            i++;
        }
        return out.isEmpty() ? null : Collections.singletonMap("items", Collections.unmodifiableList(out));
    }
}
