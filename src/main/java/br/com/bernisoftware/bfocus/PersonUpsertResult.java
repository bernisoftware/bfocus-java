package br.com.bernisoftware.bfocus;

/** A pessoa gravada por {@link PeopleResource#upsert(String, String, PersonUpsert)}, com o que aconteceu. */
public final class PersonUpsertResult extends Person {
    private final String status;

    PersonUpsertResult(Wire w) {
        super(w);
        status = w.string("status");
    }

    static PersonUpsertResult from(Object json) {
        return new PersonUpsertResult(Wire.of(json, "PersonUpsertResult"));
    }

    /**
     * @return {@code created} (pessoa nova), {@code updated} (já existia — inclusive quando foi achada pelo
     *         e-mail/telefone e adotada, ou transferida de outro cliente) ou {@code unchanged}
     */
    public String getStatus() {
        return status;
    }
}
