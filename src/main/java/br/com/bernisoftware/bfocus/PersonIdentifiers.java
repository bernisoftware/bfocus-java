package br.com.bernisoftware.bfocus;

import java.util.List;

/**
 * Identificadores de uma pessoa — {@code PersonIdentifiersOut}, a resposta de
 * {@link PersonIdentifiersResource#add(String, String)} e {@link PersonIdentifiersResource#remove(String, String)}.
 */
public final class PersonIdentifiers extends ApiObject {
    private final String externalId;
    private final List<Identifier> identifiers;

    PersonIdentifiers(Wire w) {
        super(w);
        externalId = w.string("external_id");
        identifiers = w.list("identifiers", Identifier::from);
    }

    static PersonIdentifiers from(Object json) {
        return new PersonIdentifiers(Wire.of(json, "PersonIdentifiers"));
    }

    /** @return identificador principal da pessoa, ou {@code null} */
    public String getExternalId() {
        return externalId;
    }

    /** @return identificadores extras (lista imutável, nunca {@code null}) */
    public List<Identifier> getIdentifiers() {
        return identifiers;
    }
}
