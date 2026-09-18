package br.com.bernisoftware.bfocus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Estado de um builder de upsert parcial: guarda SÓ os campos que o usuário tocou (pelo nome JSON). Setter
 * chamado = campo no corpo (com {@code null} = limpar); setter não chamado = omitido. {@code clear(...)} marca
 * campos para limpar pelo nome Java ou JSON.
 */
final class PatchState {
    /** Campos de um tipo de upsert: nome Java/JSON → nome JSON, e quais podem ser limpos. */
    static final class Spec {
        private final String owner;
        private final Map<String, String> wireByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final Set<String> fixed = new HashSet<>();
        private final List<String> clearable = new ArrayList<>();

        Spec(String owner) {
            this.owner = owner;
        }

        Spec field(String javaName, String wireName) {
            wireByName.put(javaName, wireName);
            wireByName.put(wireName, wireName);
            clearable.add(wireName);
            return this;
        }

        /** Campo que a API não aceita como {@code null}. */
        Spec fixed(String javaName, String wireName) {
            wireByName.put(javaName, wireName);
            wireByName.put(wireName, wireName);
            fixed.add(wireName);
            return this;
        }
    }

    private final Spec spec;
    private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    PatchState(Spec spec) {
        this.spec = spec;
    }

    void set(String wireName, Object value) {
        values.put(wireName, value);
    }

    void clear(String... names) {
        Objects.requireNonNull(names, "fields");
        for (String name : names) {
            Objects.requireNonNull(name, "field");
            String wire = spec.wireByName.get(name);
            if (wire == null) {
                throw new IllegalArgumentException("'" + name + "' não é um campo de " + spec.owner
                        + ". Campos que podem ser limpos: " + String.join(", ", spec.clearable) + ".");
            }
            if (spec.fixed.contains(wire)) {
                throw new IllegalArgumentException("O campo '" + wire + "' de " + spec.owner + " não pode ser limpo (a API não aceita null nele).");
            }
            values.put(wire, null);
        }
    }

    Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
