package br.com.bernisoftware.bfocus;

/** Contadores de um lote — {@code BatchSummary}. */
public final class BatchSummary extends ApiObject {
    private final int created;
    private final int updated;
    private final int unchanged;
    private final int error;

    BatchSummary(Wire w) {
        super(w);
        created = w.integer("created");
        updated = w.integer("updated");
        unchanged = w.integer("unchanged");
        error = w.integer("error");
    }

    static BatchSummary from(Object json) {
        return new BatchSummary(Wire.of(json, "BatchSummary"));
    }

    /** @return itens criados */
    public int getCreated() {
        return created;
    }

    /** @return itens alterados */
    public int getUpdated() {
        return updated;
    }

    /** @return itens sem mudança */
    public int getUnchanged() {
        return unchanged;
    }

    /** @return itens com erro (veja {@link BatchItemResult#getError()}) */
    public int getError() {
        return error;
    }
}
