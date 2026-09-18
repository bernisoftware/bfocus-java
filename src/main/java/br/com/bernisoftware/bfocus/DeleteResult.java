package br.com.bernisoftware.bfocus;

/** Resultado de uma exclusão — {@code {"deleted": true}}. */
public final class DeleteResult extends ApiObject {
    private final boolean deleted;

    DeleteResult(Wire w) {
        super(w);
        deleted = w.bool("deleted", true);
    }

    static DeleteResult from(Object json) {
        return new DeleteResult(Wire.of(json, "DeleteResult"));
    }

    /** @return excluído */
    public boolean isDeleted() {
        return deleted;
    }
}
