package br.com.bernisoftware.bfocus;

import java.util.List;

/**
 * Cliente com os identificadores extras — {@code CustomerIdentifiersOut}, a resposta de
 * {@link CustomerIdentifiersResource#add(String, String)} e {@link CustomerIdentifiersResource#remove(String, String)}.
 * O {@code external_id} principal continua em {@link #getExternalId()}.
 */
public final class CustomerWithIdentifiers extends Customer {
    private final List<Identifier> identifiers;

    CustomerWithIdentifiers(Wire w) {
        super(w);
        identifiers = w.list("identifiers", Identifier::from);
    }

    static CustomerWithIdentifiers from(Object json) {
        return new CustomerWithIdentifiers(Wire.of(json, "CustomerWithIdentifiers"));
    }

    /** @return identificadores extras (lista imutável, nunca {@code null}; o principal é {@link #getExternalId()}) */
    public List<Identifier> getIdentifiers() {
        return identifiers;
    }
}
