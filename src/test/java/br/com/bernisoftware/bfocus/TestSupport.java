package br.com.bernisoftware.bfocus;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.com.bernisoftware.bfocus.internal.Json;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Apoio dos testes: JSON à mão, respostas no envelope da API e localização dos arquivos do projeto.
 *
 * <p>Os casos de conformidade moram em {@code clients/conformance/cases.json} no monorepo. O espelho público
 * ({@code bernisoftware/bfocus-java}) recebe só {@code clients/java} — por isso a suíte lê a cópia
 * {@code src/test/resources/conformance/cases.json}, escrita pelo {@code clients/conformance/generate.py} (não edite
 * à mão). Um teste trava que ela seja idêntica à fonte quando as duas existem (no monorepo).
 */
final class TestSupport {
    static final String P = "/api/v1/integration";

    private TestSupport() {
    }

    /** Raiz do projeto Maven ({@code clients/java} no monorepo). */
    static Path projectDir() {
        String dir = System.getProperty("bfocus.projectDir");
        return Path.of(dir != null && !dir.isEmpty() ? dir : System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    /** A cópia local dos casos de conformidade (no classpath de teste). */
    static Map<String, Object> loadCases() {
        try (InputStream in = TestSupport.class.getResourceAsStream("/conformance/cases.json")) {
            assertNotNull(in, "src/test/resources/conformance/cases.json não está no classpath de teste");
            return map(Json.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ── JSON à mão ───────────────────────────────────────────────────────────────────────────────────────

    /** Objeto JSON mutável, na ordem dada: {@code obj("a", 1, "b", null)}. */
    static Map<String, Object> obj(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("obj(...) precisa de pares chave/valor");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            out.put((String) keyValues[i], keyValues[i + 1]);
        }
        return out;
    }

    static List<Object> arr(Object... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    static Map<String, String> headers(String... keyValues) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            out.put(keyValues[i], keyValues[i + 1]);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object value) {
        return (List<Object>) value;
    }

    static List<Map<String, Object>> mapList(Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list(value)) {
            out.add(map(item));
        }
        return out;
    }

    static List<String> stringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value != null) {
            for (Object item : list(value)) {
                out.add((String) item);
            }
        }
        return out;
    }

    // ── respostas ────────────────────────────────────────────────────────────────────────────────────────

    /** Sucesso no envelope da API. */
    static FakeServer.Reply ok(Object data) {
        return ok(data, null);
    }

    /** Sucesso paginado no envelope da API. */
    static FakeServer.Reply ok(Object data, Map<String, Object> pagination) {
        Map<String, Object> body = obj("code", 200L, "data", data, "message", "Executado com sucesso");
        if (pagination != null) {
            body.put("pagination", pagination);
        }
        return new FakeServer.Reply(200, null, body);
    }

    static Map<String, Object> pagination(int page, int pageSize, int total, int pages) {
        return obj("page", (long) page, "page_size", (long) pageSize, "total", (long) total, "pages", (long) pages);
    }

    /** Erro no envelope da API ({@code request_id = "req-unit"}); {@code headers} em pares nome/valor. */
    static FakeServer.Reply fail(int status, String code, String... headers) {
        Map<String, Object> body = obj("code", (long) status, "data", null, "message", code, "error", code,
                "validation", obj(), "request_id", "req-unit");
        return new FakeServer.Reply(status, headers(headers), body);
    }

    /** Resposta crua: {@code body} JSON, texto ou {@code null}. */
    static FakeServer.Reply raw(int status, Object body, String... headers) {
        return new FakeServer.Reply(status, headers(headers), body);
    }

    static long millis(Duration d) {
        return d.toMillis();
    }
}
