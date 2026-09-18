package br.com.bernisoftware.bfocus;

import br.com.bernisoftware.bfocus.internal.Json;
import java.util.Map;

/**
 * Base dos corpos de upsert (imutáveis, montados por builder). Os upserts do bFocus são PARCIAIS: só muda o
 * que vai no corpo, e {@code null} explícito LIMPA o campo. Nos builders:
 * <ul>
 * <li>setter <b>não chamado</b> = campo <b>omitido</b> — não vai no corpo e fica como está;</li>
 * <li>setter chamado com valor = o campo muda;</li>
 * <li>setter chamado com {@code null}, ou {@code clear("campo")} (nome Java ou JSON) = o campo vai como
 * {@code null} e é <b>limpo</b>.</li>
 * </ul>
 * Vale a última chamada para cada campo. Nome desconhecido em {@code clear} lança
 * {@link IllegalArgumentException} na hora.
 *
 * <pre>{@code
 * // Muda o nome e limpa o telefone; e-mail, documento etc. ficam como estão.
 * CustomerUpsert.builder().name("Padaria Estrela").clear("phone").build();
 * // Corpo enviado: {"name":"Padaria Estrela","phone":null}
 * }</pre>
 */
public abstract class PatchRequest {
    private final Map<String, Object> body;

    PatchRequest(Map<String, Object> body) {
        this.body = body;
    }

    Map<String, Object> body() {
        return body;
    }

    /**
     * O corpo JSON que a SDK vai enviar (só os campos informados).
     *
     * @return o JSON compacto
     */
    public final String toJson() {
        return Json.write(body);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + toJson();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass() && body.equals(((PatchRequest) o).body);
    }

    @Override
    public int hashCode() {
        return body.hashCode();
    }
}
