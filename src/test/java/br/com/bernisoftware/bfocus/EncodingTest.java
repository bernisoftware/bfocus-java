package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.P;
import static br.com.bernisoftware.bfocus.TestSupport.arr;
import static br.com.bernisoftware.bfocus.TestSupport.obj;
import static br.com.bernisoftware.bfocus.TestSupport.ok;
import static br.com.bernisoftware.bfocus.TestSupport.pagination;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** Caminho, query e corpo: codificação, omitido x null, idempotência por chamada (BRIEF §3 e §10.4). */
class EncodingTest extends ServerTestBase {

    private static FakeServer.Reply[] okTimes(int n, Object data) {
        FakeServer.Reply[] out = new FakeServer.Reply[n];
        Arrays.fill(out, ok(data));
        return out;
    }

    private List<Object> bodies() {
        return requests().stream().map(FakeServer.Recorded::json).collect(Collectors.toList());
    }

    @Test
    void updatedSinceViraUtcComZ() {
        FakeServer.Reply empty = ok(arr(), pagination(1, 50, 0, 0));
        BfocusClient bf = client(empty, empty, empty, empty, empty, empty);
        bf.customers().list(CustomerListParams.builder().updatedSince(OffsetDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-3))).build());
        bf.customers().list(CustomerListParams.builder().updatedSince(Instant.parse("2026-09-01T03:00:00Z")).build());
        bf.customers().list(CustomerListParams.builder().updatedSince(ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("America/Sao_Paulo"))).build());
        bf.customers().list(CustomerListParams.builder().updatedSince(Instant.parse("2026-09-01T03:00:00.250Z")).build());
        bf.customers().list(CustomerListParams.builder().updatedSince("2026-09-01T00:00:00-03:00").build());   // string passa como veio
        bf.kb().articles().list(KbArticleListParams.builder().updatedSince(OffsetDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-3))).build());
        assertEquals(Arrays.asList("2026-09-01T03:00:00Z", "2026-09-01T03:00:00Z", "2026-09-01T03:00:00Z",
                "2026-09-01T03:00:00.250Z", "2026-09-01T00:00:00-03:00", "2026-09-01T03:00:00Z"),
                requests().stream().map(r -> r.queryMap().get("updated_since")).collect(Collectors.toList()));
    }

    @Test
    void booleanoEOmitidosNaQuery() {
        BfocusClient bf = client(ok(arr()), ok(arr()), ok(arr()));
        bf.products().list(false);
        bf.products().list();
        bf.products().list(true);
        assertEquals(Arrays.asList(Map.of("include_inactive", "false"), Map.of(), Map.of("include_inactive", "true")),
                requests().stream().map(FakeServer.Recorded::queryMap).collect(Collectors.toList()));
        assertNull(requests().get(1).rawQuery, "sem parâmetros = sem '?'");
    }

    @Test
    void queryComCaracteresEspeciais() {
        BfocusClient bf = client(ok(arr()));
        bf.kb().search("nota fiscal & cia = 100%", "erp", 3);
        FakeServer.Recorded r = requests().get(0);
        assertEquals(Map.of("q", "nota fiscal & cia = 100%", "product", "erp", "limit", "3"), r.queryMap());
        assertTrue(r.rawQuery.contains("%26") && r.rawQuery.contains("%3D") && r.rawQuery.contains("%25"), r.rawQuery);
    }

    @Test
    void omitidoXNull() {
        BfocusClient bf = client(okTimes(10, obj()));
        bf.customers().upsert("C1", CustomerUpsert.builder().build());
        bf.customers().upsert("C1", CustomerUpsert.builder().phone(null).name("Novo").build());
        bf.customers().upsert("C1", CustomerUpsert.builder().name("Novo").clear("phone").build());
        bf.customers().upsert("C1", CustomerUpsert.builder().customFields(Collections.emptyList()).build());
        bf.customers().upsert("C1", CustomerUpsert.builder().clear("customFields").build());
        bf.customers().upsert("C1", CustomerUpsert.builder().customFields(CustomFieldInput.of("k", null)).build());
        bf.products().upsert("erp", ProductUpsert.builder().isActive(false).sortOrder(null).build());
        bf.releaseNotes().upsert("erp", "1.0.0", ReleaseNoteUpsert.builder().descriptionMarkdown("x").requireAckExternal(true).build());
        bf.customers().contacts().upsert("C1", "CT", ContactUpsert.builder().isPrimary(false).build());
        bf.kb().articles().upsert("git:x", KbArticleUpsert.builder().title("T").product(null).clear("bodyHtml", "body_markdown").build());
        assertEquals(Arrays.asList(
                obj(),
                obj("phone", null, "name", "Novo"),
                obj("name", "Novo", "phone", null),
                obj("custom_fields", arr()),
                obj("custom_fields", null),
                obj("custom_fields", arr(obj("key", "k", "value", null))),
                obj("is_active", false, "sort_order", null),
                obj("description_markdown", "x", "require_ack_external", true),
                obj("is_primary", false),
                obj("title", "T", "product", null, "body_html", null, "body_markdown", null)), bodies());
        assertEquals("application/json", requests().get(0).header("content-type"), "{} ainda é corpo");
    }

    @Test
    void ultimaChamadaVenceEClearValidaONome() {
        assertEquals("{\"name\":\"b\"}", CustomerUpsert.builder().name("a").name("b").build().toJson());
        assertEquals("{\"name\":null}", CustomerUpsert.builder().name("a").clear("name").build().toJson());
        assertEquals("{\"description_html\":null}", ReleaseNoteUpsert.builder().clear("descriptionHtml").build().toJson());
        assertThrows(IllegalArgumentException.class, () -> CustomerUpsert.builder().clear("nao_existe"));
        assertThrows(IllegalArgumentException.class, () -> ReleaseNoteUpsert.builder().clear("publish"));
        assertEquals(CustomerUpsert.builder().name("x").build(), CustomerUpsert.builder().name("x").build());
        assertThrows(IllegalArgumentException.class, () -> CustomFieldInput.of("k", Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> CustomFieldInput.of("k", new Object()));
    }

    @Test
    void caminhoCodificadoPorSegmento() {
        BfocusClient bf = client(okTimes(6, obj()));
        bf.customers().get("ERP/1042 ç");
        bf.customers().contacts().delete("A B", "c?d");
        bf.releaseNotes().get("erp", "v2.3.0");
        bf.customers().get("...");
        bf.customers().contacts().delete("x y", "it's (1)*!");
        bf.kb().articles().get("notion:emitir-nfse#1");
        assertEquals(Arrays.asList(
                P + "/customers/ERP%2F1042%20%C3%A7",
                P + "/customers/A%20B/contacts/c%3Fd",
                P + "/products/erp/release-notes/v2.3.0",
                P + "/customers/...",
                P + "/customers/x%20y/contacts/it%27s%20%281%29%2A%21",
                P + "/kb/articles/notion%3Aemitir-nfse%231"), requests().stream().map(r -> r.path).collect(Collectors.toList()));
    }

    @Test
    void artigoRecusaBarraEIdsVazios() {
        BfocusClient bf = client();
        List<Executable> calls = new ArrayList<>(Arrays.<Executable>asList(
                () -> bf.kb().articles().get("docs/guia"),
                () -> bf.kb().articles().upsert("a/b", KbArticleUpsert.builder().title("x").build()),
                () -> bf.kb().articles().delete("a/b"),
                () -> bf.customers().get(""),
                () -> bf.customers().get("."),
                () -> bf.customers().delete(".."),
                () -> bf.customers().contacts().upsert("C1", "", ContactUpsert.builder().build()),
                () -> bf.customers().contacts().delete("C1", "."),
                () -> bf.customers().products().attach("C1", ".."),
                () -> bf.releaseNotes().get("erp", "."),
                () -> bf.releaseNotes().listAll(""),
                () -> bf.customers().interactions().listAll(".."),
                () -> bf.kb().articles().publish(".."),
                () -> bf.products().get(".."),
                () -> bf.aiAgents().get("")));
        for (Executable call : calls) {
            assertThrows(IllegalArgumentException.class, call);
        }
        assertThrows(NullPointerException.class, () -> bf.customers().get(null));
        assertEquals(0, requests().size(), "nada vai à rede");
    }

    @Test
    void idempotencyKeyPorChamadaESoEmEscrita() {
        BfocusClient bf = client(okTimes(5, obj()));
        bf.customers().upsert("C1", CustomerUpsert.builder().name("x").build(), RequestOptions.idempotencyKey("minha-chave"));
        bf.customers().get("C1", RequestOptions.idempotencyKey("ignorada-em-leitura"));
        bf.customers().products().attach("C1", "erp");
        bf.kb().articles().publish("a1");
        bf.kb().articles().publish("a1");
        List<FakeServer.Recorded> r = requests();
        assertEquals("minha-chave", r.get(0).header("idempotency-key"));
        assertNull(r.get(1).header("idempotency-key"));
        assertNull(r.get(1).header("content-type"));
        assertTrue(r.get(2).header("idempotency-key").matches("[0-9a-f-]{36}"), "PUT sem corpo continua sendo escrita");
        assertNull(r.get(2).header("content-type"));
        assertEquals(0, r.get(2).body.length);
        assertTrue(!r.get(3).header("idempotency-key").equals(r.get(4).header("idempotency-key")), "cada chamada lógica, uma chave");
        assertTrue(!r.get(3).header("x-request-id").equals(r.get(4).header("x-request-id")));
        assertThrows(IllegalArgumentException.class, () -> RequestOptions.idempotencyKey(""));
    }

    @Test
    void corpoUtf8() {
        BfocusClient bf = client(ok(obj()));
        bf.customers().interactions().create("C1", InteractionCreate.builder("Pedido faturado — ação ✓").isInternal(false).build());
        assertEquals(obj("content", "Pedido faturado — ação ✓", "is_internal", false), requests().get(0).json());
    }

    @Test
    void previewComESemHistorico() {
        BfocusClient bf = client(ok(obj("action", "answer")), ok(obj("action", "answer")));
        bf.aiAgents().preview("ag-1", "Oi", Arrays.asList(AiAgentPreviewTurn.customer("a"), AiAgentPreviewTurn.bot("b")));
        bf.aiAgents().preview("ag-1", "Oi");
        assertEquals(Arrays.asList(
                obj("message", "Oi", "history", arr(obj("role", "customer", "content", "a"), obj("role", "bot", "content", "b"))),
                obj("message", "Oi")), bodies());
        assertEquals(P + "/ai-agents/ag-1/preview", requests().get(0).path);
    }

    @Test
    void interacaoSoComConteudo() {
        BfocusClient bf = client(ok(obj()));
        bf.customers().interactions().create("C1", "Nota simples");
        assertEquals(obj("content", "Nota simples"), requests().get(0).json());
    }
}
