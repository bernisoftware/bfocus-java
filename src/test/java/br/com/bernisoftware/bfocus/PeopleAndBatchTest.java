package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.P;
import static br.com.bernisoftware.bfocus.TestSupport.arr;
import static br.com.bernisoftware.bfocus.TestSupport.list;
import static br.com.bernisoftware.bfocus.TestSupport.map;
import static br.com.bernisoftware.bfocus.TestSupport.obj;
import static br.com.bernisoftware.bfocus.TestSupport.ok;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** {@code customers().batch}, {@code people()}, identificadores extras e a identidade v2 do widget (SDK 0.2.0). */
class PeopleAndBatchTest extends ServerTestBase {

    private static List<CustomerBatchItem> customers(int n) {
        return IntStream.range(0, n)
                .mapToObj(i -> CustomerBatchItem.of("erp-" + i, CustomerUpsert.builder().name("C" + i).build()))
                .collect(Collectors.toList());
    }

    private static List<PersonBatchItem> people(int n) {
        return IntStream.range(0, n)
                .mapToObj(i -> PersonBatchItem.of("erp-1", "app-" + i, PersonUpsert.builder().name("P" + i).build()))
                .collect(Collectors.toList());
    }

    private static FakeServer.Reply batchReply(int n) {
        List<Object> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            results.add(obj("index", (long) i, "status", "created", "external_id", "x" + i, "merged_into", null, "error", null, "code", null));
        }
        return ok(obj("results", results, "summary", obj("created", (long) n, "updated", 0L, "unchanged", 0L, "error", 0L)));
    }

    @Test
    void customersBatchAcimaDe500RecusaSemRequisicao() {
        BfocusClient bf = client();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> bf.customers().batch(customers(501)));
        assertEquals("customers().batch aceita até 500 itens por chamada (recebeu 501); divida em lotes de 500.", e.getMessage());
        assertEquals(0, requests().size());
        assertEquals(500, BfocusClient.BATCH_MAX);
    }

    @Test
    void peopleBatchAcimaDe500RecusaSemRequisicao() {
        BfocusClient bf = client();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> bf.people().batch(people(501)));
        assertTrue(e.getMessage().startsWith("people().batch aceita até 500 itens"), e.getMessage());
        assertEquals(0, requests().size());
    }

    @Test
    void customersBatchCom500VaiNumaRequisicao() {
        BfocusClient bf = client(batchReply(500));
        BatchResult out = bf.customers().batch(customers(500), RequestOptions.idempotencyKey("carga-1"));
        assertEquals(1, requests().size());
        FakeServer.Recorded r = requests().get(0);
        assertEquals("POST", r.method);
        assertEquals(P + "/customers/batch", r.path);
        assertEquals("carga-1", r.header("idempotency-key"));
        List<Object> items = list(map(r.json()).get("items"));
        assertEquals(500, items.size());
        assertEquals(obj("external_id", "erp-0", "name", "C0"), items.get(0));
        assertEquals(500, out.getResults().size());
        assertEquals(499, out.getResults().get(499).getIndex());
        assertEquals(500, out.getSummary().getCreated());
    }

    @Test
    void peopleBatchCom500VaiNumaRequisicaoNoFormatoDoFio() {
        BfocusClient bf = client(batchReply(500));
        bf.people().batch(people(500));
        assertEquals(1, requests().size());
        FakeServer.Recorded r = requests().get(0);
        assertEquals(P + "/people/batch", r.path);
        List<Object> items = list(map(r.json()).get("items"));
        assertEquals(500, items.size());
        assertEquals(obj("customer_external_id", "erp-1", "person", obj("external_id", "app-0", "name", "P0")), items.get(0));
    }

    @Test
    void loteVazioNaoChamaAApi() {
        BfocusClient bf = client();
        BatchResult c = bf.customers().batch(new ArrayList<>());
        BatchResult p = bf.people().batch(new ArrayList<>());
        assertEquals(0, requests().size());
        Object zero = obj("results", arr(), "summary", obj("created", 0L, "updated", 0L, "unchanged", 0L, "error", 0L));
        assertEquals(zero, c.toMap());
        assertEquals(zero, p.toMap());
        assertTrue(c.getResults().isEmpty());
        assertEquals(0, c.getSummary().getError());
    }

    @Test
    void itemNullOuIdVazioRecusaAntesDeEnviar() {
        BfocusClient bf = client();
        List<CustomerBatchItem> withNull = new ArrayList<>(customers(3));
        withNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> bf.customers().batch(withNull));
        assertThrows(NullPointerException.class, () -> bf.customers().batch(null));
        assertThrows(IllegalArgumentException.class, () -> CustomerBatchItem.of("", CustomerUpsert.builder().build()));
        assertThrows(IllegalArgumentException.class, () -> PersonBatchItem.of("erp-1", "", PersonUpsert.builder().build()));
        assertThrows(IllegalArgumentException.class, () -> PersonBatchItem.of("", "app-1", PersonUpsert.builder().build()));
        assertEquals(0, requests().size());
    }

    @Test
    void resultadoDoItemComErro() {
        BfocusClient bf = client(ok(obj("results", arr(
                obj("index", 0L, "status", "updated", "external_id", "erp-1", "merged_into", "erp-9", "error", null, "code", null),
                obj("index", 1L, "status", "error", "external_id", "erp-2", "merged_into", null, "error", "NAME_REQUIRED", "code", 422L)),
                "summary", obj("created", 0L, "updated", 1L, "unchanged", 0L, "error", 1L))));
        BatchResult out = bf.customers().batch(customers(2));
        BatchItemResult first = out.getResults().get(0);
        BatchItemResult second = out.getResults().get(1);
        assertEquals("erp-9", first.getMergedInto());
        assertNull(first.getCode());
        assertFalse(first.isError());
        assertTrue(second.isError());
        assertEquals("NAME_REQUIRED", second.getError());
        assertEquals(Integer.valueOf(422), second.getCode());
        assertEquals(1, out.getSummary().getError());
    }

    @Test
    void upsertDePessoaMandaSoOQueVeio() {
        BfocusClient bf = client(ok(obj("external_id", "app-77", "name", "Paula", "email", null, "phone", null, "role", null,
                "access", true, "is_primary", false, "customer_external_id", "erp-1042", "status", "unchanged")));
        PersonUpsertResult p = bf.people().upsert("erp-1042", "app-77",
                PersonUpsert.builder().access(true).extraPhones("+55 11 3333-4444").clear("role").build());
        FakeServer.Recorded r = requests().get(0);
        assertEquals("PUT", r.method);
        assertEquals(P + "/customers/erp-1042/people/app-77", r.path);
        assertEquals(obj("person", obj("access", true, "extra_phones", arr("+55 11 3333-4444"), "role", null)), r.json());
        assertEquals("unchanged", p.getStatus());
        assertTrue(p.hasAccess());
        assertEquals("erp-1042", p.getCustomerExternalId());
        assertThrows(IllegalArgumentException.class, () -> PersonUpsert.builder().clear("nope"));
    }

    @Test
    void identificadorComESemRotulo() {
        Object data = obj("external_id", "app-77", "identifiers", arr(obj("external_id", "crm-p5", "label", "CRM", "source", "api")));
        BfocusClient bf = client(ok(data), ok(data));
        PersonIdentifiers ids = bf.people().identifiers().add("app-77", "crm-p5", "CRM");
        bf.people().identifiers().add("app-77", "crm-p5");
        assertEquals(obj("label", "CRM"), requests().get(0).json());
        assertEquals(0, requests().get(1).body.length, "sem rótulo, sem corpo");
        assertEquals(P + "/people/app-77/identifiers/crm-p5", requests().get(1).path);
        assertEquals("CRM", ids.getIdentifiers().get(0).getLabel());
        assertEquals("api", ids.getIdentifiers().get(0).getSource());
        assertThrows(IllegalArgumentException.class, () -> bf.customers().identifiers().add("erp-1", ".."));
    }

    @Test
    void assinaturaV2() {
        String secret = "bf_whs_x";
        long before = Instant.now().getEpochSecond();
        String sig = WidgetIdentity.signV2(secret, "app-77", "erp-1042");
        assertTrue(sig.matches("^v2\\.\\d+\\.[0-9a-f]{64}$"), sig);
        long ts = Long.parseLong(sig.split("\\.")[1]);
        assertTrue(Math.abs(ts - before) <= 5, "ts a ±5 s de agora: " + ts);
        assertTrue(BfocusClient.signWidgetIdentityV2(secret, "app-77", "erp-1042").startsWith("v2."));

        // ':' no id do usuário é recusado (é o separador).
        assertThrows(IllegalArgumentException.class, () -> WidgetIdentity.signV2(secret, "app:77", "erp-1042"));
        assertThrows(IllegalArgumentException.class, () -> WidgetIdentity.signV2("", "app-77", "erp-1042"));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetIdentity.signV2(secret, "app-77", "erp-1042", Instant.ofEpochSecond(-1)));
        assertThrows(NullPointerException.class, () -> WidgetIdentity.signV2(secret, "app-77", "erp-1042", null));
        assertTrue(WidgetIdentity.signV2(secret, "u", "c", Instant.EPOCH).startsWith("v2.0."));
        // v1 continua igual (sem restrição de ':')
        assertEquals(64, WidgetIdentity.sign(secret, "app:77", "erp-1042").length());
    }
}
