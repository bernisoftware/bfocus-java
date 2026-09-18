package br.com.bernisoftware.bfocus;

import br.com.bernisoftware.bfocus.internal.Json;
import java.util.Collections;
import java.util.Map;

/**
 * Base dos modelos de resposta. Os modelos são imutáveis, com getters tipados; campos desconhecidos são
 * ignorados nos getters (a API ganha campos sem aviso) mas preservados em {@link #toJson()}.
 *
 * <p>Dois modelos são iguais ({@code equals}) quando vieram do mesmo JSON.
 */
public abstract class ApiObject {
    private final Map<String, Object> raw;

    ApiObject(Wire wire) {
        // O codec já entrega mapas imutáveis; o wrapper garante o contrato de toMap() para qualquer origem.
        this.raw = Collections.unmodifiableMap(wire.map());
    }

    Map<String, Object> raw() {
        return raw;
    }

    /**
     * O objeto como veio da API, como mapa JSON imutável — inclui campos que esta versão da SDK ainda não
     * conhece. Valores: {@code String}, {@code Long}/{@code BigDecimal}, {@code Boolean}, {@code List},
     * {@code Map} ou {@code null}.
     *
     * @return o mapa (imutável)
     */
    public final Map<String, Object> toMap() {
        return raw;
    }

    /**
     * Um campo qualquer da resposta, pelo nome JSON — útil para campos novos da API antes de atualizar a SDK.
     *
     * @param field nome no JSON (ex.: {@code "external_id"})
     * @return o valor como veio (ver {@link #toMap()}), ou {@code null}
     */
    public final Object getRaw(String field) {
        return raw.get(field);
    }

    /**
     * O objeto como veio da API, em JSON (inclui campos que esta versão da SDK não conhece). Útil para log ou
     * para guardar o payload original.
     *
     * @return o JSON compacto
     */
    public final String toJson() {
        return Json.write(raw);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + toJson();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass() && raw.equals(((ApiObject) o).raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }
}
