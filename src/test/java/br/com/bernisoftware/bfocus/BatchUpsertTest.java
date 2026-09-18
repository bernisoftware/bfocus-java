package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.P;
import static br.com.bernisoftware.bfocus.TestSupport.arr;
import static br.com.bernisoftware.bfocus.TestSupport.fail;
import static br.com.bernisoftware.bfocus.TestSupport.list;
import static br.com.bernisoftware.bfocus.TestSupport.map;
import static br.com.bernisoftware.bfocus.TestSupport.obj;
import static br.com.bernisoftware.bfocus.TestSupport.ok;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** {@code kb.articles.batchUpsert}: lotes de 100, agregação, chaves de idempotência por lote (BRIEF §6 e §10). */
class BatchUpsertTest extends ServerTestBase {

    private static List<KbBatchArticle> articles(int n) {
        return IntStream.range(0, n)
                .mapToObj(i -> KbBatchArticle.builder("git:a" + i).title("A" + i).build())
                .collect(Collectors.toList());
    }

    private static FakeServer.Reply chunkResponse(List<KbBatchArticle> chunk) {
        List<Object> results = new ArrayList<>();
        for (KbBatchArticle a : chunk) {
            results.add(obj("external_id", a.getExternalId(), "ok", true, "action", "created", "error", null, "article", null));
        }
        return ok(obj("results", results, "created", (long) chunk.size(), "updated", 0L, "unchanged", 0L, "failed", 0L));
    }

    private static List<FakeServer.Reply> chunked(List<KbBatchArticle> all) {
        List<FakeServer.Reply> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i += 100) {
            out.add(chunkResponse(all.subList(i, Math.min(i + 100, all.size()))));
        }
        return out;
    }

    private static List<Map<String, Object>> sentArticles(FakeServer.Recorded r) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list(map(r.json()).get("articles"))) {
            out.add(map(item));
        }
        return out;
    }

    @Test
    void divideEmLotesDe100EAgrega() {
        List<KbBatchArticle> all = articles(250);
        BfocusClient bf = client(chunked(all).toArray(new FakeServer.Reply[0]));

        KbBatchResult out = bf.kb().articles().batchUpsert(all);

        List<FakeServer.Recorded> requests = requests();
        assertEquals(3, requests.size());
        assertEquals(Arrays.asList(100, 100, 50), requests.stream().map(r -> sentArticles(r).size()).collect(Collectors.toList()));
        List<String> sentIds = new ArrayList<>();
        for (FakeServer.Recorded r : requests) {
            assertEquals("POST", r.method);
            assertEquals(P + "/kb/articles/batch", r.path);
            for (Map<String, Object> a : sentArticles(r)) {
                sentIds.add((String) a.get("external_id"));
            }
        }
        List<String> expectedIds = all.stream().map(KbBatchArticle::getExternalId).collect(Collectors.toList());
        assertEquals(expectedIds, sentIds, "ordem preservada entre os lotes");
        // cada lote é uma chamada lógica: ids próprios
        assertEquals(3, new HashSet<>(requests.stream().map(r -> r.header("idempotency-key")).collect(Collectors.toList())).size());
        assertEquals(3, new HashSet<>(requests.stream().map(r -> r.header("x-request-id")).collect(Collectors.toList())).size());

        assertEquals(expectedIds, out.getResults().stream().map(KbBatchItemResult::getExternalId).collect(Collectors.toList()));
        assertEquals(Arrays.asList(250, 0, 0, 0), Arrays.asList(out.getCreated(), out.getUpdated(), out.getUnchanged(), out.getFailed()));
    }

    @Test
    void exatamente100VaiEmUmLote() {
        List<KbBatchArticle> all = articles(100);
        BfocusClient bf = client(chunkResponse(all));
        bf.kb().articles().batchUpsert(all);
        assertEquals(1, requests().size());
    }

    @Test
    void idempotencyKeyDoUsuarioPorLote() {
        List<KbBatchArticle> a150 = articles(150);
        BfocusClient bf = client(chunked(a150).toArray(new FakeServer.Reply[0]));
        bf.kb().articles().batchUpsert(a150, RequestOptions.idempotencyKey("sync-42"));
        assertEquals(Arrays.asList("sync-42", "sync-42:2"), idempotencyKeys());

        List<KbBatchArticle> a250 = articles(250);
        bf = client(chunked(a250).toArray(new FakeServer.Reply[0]));
        bf.kb().articles().batchUpsert(a250, RequestOptions.idempotencyKey("k"));
        assertEquals(Arrays.asList("k", "k:2", "k:3"), idempotencyKeys());

        List<KbBatchArticle> one = articles(1);
        bf = client(chunkResponse(one));
        bf.kb().articles().batchUpsert(one, RequestOptions.idempotencyKey("sync-43"));
        assertEquals(Arrays.asList("sync-43"), idempotencyKeys());
    }

    private List<String> idempotencyKeys() {
        return requests().stream().map(r -> r.header("idempotency-key")).collect(Collectors.toList());
    }

    @Test
    void somaContadoresMistos() {
        List<Object> hundred = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            hundred.add(obj("external_id", "x" + i, "ok", true));
        }
        FakeServer.Reply first = ok(obj("results", hundred, "created", 60L, "updated", 30L, "unchanged", 9L, "failed", 1L));
        FakeServer.Reply second = ok(obj("results", arr(obj("external_id", "y", "ok", false, "error", "KB_ARTICLE_TITLE_REQUIRED")),
                "created", 0L, "updated", 0L, "unchanged", 0L, "failed", 1L));
        KbBatchResult out = client(first, second).kb().articles().batchUpsert(articles(101));
        assertEquals(101, out.getResults().size());
        assertEquals(Arrays.asList(60, 30, 9, 2), Arrays.asList(out.getCreated(), out.getUpdated(), out.getUnchanged(), out.getFailed()));
        assertEquals("KB_ARTICLE_TITLE_REQUIRED", out.getResults().get(100).getError());
    }

    @Test
    void vazioNaoChamaAApi() {
        KbBatchResult out = client().kb().articles().batchUpsert(new ArrayList<>());
        assertEquals(0, requests().size());
        assertTrue(out.getResults().isEmpty());
        assertEquals(Arrays.asList(0, 0, 0, 0), Arrays.asList(out.getCreated(), out.getUpdated(), out.getUnchanged(), out.getFailed()));
        assertEquals(obj("results", arr(), "created", 0L, "updated", 0L, "unchanged", 0L, "failed", 0L), out.toMap());
    }

    @Test
    void validaExternalIdAntesDeEnviar() {
        BfocusClient bf = client();
        assertThrows(IllegalArgumentException.class, () -> KbBatchArticle.builder("docs/guia"));
        assertThrows(IllegalArgumentException.class, () -> KbBatchArticle.builder(""));
        assertThrows(NullPointerException.class, () -> KbBatchArticle.builder(null));
        assertThrows(IllegalArgumentException.class, () -> KbBatchArticle.builder("git:x").clear("external_id"));
        List<KbBatchArticle> withNull = new ArrayList<>(articles(150));
        withNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> bf.kb().articles().batchUpsert(withNull));
        assertEquals(0, requests().size(), "nada vai à rede antes de validar tudo");
    }

    @Test
    void productNullExplicitoEPreservado() {
        KbBatchArticle global = KbBatchArticle.builder("git:global").title("G").product(null).build();
        BfocusClient bf = client(chunkResponse(Arrays.asList(global)));
        bf.kb().articles().batchUpsert(Arrays.asList(global));
        assertEquals(obj("articles", arr(obj("external_id", "git:global", "title", "G", "product", null))), requests().get(0).json());
    }

    @Test
    void erroHttpInterrompeOsLotesSeguintes() {
        List<KbBatchArticle> all = articles(250);
        BfocusClient bf = client(chunkResponse(all.subList(0, 100)), fail(500, "INTERNAL_ERROR"));
        assertThrows(ServerException.class, () -> bf.kb().articles().batchUpsert(all));
        assertEquals(2, requests().size(), "o 3º lote não é enviado depois do erro");
    }
}
