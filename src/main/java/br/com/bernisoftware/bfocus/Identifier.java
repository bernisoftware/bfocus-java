package br.com.bernisoftware.bfocus;

/** Identificador extra de um cliente ou de uma pessoa — {@code IdentifierOut}. */
public final class Identifier extends ApiObject {
    private final String externalId;
    private final String label;
    private final String source;

    Identifier(Wire w) {
        super(w);
        externalId = w.string("external_id");
        label = w.string("label");
        source = w.string("source");
    }

    static Identifier from(Object json) {
        return new Identifier(Wire.of(json, "Identifier"));
    }

    /** @return o id do outro sistema ligado ao cadastro */
    public String getExternalId() {
        return externalId;
    }

    /** @return rótulo livre (ex.: nome do sistema), ou {@code null} */
    public String getLabel() {
        return label;
    }

    /** @return quem ligou: {@code api}, {@code panel}, {@code import}… */
    public String getSource() {
        return source;
    }
}
