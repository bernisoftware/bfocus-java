package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.arr;
import static br.com.bernisoftware.bfocus.TestSupport.fail;
import static br.com.bernisoftware.bfocus.TestSupport.obj;
import static br.com.bernisoftware.bfocus.TestSupport.ok;
import static br.com.bernisoftware.bfocus.TestSupport.raw;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Mapeamento de erros (BRIEF §4 e §10.1–10.2). */
class ErrorsTest extends ServerTestBase {

    @Test
    void classePorStatus() {
        Map<Integer, Class<? extends BfocusException>> want = new LinkedHashMap<>();
        want.put(400, BfocusException.class);
        want.put(401, AuthenticationException.class);
        want.put(403, PermissionDeniedException.class);
        want.put(404, NotFoundException.class);
        want.put(409, ConflictException.class);
        want.put(418, BfocusException.class);
        want.put(422, ValidationException.class);
        want.put(429, RateLimitException.class);
        want.put(500, ServerException.class);
        want.put(503, ServerException.class);
        for (Map.Entry<Integer, Class<? extends BfocusException>> e : want.entrySet()) {
            BfocusClient bf = client(b -> b.maxRetries(0), fail(e.getKey(), "CODE_" + e.getKey()));
            BfocusException err = assertThrows(BfocusException.class, () -> bf.products().get("erp"));
            assertSame(e.getValue(), err.getClass(), "status " + e.getKey());
            assertEquals("CODE_" + e.getKey(), err.getCode());
            assertEquals((int) e.getKey(), err.getStatus());
            assertEquals("req-unit", err.getRequestId());
        }
    }

    @Test
    void mensagemECampos() {
        BfocusClient bf = client(raw(403, obj("code", 403L, "data", null, "message", "INTEGRATION_SCOPE_MISSING",
                "error", "INTEGRATION_SCOPE_MISSING", "validation", obj(), "request_id", "req-9"), "X-Required-Scope", "kb:write"));
        PermissionDeniedException err = assertThrows(PermissionDeniedException.class, () -> bf.kb().articles().publish("git:x"));
        assertEquals("kb:write", err.getRequiredScope());
        assertTrue(err.getMessage().contains("kb:write"), err.getMessage());
        assertTrue(err.getMessage().contains("req-9"), err.getMessage());
        assertTrue(err.getMessage().startsWith("INTEGRATION_SCOPE_MISSING"), err.getMessage());
        assertTrue(err.getMessage().contains("HTTP 403"), err.getMessage());
        assertNull(err.getRetryAfter());
        assertTrue(err.getValidation().isEmpty());
    }

    @Test
    void validacaoPorCampo() {
        BfocusClient bf = client(raw(422, obj("error", "VALIDATION_ERROR", "message", "Dados inválidos",
                "validation", obj("email", "value is not a valid email address", "name", "field required"))));
        ValidationException err = assertThrows(ValidationException.class,
                () -> bf.customers().contacts().upsert("C1", "CT", ContactUpsert.builder().email("x").build()));
        assertEquals(Map.of("email", "value is not a valid email address", "name", "field required"), err.getValidation());
        assertTrue(err.getMessage().contains("email: value is not a valid email address"), err.getMessage());
        assertThrows(UnsupportedOperationException.class, () -> err.getValidation().clear());
    }

    @Test
    void moduloNaoContratado() {
        BfocusClient bf = client(raw(403, obj("error", "MODULE_NOT_CONTRACTED", "request_id", "r"), "X-Required-Module", "atendimento"));
        PermissionDeniedException err = assertThrows(PermissionDeniedException.class, () -> bf.aiAgents().list());
        assertEquals("MODULE_NOT_CONTRACTED", err.getCode());
        assertTrue(err.getMessage().contains("atendimento"), err.getMessage());
    }

    @Test
    void doisXxSemEnvelopeEInvalidResponse() {
        FakeServer.Reply[] replies = {
            raw(200, "<html>proxy</html>"),
            raw(200, null),                                   // corpo vazio
            raw(200, obj("id", "sem envelope")),
            raw(201, arr(1L, 2L), "X-Request-Id", "req-do-header"),
            raw(200, obj("code", 200L, "data", obj("name", 123L), "message", "ok")),   // campo conhecido com tipo errado
        };
        BfocusClient bf = client(replies);
        List<BfocusException> got = new ArrayList<>();
        for (int i = 0; i < replies.length; i++) {
            got.add(assertThrows(BfocusException.class, () -> bf.products().get("erp")));
        }
        List<FakeServer.Recorded> r = requests();
        for (int i = 0; i < got.size(); i++) {
            BfocusException err = got.get(i);
            assertSame(BfocusException.class, err.getClass(), "resposta " + i);
            assertEquals("INVALID_RESPONSE", err.getCode());
            assertTrue(err.getMessage().contains("INVALID_RESPONSE"), err.getMessage());
            assertEquals(i == 3 ? 201 : 200, err.getStatus());
            assertEquals(i == 3 ? "req-do-header" : r.get(i).header("x-request-id"), err.getRequestId(), "request_id " + i);
        }
    }

    @Test
    void requestIdCaiParaOEnviado() {
        BfocusClient bf = client(raw(404, "Not Found"), raw(409, obj("code", 409L, "error", "X")));
        for (int i = 0; i < 2; i++) {
            BfocusException err = assertThrows(BfocusException.class, () -> bf.products().get("erp"));
            assertEquals(requests().get(requests().size() - 1).header("x-request-id"), err.getRequestId());
        }
    }

    @Test
    void codeCaiParaMessageEDepoisParaHttpStatus() {
        BfocusClient bf = client(raw(404, obj("code", 404L, "message", "TENANT_NOT_FOUND")), raw(404, obj()),
                raw(418, "<html>teapot</html>", "X-Request-Id", "rid-header"));
        NotFoundException e1 = assertThrows(NotFoundException.class, () -> bf.products().list());
        assertEquals("TENANT_NOT_FOUND", e1.getCode());
        assertTrue(e1.getValidation().isEmpty());
        assertEquals("HTTP_404", assertThrows(NotFoundException.class, () -> bf.products().list()).getCode());
        BfocusException e3 = assertThrows(BfocusException.class, () -> bf.products().list());
        assertEquals("HTTP_418", e3.getCode());
        assertEquals("rid-header", e3.getRequestId());
    }

    @Test
    void camposDesconhecidosSaoPreservados() {
        BfocusClient bf = client(ok(obj("id", "1", "external_id", "C1", "campo_novo", obj("a", 1L))));
        Customer c = bf.customers().get("C1");
        assertEquals("1", c.getId());
        assertEquals(Map.of("a", 1L), c.getRaw("campo_novo"));
        assertEquals(Map.of("a", 1L), c.toMap().get("campo_novo"));
        assertTrue(c.toJson().contains("\"campo_novo\":{\"a\":1}"), c.toJson());
    }
}
