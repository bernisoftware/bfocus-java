package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.loadCases;
import static br.com.bernisoftware.bfocus.TestSupport.map;
import static br.com.bernisoftware.bfocus.TestSupport.mapList;
import static br.com.bernisoftware.bfocus.TestSupport.obj;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Getters tipados dos modelos, decodificando os resultados esperados dos casos de conformidade (a conformidade
 * compara o JSON cru; aqui confere o mapeamento campo → getter).
 */
class ModelsTest {
    private static final Map<String, Object> CASES = loadCases();
    private static final OffsetDateTime NOON = OffsetDateTime.of(2026, 9, 13, 12, 0, 0, 0, ZoneOffset.UTC);

    private static Object result(String id) {
        for (Map<String, Object> c : mapList(CASES.get("cases"))) {
            if (id.equals(c.get("id"))) {
                return map(c.get("expect")).get("result");
            }
        }
        throw new AssertionError("caso não encontrado: " + id);
    }

    @Test
    void cliente() {
        Customer c = Customer.from(result("customers.get/ok"));
        assertEquals("00000000-0000-4000-8000-000000000001", c.getId());
        assertEquals("ERP 1042", c.getExternalId());
        assertEquals("Padaria Estrela", c.getName());
        assertEquals("contato@padaria.example", c.getEmail());
        assertNull(c.getDocument());
        assertNull(c.getPhone());
        assertTrue(c.isActive());
        assertEquals(NOON, c.getCreatedAt());
        assertEquals(1, c.getCustomFields().size());
        CustomField f = c.getCustomFields().get(0);
        assertEquals(Arrays.asList("plano", "Plano", "text", "ouro", "interno"),
                Arrays.asList(f.getKey(), f.getLabel(), f.getType(), f.getValue(), f.getVisibility()));
        assertTrue(f.getExtras().isEmpty());
        assertEquals(Customer.from(result("customers.get/ok")), c);
        assertEquals(Customer.from(result("customers.get/ok")).hashCode(), c.hashCode());
    }

    @Test
    void contatoProdutoVinculadoEInteracao() {
        Contact ct = Contact.from(((List<?>) result("customers.contacts.list/ok")).get(0));
        assertEquals("CT-1", ct.getExternalId());
        assertEquals("Financeiro", ct.getRole());
        assertTrue(ct.isPrimary());
        ProductRef ref = ProductRef.from(((List<?>) result("customers.products.list/ok")).get(0));
        assertEquals("erp-cloud", ref.getSlug());
        assertTrue(ref.isActive());
        Interaction i = Interaction.from(result("customers.interactions.create/ok"));
        assertEquals("<p>Pedido 1042 faturado.</p>", i.getContent());
        assertTrue(i.isInternal());
        assertEquals("human", i.getAuthorKind());
        assertEquals("Carla Lima", i.getAuthorName());
        assertTrue(DeleteResult.from(result("customers.delete/ok")).isDeleted());
    }

    @Test
    void produtoEReleaseNote() {
        Product p = Product.from(result("products.get/ok"));
        assertEquals("erp-cloud", p.getSlug());
        assertEquals("#6366F1", p.getColor());
        assertNull(p.getIcon());
        assertEquals(0, p.getSortOrder());
        assertEquals("2.3.0", p.getCurrentVersion());
        assertEquals("autonomous", p.getAiLevel());
        ReleaseNote n = ReleaseNote.from(result("release_notes.get/ok"));
        assertEquals("2.3.0", n.getVersion());
        assertEquals("external", n.getAudience());
        assertTrue(n.isPublished());
        assertFalse(n.isRequireAckInternal());
        assertEquals(OffsetDateTime.of(2026, 9, 13, 15, 30, 0, 0, ZoneOffset.UTC), n.getPublishedAt());
    }

    @Test
    void baseDeConhecimento() {
        KbArticle a = KbArticle.from(result("kb.articles.get/ok"));
        assertEquals("notion:emitir-nfse", a.getExternalId());
        assertNull(a.getProduct(), "artigo global");
        assertEquals("published", a.getStatus());
        assertTrue(a.getBodyHtml().startsWith("<h1>Passo a passo</h1>"));
        KbSearchHit hit = KbSearchHit.from(((List<?>) result("kb.search/ok")).get(0));
        assertEquals("Como emitir NFS-e", hit.getTitle());
        KbBatchResult batch = KbBatchResult.from(result("kb.articles.batch_upsert/partial_failure"));
        assertEquals(Arrays.asList(1, 0, 0, 1), Arrays.asList(batch.getCreated(), batch.getUpdated(), batch.getUnchanged(), batch.getFailed()));
        KbBatchItemResult ok = batch.getResults().get(0);
        assertTrue(ok.isOk());
        assertEquals("created", ok.getAction());
        assertEquals("Instalação", ok.getArticle().getTitle());
        KbBatchItemResult bad = batch.getResults().get(1);
        assertFalse(bad.isOk());
        assertEquals("KB_ARTICLE_TITLE_REQUIRED", bad.getError());
        assertNull(bad.getArticle());
        assertNull(bad.getAction());
    }

    @Test
    void agentesDeIa() {
        AiAgent agent = AiAgent.from(result("ai_agents.get/ok"));
        assertEquals("Bia", agent.getName());
        assertTrue(agent.isActive());
        assertEquals("erp-cloud", agent.getProduct().getSlug());
        assertNull(agent.getAvatarUrl());
        AiAgentPreview p = AiAgentPreview.from(result("ai_agents.preview/ok"));
        assertEquals("answer", p.getAction());
        assertFalse(p.isEscalated());
        assertEquals(0.82, p.getConfidence(), 1e-9);
        assertEquals("nfse", p.getTopic());
        assertEquals(Collections.emptyList(), p.getGuards());
        assertEquals(Arrays.asList(1L), p.getCitations());
        assertEquals("kb_article", p.getSources().get(0).get("type"));
        assertTrue(p.getCollected().isEmpty());
        assertTrue(p.getExtras().isEmpty());
        AiAgentPreview withExtra = AiAgentPreview.from(obj("action", "handoff", "debug", obj("ms", 12L)));
        assertEquals(Map.of("ms", 12L), withExtra.getExtra("debug"));
    }

    @Test
    void datasSemOffsetSaoUtcEComFracao() {
        assertEquals(NOON, Customer.from(obj("created_at", "2026-09-13T12:00:00")).getCreatedAt());
        assertEquals(NOON.plusNanos(123_456_000), Customer.from(obj("created_at", "2026-09-13 12:00:00.123456")).getCreatedAt());
        assertEquals(OffsetDateTime.of(2026, 9, 13, 9, 0, 0, 0, ZoneOffset.ofHours(-3)),
                Customer.from(obj("created_at", "2026-09-13T09:00:00-03:00")).getCreatedAt());
    }

    @Test
    void camposDesconhecidosIgnoradosETipoErradoRecusado() {
        Customer c = Customer.from(obj("id", "1", "novo", Arrays.asList(1L, 2L)));
        assertEquals("1", c.getId());
        assertNotNull(c.getCustomFields());
        assertEquals(Arrays.asList(1L, 2L), c.getRaw("novo"));
        assertEquals("x", CustomField.from(obj("key", "k", "novo", "x")).getExtra("novo"));
        assertThrows(InvalidDataException.class, () -> Product.from(obj("name", 123L)));
        assertThrows(InvalidDataException.class, () -> Product.from(obj("sort_order", new BigDecimal("1.5"))));
        assertThrows(InvalidDataException.class, () -> Customer.from(obj("created_at", "ontem")));
        assertThrows(InvalidDataException.class, () -> Customer.from("não é objeto"));
        assertThrows(UnsupportedOperationException.class, () -> c.toMap().put("x", 1));
    }
}
