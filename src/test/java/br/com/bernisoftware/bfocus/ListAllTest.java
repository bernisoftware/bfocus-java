package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.P;
import static br.com.bernisoftware.bfocus.TestSupport.arr;
import static br.com.bernisoftware.bfocus.TestSupport.fail;
import static br.com.bernisoftware.bfocus.TestSupport.obj;
import static br.com.bernisoftware.bfocus.TestSupport.ok;
import static br.com.bernisoftware.bfocus.TestSupport.pagination;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** {@code listAll} e {@code Page}: paginação preguiçosa, {@code page_size} 100, parada (BRIEF §10.7). */
class ListAllTest extends ServerTestBase {

    private static Map<String, Object> customer(int n) {
        return obj("id", "id-" + n, "external_id", "C" + n, "name", "Cliente " + n);
    }

    private List<Map<String, String>> queries() {
        return requests().stream().map(FakeServer.Recorded::queryMap).collect(Collectors.toList());
    }

    @Test
    void percorreTodasAsPaginas() {
        BfocusClient bf = client(
                ok(arr(customer(1), customer(2)), pagination(1, 2, 5, 3)),
                ok(arr(customer(3), customer(4)), pagination(2, 2, 5, 3)),
                ok(arr(customer(5)), pagination(3, 2, 5, 3)));
        PagedIterable<Customer> all = bf.customers().listAll(CustomerListParams.builder().q("padaria").pageSize(2).build());
        assertEquals(0, requests().size(), "listAll é preguiçoso");

        List<String> got = new ArrayList<>();
        for (Customer c : all) {
            got.add(c.getExternalId());
        }
        assertEquals(Arrays.asList("C1", "C2", "C3", "C4", "C5"), got);
        assertEquals(Arrays.asList(
                Map.of("q", "padaria", "page", "1", "page_size", "2"),
                Map.of("q", "padaria", "page", "2", "page_size", "2"),
                Map.of("q", "padaria", "page", "3", "page_size", "2")), queries());
        // cada página é uma chamada lógica nova: X-Request-Id próprio
        assertEquals(3, new HashSet<>(requests().stream().map(r -> r.header("x-request-id")).collect(Collectors.toList())).size());
    }

    @Test
    void pageSizePadrao100() {
        BfocusClient bf = client(ok(arr(), pagination(1, 100, 0, 0)));
        assertFalse(bf.customers().listAll().iterator().hasNext());
        assertEquals(Arrays.asList(Map.of("page", "1", "page_size", "100")), queries());
    }

    @Test
    void paraEmPaginaVazia() {
        BfocusClient bf = client(ok(arr(), pagination(1, 2, 9, 5)));
        assertFalse(bf.customers().listAll(CustomerListParams.builder().pageSize(2).build()).iterator().hasNext());
        assertEquals(1, requests().size());
    }

    @Test
    void paraSemPaginacaoNaResposta() {
        BfocusClient bf = client(ok(arr(customer(1))));
        assertEquals(1, bf.customers().listAll().stream().count());
        assertEquals(1, requests().size(), "sem 'pagination' = página única");
    }

    @Test
    void outrosListAll() {
        Map<String, Object> one = pagination(1, 100, 1, 1);
        BfocusClient bf = client(ok(arr(obj("id", "i")), one), ok(arr(obj("id", "r")), one), ok(arr(obj("id", "a")), one));
        long n = bf.customers().interactions().listAll("ERP 1").stream().count()
                + bf.releaseNotes().listAll("erp", ReleaseNoteListParams.builder().published(false).build()).stream().count()
                + bf.kb().articles().listAll(KbArticleListParams.builder().product("erp").status("draft").build()).stream().count();
        assertEquals(3, n);
        List<FakeServer.Recorded> r = requests();
        assertEquals(P + "/customers/ERP%201/interactions", r.get(0).path);
        assertEquals(Map.of("page", "1", "page_size", "100"), r.get(0).queryMap());
        assertEquals(P + "/products/erp/release-notes", r.get(1).path);
        assertEquals(Map.of("published", "false", "page", "1", "page_size", "100"), r.get(1).queryMap());
        assertEquals(P + "/kb/articles", r.get(2).path);
        assertEquals(Map.of("product", "erp", "status", "draft", "page", "1", "page_size", "100"), r.get(2).queryMap());
    }

    @Test
    void streamPreguicoso() {
        BfocusClient bf = client(
                ok(arr(obj("id", "a1", "status", "draft"), obj("id", "a2", "status", "published")), pagination(1, 2, 3, 2)),
                ok(arr(obj("id", "a3", "status", "draft")), pagination(2, 2, 3, 2)));
        List<String> drafts = bf.kb().articles().listAll(KbArticleListParams.builder().pageSize(2).build()).stream()
                .filter(a -> "draft".equals(a.getStatus()))
                .map(KbArticleSummary::getId)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("a1", "a3"), drafts);
        assertEquals(2, requests().size());
    }

    @Test
    void cadaIteradorRecomecaDaPagina1() {
        BfocusClient bf = client(ok(arr(customer(1)), pagination(1, 100, 1, 1)), ok(arr(customer(1)), pagination(1, 100, 1, 1)));
        PagedIterable<Customer> all = bf.customers().listAll();
        assertEquals(1, all.stream().count());
        assertEquals(1, all.stream().count());
        assertEquals(Arrays.asList(Map.of("page", "1", "page_size", "100"), Map.of("page", "1", "page_size", "100")), queries());
    }

    @Test
    void erroNoMeioDaIteracao() {
        BfocusClient bf = client(ok(arr(customer(1)), pagination(1, 1, 2, 2)), fail(500, "INTERNAL_ERROR"));
        Iterator<Customer> it = bf.customers().listAll(CustomerListParams.builder().pageSize(1).build()).iterator();
        assertEquals("C1", it.next().getExternalId());
        ServerException e = assertThrows(ServerException.class, it::hasNext);
        assertEquals("INTERNAL_ERROR", e.getCode());
    }

    @Test
    void listAllRecusaPageEPageSizeInvalido() {
        BfocusClient bf = client();
        assertThrows(IllegalArgumentException.class, () -> bf.customers().listAll(CustomerListParams.builder().page(2).build()));
        assertThrows(IllegalArgumentException.class, () -> bf.customers().listAll(CustomerListParams.builder().pageSize(0).build()));
        assertEquals(0, requests().size());
    }

    @Test
    void pageEIteravel() {
        BfocusClient bf = client(ok(arr(customer(1)), pagination(1, 1, 2, 2)));
        Page<Customer> page = bf.customers().list(CustomerListParams.builder().pageSize(1).build());
        List<String> ids = new ArrayList<>();
        for (Customer c : page) {
            ids.add(c.getExternalId());
        }
        assertEquals(Arrays.asList("C1"), ids);
        assertEquals(1, page.getItems().size());
        assertEquals(Arrays.asList(1, 1, 2, 2), Arrays.asList(page.getPageNumber(), page.getPageSize(), page.getTotal(), page.getPages()));
        assertTrue(page.hasNextPage());
        assertThrows(UnsupportedOperationException.class, () -> page.getItems().clear());
        assertEquals(Map.of("page_size", "1"), requests().get(0).queryMap());
    }
}
