package br.com.bernisoftware.bfocus;

/** Resultado de um item de {@code customers().batch} / {@code people().batch} — {@code BatchItemResult}. */
public final class BatchItemResult extends ApiObject {
    private final int index;
    private final String status;
    private final String externalId;
    private final String mergedInto;
    private final boolean linked;
    private final String error;
    private final Integer code;

    BatchItemResult(Wire w) {
        super(w);
        index = w.integer("index");
        status = w.string("status");
        externalId = w.string("external_id");
        mergedInto = w.string("merged_into");
        linked = w.bool("linked", false);
        error = w.string("error");
        code = w.integerOrNull("code");
    }

    static BatchItemResult from(Object json) {
        return new BatchItemResult(Wire.of(json, "BatchItemResult"));
    }

    /** @return posição do item no lote ENVIADO nesta chamada (0 = primeiro) */
    public int getIndex() {
        return index;
    }

    /** @return {@code created}, {@code updated}, {@code unchanged} ou {@code error} */
    public String getStatus() {
        return status;
    }

    /** @return {@code true} quando o item deu erro ({@code status = error}) */
    public boolean isError() {
        return "error".equals(status);
    }

    /** @return identificador do item (o principal, depois do upsert), ou {@code null} */
    public String getExternalId() {
        return externalId;
    }

    /**
     * @return quando o id enviado é um identificador EXTRA: o {@code external_id} principal do cadastro único
     *         (atualize o id do seu lado); senão {@code null}
     */
    public String getMergedInto() {
        return mergedInto;
    }

    /** @return {@code true} quando a pessoa já existia em outro cliente e este item a ligou também a este */
    public boolean isLinked() {
        return linked;
    }

    /** @return código estável do erro do item (ex.: {@code NAME_REQUIRED}); {@code null} quando deu certo */
    public String getError() {
        return error;
    }

    /** @return status HTTP que o item teria sozinho (só em erro), ou {@code null} */
    public Integer getCode() {
        return code;
    }
}
