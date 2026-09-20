package br.com.bernisoftware.bfocus;

/** O que {@link PeopleResource#delete(String, String)} devolve: a pessoa + se ela apenas saiu DESTE cliente. */
public final class PersonRevokeResult extends Person {
    private final boolean unlinked;

    PersonRevokeResult(Wire w) {
        super(w);
        unlinked = w.bool("unlinked", false);
    }

    static PersonRevokeResult from(Object json) {
        return new PersonRevokeResult(Wire.of(json, "PersonRevokeResult"));
    }

    /**
     * @return {@code true} quando ela continua com acesso, porque também é de OUTROS clientes — o acesso é do
     *         vínculo; {@code false} quando era só deste cliente e foi desligada, como sempre
     */
    public boolean isUnlinked() {
        return unlinked;
    }
}
