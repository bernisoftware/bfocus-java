package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.list;
import static br.com.bernisoftware.bfocus.TestSupport.loadCases;
import static br.com.bernisoftware.bfocus.TestSupport.map;
import static br.com.bernisoftware.bfocus.TestSupport.mapList;
import static br.com.bernisoftware.bfocus.TestSupport.projectDir;
import static br.com.bernisoftware.bfocus.TestSupport.stringList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.bernisoftware.bfocus.internal.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Conformidade: roda TODOS os casos de {@code cases.json} (BRIEF §7), um teste dinâmico por caso.
 *
 * <p>Para cada caso, o {@link FakeServer} confere cada troca — método, caminho exatamente como codificado, query,
 * corpo JSON (igualdade profunda), {@code Authorization}, {@code Accept}, {@code X-Bfocus-Client},
 * {@code User-Agent}, {@code X-Request-Id}, {@code Idempotency-Key} — e, entre trocas, que os ids se repetem na nova
 * tentativa ({@code retry: true}) e mudam na chamada lógica nova ({@code retry: false}). Depois compara o retorno
 * (normalizado para o JSON neutro) ou o erro com {@code expect}.
 *
 * <p>Endpoint novo sem método na SDK quebra este teste: todo {@code op} dos casos precisa estar em {@link #OPS} (a
 * menos que esteja em {@code sdk_excluded_ops}), e todo argumento do caso precisa ser usado pela chamada.
 */
class ConformanceTest {
    private static final Map<String, Object> CASES = loadCases();
    private static final String API_KEY = (String) CASES.get("api_key");
    private static final Set<String> EXCLUDED = new TreeSet<>(stringList(CASES.get("sdk_excluded_ops")));
    private static final Pattern CLIENT_RE = Pattern.compile("^bfocus-java/" + Pattern.quote(Version.VERSION) + "$");
    private static final Pattern REQUEST_ID_RE = Pattern.compile("^[0-9a-f]{32}$");
    private static final Pattern SECONDS_RE = Pattern.compile("\\d+(\\.\\d+)?");
    private static final Set<String> WRITES = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String SENT = "$sent";   // em expect.error.request_id: "o X-Request-Id que a SDK enviou"
    private static final Map<String, Class<? extends BfocusException>> ERROR_TYPES = errorTypes();

    /** op (neutro, de {@code cases.json}) → chamada idiomática da SDK. */
    static final Map<String, BiFunction<BfocusClient, Args, Object>> OPS = ops();

    private static FakeServer server;

    @BeforeAll
    static void startServer() throws IOException {
        server = new FakeServer();
    }

    @AfterAll
    static void stopServer() {
        server.close();
    }

    // ── tabela op → chamada ──────────────────────────────────────────────────────────────────────────────

    private static Map<String, BiFunction<BfocusClient, Args, Object>> ops() {
        Map<String, BiFunction<BfocusClient, Args, Object>> m = new LinkedHashMap<>();
        // Clientes
        m.put("customers.upsert", (bf, a) -> bf.customers().upsert(a.str("external_id"), customerUpsert(a)));
        m.put("customers.get", (bf, a) -> bf.customers().get(a.str("external_id")));
        m.put("customers.list", (bf, a) -> bf.customers().list(customerListParams(a, true)));
        m.put("customers.list_all", (bf, a) -> bf.customers().listAll(customerListParams(a, false)));
        m.put("customers.delete", (bf, a) -> bf.customers().delete(a.str("external_id")));
        m.put("customers.contacts.list", (bf, a) -> bf.customers().contacts().list(a.str("external_id")));
        m.put("customers.contacts.upsert", (bf, a) -> bf.customers().contacts()
                .upsert(a.str("external_id"), a.str("contact_external_id"), contactUpsert(a)));
        m.put("customers.contacts.delete", (bf, a) -> bf.customers().contacts()
                .delete(a.str("external_id"), a.str("contact_external_id")));
        m.put("customers.products.list", (bf, a) -> bf.customers().products().list(a.str("external_id")));
        m.put("customers.products.attach", (bf, a) -> bf.customers().products()
                .attach(a.str("external_id"), a.str("product_slug")));
        m.put("customers.products.detach", (bf, a) -> bf.customers().products()
                .detach(a.str("external_id"), a.str("product_slug")));
        m.put("customers.interactions.list", (bf, a) -> bf.customers().interactions()
                .list(a.str("external_id"), interactionListParams(a, true)));
        m.put("customers.interactions.list_all", (bf, a) -> bf.customers().interactions()
                .listAll(a.str("external_id"), interactionListParams(a, false)));
        m.put("customers.interactions.create", (bf, a) -> {
            InteractionCreate.Builder b = InteractionCreate.builder(a.str("content"));
            if (a.has("is_internal")) {
                b.isInternal(a.bool("is_internal"));
            }
            if (a.has("author_email")) {
                b.authorEmail(a.str("author_email"));
            }
            return bf.customers().interactions().create(a.str("external_id"), b.build());
        });
        m.put("customers.batch", (bf, a) -> bf.customers().batch(customerBatchItems(a)));
        m.put("customers.identifiers.add", (bf, a) -> bf.customers().identifiers()
                .add(a.str("external_id"), a.str("extra_id"), a.str("label")));
        m.put("customers.identifiers.remove", (bf, a) -> bf.customers().identifiers()
                .remove(a.str("external_id"), a.str("extra_id")));
        // Pessoas
        m.put("people.upsert", (bf, a) -> bf.people()
                .upsert(a.str("customer_external_id"), a.str("person_external_id"), personUpsert(a)));
        m.put("people.list", (bf, a) -> bf.people().list(a.str("customer_external_id")));
        m.put("people.delete", (bf, a) -> bf.people().delete(a.str("customer_external_id"), a.str("person_external_id")));
        m.put("people.batch", (bf, a) -> bf.people().batch(personBatchItems(a)));
        m.put("people.identifiers.list", (bf, a) -> bf.people().identifiers()
                .list(a.str("person_external_id")));
        m.put("people.identifiers.add", (bf, a) -> bf.people().identifiers()
                .add(a.str("person_external_id"), a.str("extra_id"), a.str("label")));
        m.put("people.identifiers.remove", (bf, a) -> bf.people().identifiers()
                .remove(a.str("person_external_id"), a.str("extra_id")));
        // Produtos
        m.put("products.list", (bf, a) -> bf.products().list(a.has("include_inactive") ? a.bool("include_inactive") : null, null));
        m.put("products.get", (bf, a) -> bf.products().get(a.str("slug")));
        m.put("products.upsert", (bf, a) -> bf.products().upsert(a.str("slug"), productUpsert(a)));
        m.put("products.archive", (bf, a) -> bf.products().archive(a.str("slug")));
        // Release notes
        m.put("release_notes.list", (bf, a) -> bf.releaseNotes().list(a.str("product_slug"), releaseNoteListParams(a, true)));
        m.put("release_notes.list_all", (bf, a) -> bf.releaseNotes().listAll(a.str("product_slug"), releaseNoteListParams(a, false)));
        m.put("release_notes.get", (bf, a) -> bf.releaseNotes().get(a.str("product_slug"), a.str("version")));
        m.put("release_notes.upsert", (bf, a) -> bf.releaseNotes()
                .upsert(a.str("product_slug"), a.str("version"), releaseNoteUpsert(a)));
        m.put("release_notes.publish", (bf, a) -> bf.releaseNotes().publish(a.str("product_slug"), a.str("version")));
        // Base de conhecimento
        m.put("kb.articles.list", (bf, a) -> bf.kb().articles().list(kbArticleListParams(a, true)));
        m.put("kb.articles.list_all", (bf, a) -> bf.kb().articles().listAll(kbArticleListParams(a, false)));
        m.put("kb.articles.get", (bf, a) -> bf.kb().articles().get(a.str("external_id")));
        m.put("kb.articles.upsert", (bf, a) -> bf.kb().articles().upsert(a.str("external_id"), kbArticleUpsert(a)));
        m.put("kb.articles.batch_upsert", (bf, a) -> bf.kb().articles().batchUpsert(batchArticles(a)));
        m.put("kb.articles.publish", (bf, a) -> bf.kb().articles().publish(a.str("external_id")));
        m.put("kb.articles.unpublish", (bf, a) -> bf.kb().articles().unpublish(a.str("external_id")));
        m.put("kb.articles.delete", (bf, a) -> bf.kb().articles().delete(a.str("external_id")));
        m.put("kb.search", (bf, a) -> bf.kb().search(a.str("q"), a.str("product"), a.integer("limit")));
        // Agentes de IA
        m.put("ai_agents.list", (bf, a) -> bf.aiAgents().list());
        m.put("ai_agents.get", (bf, a) -> bf.aiAgents().get(a.str("agent_id")));
        m.put("ai_agents.preview", (bf, a) -> bf.aiAgents().preview(a.str("agent_id"), a.str("message"), history(a)));
        return Collections.unmodifiableMap(m);
    }

    private static CustomerUpsert customerUpsert(Args a) {
        CustomerUpsert.Builder b = CustomerUpsert.builder();
        if (a.has("name")) {
            b.name(a.str("name"));
        }
        if (a.has("document")) {
            b.document(a.str("document"));
        }
        if (a.has("email")) {
            b.email(a.str("email"));
        }
        if (a.has("phone")) {
            b.phone(a.str("phone"));
        }
        if (a.has("website")) {
            b.website(a.str("website"));
        }
        if (a.has("notes")) {
            b.notes(a.str("notes"));
        }
        if (a.has("custom_fields")) {
            List<Args> fields = a.objects("custom_fields");
            if (fields == null) {
                b.customFields((List<CustomFieldInput>) null);
            } else {
                b.customFields(fields.stream().map(ConformanceTest::customField).collect(Collectors.toList()));
            }
        }
        return b.build();
    }

    private static List<CustomerBatchItem> customerBatchItems(Args a) {
        List<CustomerBatchItem> out = new ArrayList<>();
        for (Args x : a.objects("items")) {
            out.add(CustomerBatchItem.of(x.str("external_id"), customerUpsert(x)));
        }
        return out;
    }

    private static PersonUpsert personUpsert(Args a) {
        PersonUpsert.Builder b = PersonUpsert.builder();
        if (a.has("name")) {
            b.name(a.str("name"));
        }
        if (a.has("email")) {
            b.email(a.str("email"));
        }
        if (a.has("phone")) {
            b.phone(a.str("phone"));
        }
        if (a.has("role")) {
            b.role(a.str("role"));
        }
        if (a.has("access")) {
            b.access(a.bool("access"));
        }
        if (a.has("is_primary")) {
            b.isPrimary(a.bool("is_primary"));
        }
        if (a.has("extra_emails")) {
            Object v = a.raw("extra_emails");
            b.extraEmails(v == null ? null : stringList(v));
        }
        if (a.has("extra_phones")) {
            Object v = a.raw("extra_phones");
            b.extraPhones(v == null ? null : stringList(v));
        }
        return b.build();
    }

    private static List<PersonBatchItem> personBatchItems(Args a) {
        List<PersonBatchItem> out = new ArrayList<>();
        for (Args x : a.objects("items")) {
            out.add(PersonBatchItem.of(x.str("customer_external_id"), x.str("external_id"), personUpsert(x)));
        }
        return out;
    }

    private static CustomFieldInput customField(Args f) {
        CustomFieldInput.Builder b = CustomFieldInput.builder(f.str("key"));
        if (f.has("label")) {
            b.label(f.str("label"));
        }
        if (f.has("type")) {
            b.type(f.str("type"));
        }
        if (f.has("value")) {
            b.value(f.raw("value"));
        }
        if (f.has("options")) {
            b.options(stringList(f.raw("options")));
        }
        return b.build();
    }

    private static CustomerListParams customerListParams(Args a, boolean withPage) {
        CustomerListParams.Builder b = CustomerListParams.builder()
                .q(a.str("q"))
                .updatedSince(a.str("updated_since"))
                .pageSize(a.integer("page_size"));
        if (withPage) {
            b.page(a.integer("page"));
        }
        return b.build();
    }

    private static ContactUpsert contactUpsert(Args a) {
        ContactUpsert.Builder b = ContactUpsert.builder();
        if (a.has("name")) {
            b.name(a.str("name"));
        }
        if (a.has("role")) {
            b.role(a.str("role"));
        }
        if (a.has("email")) {
            b.email(a.str("email"));
        }
        if (a.has("phone")) {
            b.phone(a.str("phone"));
        }
        if (a.has("notes")) {
            b.notes(a.str("notes"));
        }
        if (a.has("is_primary")) {
            b.isPrimary(a.bool("is_primary"));
        }
        return b.build();
    }

    private static InteractionListParams interactionListParams(Args a, boolean withPage) {
        InteractionListParams.Builder b = InteractionListParams.builder().pageSize(a.integer("page_size"));
        if (withPage) {
            b.page(a.integer("page"));
        }
        return b.build();
    }

    private static ProductUpsert productUpsert(Args a) {
        ProductUpsert.Builder b = ProductUpsert.builder();
        if (a.has("name")) {
            b.name(a.str("name"));
        }
        if (a.has("description")) {
            b.description(a.str("description"));
        }
        if (a.has("color")) {
            b.color(a.str("color"));
        }
        if (a.has("icon")) {
            b.icon(a.str("icon"));
        }
        if (a.has("is_active")) {
            b.isActive(a.bool("is_active"));
        }
        if (a.has("sort_order")) {
            b.sortOrder(a.integer("sort_order"));
        }
        return b.build();
    }

    private static ReleaseNoteListParams releaseNoteListParams(Args a, boolean withPage) {
        ReleaseNoteListParams.Builder b = ReleaseNoteListParams.builder()
                .published(a.bool("published"))
                .pageSize(a.integer("page_size"));
        if (withPage) {
            b.page(a.integer("page"));
        }
        return b.build();
    }

    private static ReleaseNoteUpsert releaseNoteUpsert(Args a) {
        ReleaseNoteUpsert.Builder b = ReleaseNoteUpsert.builder();
        if (a.has("title")) {
            b.title(a.str("title"));
        }
        if (a.has("description_html")) {
            b.descriptionHtml(a.str("description_html"));
        }
        if (a.has("description_markdown")) {
            b.descriptionMarkdown(a.str("description_markdown"));
        }
        if (a.has("audience")) {
            b.audience(a.str("audience"));
        }
        if (a.has("require_ack_internal")) {
            b.requireAckInternal(a.bool("require_ack_internal"));
        }
        if (a.has("require_ack_external")) {
            b.requireAckExternal(a.bool("require_ack_external"));
        }
        if (a.has("publish")) {
            b.publish(a.bool("publish"));
        }
        return b.build();
    }

    private static KbArticleListParams kbArticleListParams(Args a, boolean withPage) {
        KbArticleListParams.Builder b = KbArticleListParams.builder()
                .product(a.str("product"))
                .status(a.str("status"))
                .q(a.str("q"))
                .updatedSince(a.str("updated_since"))
                .pageSize(a.integer("page_size"));
        if (withPage) {
            b.page(a.integer("page"));
        }
        return b.build();
    }

    private static KbArticleUpsert kbArticleUpsert(Args a) {
        KbArticleUpsert.Builder b = KbArticleUpsert.builder();
        if (a.has("title")) {
            b.title(a.str("title"));
        }
        if (a.has("body_html")) {
            b.bodyHtml(a.str("body_html"));
        }
        if (a.has("body_markdown")) {
            b.bodyMarkdown(a.str("body_markdown"));
        }
        if (a.has("product")) {
            b.product(a.str("product"));
        }
        if (a.has("status")) {
            b.status(a.str("status"));
        }
        return b.build();
    }

    private static List<KbBatchArticle> batchArticles(Args a) {
        List<KbBatchArticle> out = new ArrayList<>();
        for (Args x : a.objects("articles")) {
            KbBatchArticle.Builder b = KbBatchArticle.builder(x.str("external_id"));
            if (x.has("title")) {
                b.title(x.str("title"));
            }
            if (x.has("body_html")) {
                b.bodyHtml(x.str("body_html"));
            }
            if (x.has("body_markdown")) {
                b.bodyMarkdown(x.str("body_markdown"));
            }
            if (x.has("product")) {
                b.product(x.str("product"));
            }
            if (x.has("status")) {
                b.status(x.str("status"));
            }
            out.add(b.build());
        }
        return out;
    }

    private static List<AiAgentPreviewTurn> history(Args a) {
        if (!a.has("history")) {
            return null;
        }
        List<AiAgentPreviewTurn> out = new ArrayList<>();
        for (Args turn : a.objects("history")) {
            out.add(new AiAgentPreviewTurn(turn.str("role"), turn.str("content")));
        }
        return out;
    }

    private static Map<String, Class<? extends BfocusException>> errorTypes() {
        Map<String, Class<? extends BfocusException>> m = new LinkedHashMap<>();
        m.put("authentication", AuthenticationException.class);
        m.put("permission_denied", PermissionDeniedException.class);
        m.put("not_found", NotFoundException.class);
        m.put("conflict", ConflictException.class);
        m.put("validation", ValidationException.class);
        m.put("rate_limit", RateLimitException.class);
        m.put("server", ServerException.class);
        m.put("network", NetworkException.class);
        m.put("api", BfocusException.class);
        return Collections.unmodifiableMap(m);
    }

    // ── casos ────────────────────────────────────────────────────────────────────────────────────────────

    @TestFactory
    Stream<DynamicTest> casos() {
        List<Map<String, Object>> cases = mapList(CASES.get("cases"));
        assertFalse(cases.isEmpty(), "cases.json sem casos");
        Set<String> ids = new HashSet<>();
        for (Map<String, Object> c : cases) {
            assertTrue(ids.add((String) c.get("id")), "id de caso duplicado: " + c.get("id"));
        }
        return cases.stream().map(c -> DynamicTest.dynamicTest((String) c.get("id"), () -> runCase(c)));
    }

    private static void runCase(Map<String, Object> c) {
        String op = (String) c.get("op");
        assertTrue(OPS.containsKey(op), "op '" + op + "' sem método na SDK (e fora de sdk_excluded_ops) — implemente e mapeie em OPS");
        List<Map<String, Object>> exchanges = mapList(c.get("exchanges"));
        List<FakeServer.Reply> replies = new ArrayList<>();
        for (Map<String, Object> exchange : exchanges) {
            replies.add(FakeServer.Reply.fromCase(map(exchange.get("response"))));
        }
        server.reset(replies);

        List<Duration> sleeps = new ArrayList<>();
        Args args = new Args(map(c.get("args")), "args");
        Object result = null;
        BfocusException error = null;
        try (BfocusClient bf = BfocusClient.builder(API_KEY).baseUrl(server.baseUrl()).build()) {
            bf.setSleeper(sleeps::add);   // esperas desligadas: a suíte não dorme
            try {
                result = normalize(OPS.get(op).apply(bf, args));
            } catch (BfocusException e) {
                error = e;
            }
        }
        args.assertAllUsed();

        List<FakeServer.Recorded> requests = server.requests();
        checkExchanges(exchanges, requests);
        checkSleeps(exchanges, sleeps);

        Map<String, Object> expect = map(c.get("expect"));
        if (expect.containsKey("error")) {
            Map<String, Object> want = map(expect.get("error"));
            String type = (String) want.get("type");
            assertNotNull(error, "esperava erro " + type + ", veio " + result);
            Class<? extends BfocusException> cls = ERROR_TYPES.get(type);
            assertNotNull(cls, "tipo de erro desconhecido no caso: " + type);
            assertSame(cls, error.getClass(), "classe do erro");
            assertEquals(want.get("code"), error.getCode(), "code");
            assertEquals(((Number) want.get("status")).intValue(), error.getStatus(), "status");
            if (want.containsKey("request_id")) {
                Object expectedId = want.get("request_id");
                if (SENT.equals(expectedId)) {
                    expectedId = requests.get(requests.size() - 1).header("x-request-id");
                    assertNotNull(expectedId);
                }
                assertEquals(expectedId, error.getRequestId(), "request_id");
            }
            if (want.containsKey("retry_after")) {
                assertNotNull(error.getRetryAfter(), "retry_after");
                assertEquals(((Number) want.get("retry_after")).doubleValue(), error.getRetryAfter().toMillis() / 1000.0, 1e-9,
                        "retry_after (segundos)");
            }
            if (want.containsKey("required_scope")) {
                assertEquals(want.get("required_scope"), error.getRequiredScope(), "required_scope");
            }
            if (want.containsKey("validation")) {
                assertEquals(want.get("validation"), error.getValidation(), "validation");
            }
            assertTrue(error.getMessage().contains((String) want.get("code")), "a mensagem traz o code: " + error.getMessage());
        } else {
            if (error != null) {
                fail("erro inesperado: " + error, error);
            }
            assertEquals(expect.get("result"), result, "resultado (JSON neutro)");
        }
    }

    private static void checkExchanges(List<Map<String, Object>> exchanges, List<FakeServer.Recorded> requests) {
        assertEquals(exchanges.size(), requests.size(), () -> "nº de requisições: " + requests);
        FakeServer.Recorded previous = null;
        for (int i = 0; i < exchanges.size(); i++) {
            Map<String, Object> exchange = exchanges.get(i);
            Map<String, Object> want = map(exchange.get("request"));
            FakeServer.Recorded got = requests.get(i);
            String where = "troca " + i;

            assertEquals(want.get("method"), got.method, where + ": método");
            assertEquals(want.get("path"), got.path, where + ": caminho (exatamente como codificado)");
            assertEquals(sortedPairs(map(want.get("query"))), got.sortedQuery(), where + ": query");
            Object body = want.get("body");
            if (body == null) {
                assertEquals(0, got.body.length, where + ": não devia ter corpo");
                assertNull(got.header("content-type"), where + ": Content-Type sem corpo");
            } else {
                assertEquals("application/json", got.header("content-type"), where + ": Content-Type");
                assertEquals(body, got.json(), where + ": corpo (igualdade profunda)");
            }

            assertEquals("Bearer " + API_KEY, got.header("authorization"), where + ": Authorization");
            assertEquals("application/json", got.header("accept"), where + ": Accept");
            assertMatches(CLIENT_RE, got.header("x-bfocus-client"), where + ": X-Bfocus-Client");
            assertEquals(got.header("x-bfocus-client"), got.header("user-agent"), where + ": User-Agent");
            assertMatches(REQUEST_ID_RE, got.header("x-request-id"), where + ": X-Request-Id");
            if (WRITES.contains(want.get("method"))) {
                String key = got.header("idempotency-key");
                assertTrue(key != null && !key.isEmpty(), where + ": Idempotency-Key em escrita");
            } else {
                assertNull(got.header("idempotency-key"), where + ": Idempotency-Key em leitura");
            }

            assertTrue(exchange.containsKey("retry"), where + ": troca sem o campo 'retry'");
            boolean retry = Boolean.TRUE.equals(exchange.get("retry"));
            if (i == 0) {
                assertFalse(retry, "a 1ª troca não pode ser nova tentativa");
            } else if (retry) {
                // nova tentativa da MESMA chamada: ids repetidos
                assertEquals(previous.header("x-request-id"), got.header("x-request-id"), where + ": X-Request-Id da nova tentativa");
                assertEquals(previous.header("idempotency-key"), got.header("idempotency-key"),
                        where + ": Idempotency-Key da nova tentativa");
            } else {
                // chamada lógica nova (ex.: próxima página do listAll): ids novos
                assertNotEquals(previous.header("x-request-id"), got.header("x-request-id"),
                        where + ": chamada nova precisa de X-Request-Id novo");
                if (got.header("idempotency-key") != null) {
                    assertNotEquals(previous.header("idempotency-key"), got.header("idempotency-key"),
                            where + ": chamada nova precisa de Idempotency-Key nova");
                }
            }
            previous = got;
        }
    }

    /** Uma espera (capturada, não dormida) por nova tentativa: Retry-After (teto 60 s) ou o backoff do BRIEF §5. */
    private static void checkSleeps(List<Map<String, Object>> exchanges, List<Duration> sleeps) {
        int retries = 0;
        for (Map<String, Object> exchange : exchanges) {
            if (Boolean.TRUE.equals(exchange.get("retry"))) {
                retries++;
            }
        }
        assertEquals(retries, sleeps.size(), "uma espera por nova tentativa");
        int k = 0;
        int attempt = 0;
        for (int i = 1; i < exchanges.size(); i++) {
            if (!Boolean.TRUE.equals(exchanges.get(i).get("retry"))) {
                attempt = 0;
                continue;
            }
            Duration slept = sleeps.get(k++);
            String retryAfter = headerIgnoringCase(map(map(exchanges.get(i - 1).get("response")).get("headers")), "Retry-After");
            if (retryAfter != null && SECONDS_RE.matcher(retryAfter).matches()) {
                long expected = Math.round(Math.min(60.0, Double.parseDouble(retryAfter)) * 1000);
                assertEquals(expected, slept.toMillis(), "espera = Retry-After (teto 60 s)");
            } else {
                double base = Math.min(8.0, 0.5 * Math.pow(2, attempt));
                double seconds = slept.toMillis() / 1000.0;
                assertTrue(seconds >= base - 0.001 && seconds <= base * 1.25 + 0.001, "backoff " + attempt + ": " + slept);
            }
            attempt++;
        }
    }

    private static String headerIgnoringCase(Map<String, Object> headers, String name) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, Object> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return String.valueOf(e.getValue());
            }
        }
        return null;
    }

    private static List<String> sortedPairs(Map<String, Object> query) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Object> e : query.entrySet()) {
            out.add(e.getKey() + "=" + e.getValue());
        }
        Collections.sort(out);
        return out;
    }

    private static void assertMatches(Pattern pattern, String value, String what) {
        assertTrue(value != null && pattern.matcher(value).matches(), what + ": '" + value + "' não casa com " + pattern);
    }

    /** Retorno da SDK → JSON neutro ({@code Page} vira objeto; {@code PagedIterable} vira lista). */
    static Object normalize(Object value) {
        if (value instanceof ApiObject) {
            return normalize(((ApiObject) value).raw());
        }
        if (value instanceof Page) {
            Page<?> page = (Page<?>) value;
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("items", normalize(page.getItems()));
            out.put("page", (long) page.getPageNumber());
            out.put("page_size", (long) page.getPageSize());
            out.put("total", (long) page.getTotal());
            out.put("pages", (long) page.getPages());
            return out;
        }
        if (value instanceof PagedIterable) {
            List<Object> out = new ArrayList<>();
            for (Object item : (PagedIterable<?>) value) {
                out.add(normalize(item));
            }
            return out;
        }
        if (value instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                out.put((String) e.getKey(), normalize(e.getValue()));
            }
            return out;
        }
        if (value instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) value) {
                out.add(normalize(item));
            }
            return out;
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return ((Number) value).longValue();
        }
        return value;
    }

    // ── cobertura ────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void todoOpDosCasosTemMetodo() {
        Set<String> missing = new TreeSet<>();
        for (Map<String, Object> c : mapList(CASES.get("cases"))) {
            missing.add((String) c.get("op"));
        }
        missing.removeAll(OPS.keySet());
        missing.removeAll(EXCLUDED);
        assertEquals(Set.of(), missing, "ops sem método na SDK — implemente ou exclua");
    }

    @Test
    void helpersApontamParaOpsMapeados() {
        Object helpers = CASES.get("sdk_helper_ops");
        if (helpers == null) {
            return;
        }
        for (Map.Entry<String, Object> e : map(helpers).entrySet()) {
            assertTrue(OPS.containsKey(e.getKey()), "helper sem método: " + e.getKey());
            assertTrue(OPS.containsKey((String) e.getValue()), "base do helper sem método: " + e.getValue());
        }
    }

    @Test
    void excluidosNaoEstaoNaSdk() {
        Set<String> both = new TreeSet<>(EXCLUDED);
        both.retainAll(OPS.keySet());
        assertEquals(Set.of(), both);
    }

    @Test
    void todaOperacaoDaSpecTemMetodo() throws IOException {
        Path spec = projectDir().resolve("../../api/openapi/public.json").normalize();
        assumeTrue(Files.isRegularFile(spec), "public.json só existe no monorepo");
        Map<String, Object> doc = map(Json.parse(Files.readString(spec, StandardCharsets.UTF_8)));
        Set<String> opIds = new TreeSet<>();
        for (Object item : map(doc.get("paths")).values()) {
            for (Object operation : map(item).values()) {
                if (operation instanceof Map && map(operation).get("operationId") instanceof String) {
                    opIds.add((String) map(operation).get("operationId"));
                }
            }
        }
        assertFalse(opIds.isEmpty());
        opIds.removeAll(OPS.keySet());
        opIds.removeAll(EXCLUDED);
        assertEquals(Set.of(), opIds, "operações da spec sem método na SDK");
    }

    @Test
    void copiaDosCasosEmDia() throws IOException {
        Path original = projectDir().resolve("../conformance/cases.json").normalize();
        assumeTrue(Files.isRegularFile(original), "a fonte dos casos só existe no monorepo");
        Path copy = projectDir().resolve("src/test/resources/conformance/cases.json");
        assertArrayEquals(Files.readAllBytes(original), Files.readAllBytes(copy),
                "src/test/resources/conformance/cases.json desatualizado — rode `python3 clients/conformance/generate.py`"
                        + " (ele escreve a cópia; não edite à mão)");
    }

    @Test
    void vetoresDeAssinatura() {
        List<Map<String, Object>> vectors = mapList(CASES.get("signatures"));
        assertFalse(vectors.isEmpty());
        for (Map<String, Object> v : vectors) {
            String secret = (String) v.get("secret");
            String user = (String) v.get("user_external_id");
            String customer = (String) v.get("customer_external_id");
            assertEquals(v.get("expected"), WidgetIdentity.sign(secret, user, customer), "WidgetIdentity.sign " + user);
            assertEquals(v.get("expected"), BfocusClient.signWidgetIdentity(secret, user, customer), "BfocusClient.signWidgetIdentity " + user);
        }
    }

    @Test
    void vetoresDeAssinaturaV2() {
        List<Map<String, Object>> vectors = mapList(CASES.get("signatures_v2"));
        assertFalse(vectors.isEmpty());
        for (Map<String, Object> v : vectors) {
            String secret = (String) v.get("secret");
            String user = (String) v.get("user_external_id");
            String customer = (String) v.get("customer_external_id");
            Instant at = Instant.ofEpochSecond(((Number) v.get("timestamp")).longValue());
            assertEquals(v.get("expected"), WidgetIdentity.signV2(secret, user, customer, at), "WidgetIdentity.signV2 " + user);
            assertEquals(v.get("expected"), BfocusClient.signWidgetIdentityV2(secret, user, customer, at),
                    "BfocusClient.signWidgetIdentityV2 " + user);
            // fração de segundo é descartada (floor)
            assertEquals(v.get("expected"), WidgetIdentity.signV2(secret, user, customer, at.plusMillis(999)), "floor " + user);
        }
    }

    // ── argumentos do caso ───────────────────────────────────────────────────────────────────────────────

    /**
     * Os {@code args} de um caso, com registro do que a tabela usou: argumento que a tabela não repassa à SDK
     * (campo novo no caso, sem método/setter) FALHA o caso.
     */
    static final class Args {
        private final Map<String, Object> map;
        private final String where;
        private final Set<String> used = new HashSet<>();
        private final List<Args> children = new ArrayList<>();

        Args(Map<String, Object> map, String where) {
            this.map = map == null ? Collections.<String, Object>emptyMap() : map;
            this.where = where;
        }

        boolean has(String key) {
            return map.containsKey(key);
        }

        Object raw(String key) {
            used.add(key);
            return map.get(key);
        }

        String str(String key) {
            Object v = raw(key);
            if (v != null && !(v instanceof String)) {
                fail(where + "." + key + ": esperava string, veio " + v);
            }
            return (String) v;
        }

        Integer integer(String key) {
            Object v = raw(key);
            return v == null ? null : Math.toIntExact(((Number) v).longValue());
        }

        Boolean bool(String key) {
            return (Boolean) raw(key);
        }

        List<Args> objects(String key) {
            Object v = raw(key);
            if (v == null) {
                return null;
            }
            List<Args> out = new ArrayList<>();
            List<Object> items = list(v);
            for (int i = 0; i < items.size(); i++) {
                Args child = new Args(map(items.get(i)), where + "." + key + "[" + i + "]");
                children.add(child);
                out.add(child);
            }
            return out;
        }

        void assertAllUsed() {
            Set<String> left = new TreeSet<>(map.keySet());
            left.removeAll(used);
            assertEquals(Set.of(), left, where + ": argumentos do caso que a tabela OPS não repassou à SDK");
            for (Args child : children) {
                child.assertAllUsed();
            }
        }
    }
}
