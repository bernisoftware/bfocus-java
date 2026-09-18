package br.com.bernisoftware.bfocus;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Campo personalizado de um cliente — {@code CustomFieldOut}. A spec permite campos extras: ficam em
 * {@link #getExtras()}.
 */
public final class CustomField extends ApiObject {
    private static final Set<String> KNOWN = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("key", "label", "type", "value", "visibility")));

    private final String key;
    private final String label;
    private final String type;
    private final Object value;
    private final String visibility;
    private final Map<String, Object> extras;

    CustomField(Wire w) {
        super(w);
        key = w.string("key");
        label = w.string("label");
        type = w.string("type", "text");
        value = w.any("value");
        visibility = w.string("visibility", "interno");
        extras = w.extras(KNOWN);
    }

    static CustomField from(Object json) {
        return new CustomField(Wire.of(json, "CustomField"));
    }

    /** @return chave estável do campo */
    public String getKey() {
        return key;
    }

    /** @return rótulo, ou {@code null} */
    public String getLabel() {
        return label;
    }

    /** @return tipo ({@code text}, {@code number}, {@code select}…; padrão {@code text}) */
    public String getType() {
        return type;
    }

    /**
     * Valor como veio no JSON: {@code String}, {@code Long}/{@code BigDecimal}, {@code Boolean}, {@code List},
     * {@code Map} (imutáveis) ou {@code null}.
     *
     * @return o valor
     */
    public Object getValue() {
        return value;
    }

    /** @return quem vê o campo no bFocus (padrão {@code interno}) */
    public String getVisibility() {
        return visibility;
    }

    /** @return campos extras que a API enviar (mapa imutável, nunca {@code null}) */
    public Map<String, Object> getExtras() {
        return extras;
    }

    /**
     * Um campo extra.
     *
     * @param name nome no JSON
     * @return o valor, ou {@code null}
     */
    public Object getExtra(String name) {
        return extras.get(name);
    }
}
