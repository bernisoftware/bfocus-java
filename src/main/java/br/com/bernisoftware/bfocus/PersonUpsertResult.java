package br.com.bernisoftware.bfocus;

/** A pessoa gravada por {@link PeopleResource#upsert(String, String, PersonUpsert)}, com o que aconteceu. */
public final class PersonUpsertResult extends Person {
    private final String status;
    private final boolean linked;
    private final String mergedInto;

    PersonUpsertResult(Wire w) {
        super(w);
        status = w.string("status");
        linked = w.bool("linked", false);
        mergedInto = w.string("merged_into");
    }

    static PersonUpsertResult from(Object json) {
        return new PersonUpsertResult(Wire.of(json, "PersonUpsertResult"));
    }

    /**
     * @return {@code created} (pessoa nova), {@code updated} (já existia — inclusive quando foi achada pelo
     *         e-mail/telefone e adotada, ou ligada a mais este cliente) ou {@code unchanged}
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return {@code true} quando a pessoa JÁ EXISTIA em outro cliente e esta chamada a ligou também a este —
     *         o cadastro é único e ela circula pelos dois; nada foi transferido nem duplicado
     */
    public boolean isLinked() {
        return linked;
    }

    /**
     * @return quando o id enviado é um APELIDO: o {@code external_id} principal do cadastro (atualize o id do
     *         seu lado); senão {@code null}
     */
    public String getMergedInto() {
        return mergedInto;
    }
}
