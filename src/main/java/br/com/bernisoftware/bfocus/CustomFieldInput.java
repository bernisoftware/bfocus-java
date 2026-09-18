package br.com.bernisoftware.bfocus;

import br.com.bernisoftware.bfocus.internal.Json;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Campo personalizado enviado em {@link CustomerUpsert.Builder#customFields(List)} (imutável).
 *
 * <pre>{@code
 * CustomFieldInput.of("plano", "ouro");
 * CustomFieldInput.builder("limite").type("number").value(1500).label("Limite de crédito").build();
 * }</pre>
 */
public final class CustomFieldInput {
    private final Map<String, Object> map;

    private CustomFieldInput(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * Campo com chave e valor.
     *
     * @param key chave estável (1–80)
     * @param value valor: texto, número, booleano, lista, mapa, data {@code java.time}… ({@code null} = JSON null)
     * @return o campo
     */
    public static CustomFieldInput of(String key, Object value) {
        return builder(key).value(value).build();
    }

    /**
     * Novo builder.
     *
     * @param key chave estável do campo (1–80)
     * @return o builder
     */
    public static Builder builder(String key) {
        return new Builder(key);
    }

    /** @return a chave */
    public String getKey() {
        return (String) map.get("key");
    }

    Map<String, Object> toMap() {
        return map;
    }

    @Override
    public String toString() {
        return "CustomFieldInput" + Json.write(map);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CustomFieldInput && map.equals(((CustomFieldInput) o).map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /** Builder de {@link CustomFieldInput}. Só vai no JSON o que for informado. */
    public static final class Builder {
        private final String key;
        private String label;
        private String type;
        private boolean valueSet;
        private Object value;
        private List<String> options;

        private Builder(String key) {
            Objects.requireNonNull(key, "key");
            if (key.isEmpty()) {
                throw new IllegalArgumentException("A chave do campo personalizado não pode ser vazia.");
            }
            this.key = key;
        }

        /**
         * Rótulo exibido (até 200).
         *
         * @param label o rótulo; {@code null} = não enviar
         * @return este builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Tipo: {@code text} (padrão), {@code textarea}, {@code email}, {@code phone}, {@code url},
         * {@code number}, {@code date}, {@code datetime}, {@code bool}, {@code select} ou {@code file}.
         *
         * @param type o tipo; {@code null} = não enviar
         * @return este builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Valor (qualquer tipo JSON; datas {@code java.time} viram ISO 8601). Validado na hora.
         *
         * @param value o valor ({@code null} = JSON null)
         * @return este builder
         * @throws IllegalArgumentException valor que não vira JSON (tipo não suportado, {@code NaN}…)
         */
        public Builder value(Object value) {
            try {
                this.value = Json.parse(Json.write(value));   // valida e congela (cópia imutável)
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Valor do campo '" + key + "' não vira JSON: " + e.getMessage(), e);
            }
            this.valueSet = true;
            return this;
        }

        /**
         * Opções, para o tipo {@code select}.
         *
         * @param options as opções; {@code null} = não enviar
         * @return este builder
         */
        public Builder options(List<String> options) {
            this.options = options == null ? null : Collections.unmodifiableList(new ArrayList<>(options));
            return this;
        }

        /**
         * Opções, para o tipo {@code select}.
         *
         * @param options as opções
         * @return este builder
         */
        public Builder options(String... options) {
            return options(options == null ? null : Arrays.asList(options));
        }

        /**
         * Monta o campo.
         *
         * @return o campo
         */
        public CustomFieldInput build() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key", key);
            if (label != null) {
                map.put("label", label);
            }
            if (type != null) {
                map.put("type", type);
            }
            if (valueSet) {
                map.put("value", value);
            }
            if (options != null) {
                map.put("options", options);
            }
            return new CustomFieldInput(Collections.unmodifiableMap(map));
        }
    }
}
