package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.fail;
import static br.com.bernisoftware.bfocus.TestSupport.obj;
import static br.com.bernisoftware.bfocus.TestSupport.ok;
import static br.com.bernisoftware.bfocus.TestSupport.raw;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Novas tentativas (BRIEF §5 e §10.3): o que repete, quanto espera, e ids repetidos. */
class RetryTest extends ServerTestBase {

    private static void assertBetweenMillis(long min, long max, Duration actual) {
        assertTrue(actual.toMillis() >= min && actual.toMillis() <= max, "esperava entre " + min + " e " + max + " ms, veio " + actual);
    }

    @Test
    void backoffExponencialComJitter() {
        BfocusClient bf = client(fail(503, "X"), fail(504, "X"), ok(obj("deleted", true)));
        assertTrue(bf.customers().delete("C1").isDeleted());
        assertEquals(2, sleeps.size());
        assertBetweenMillis(500, 625, sleeps.get(0));
        assertBetweenMillis(1000, 1250, sleeps.get(1));
        List<FakeServer.Recorded> r = requests();
        assertEquals(3, r.size());
        assertEquals(1, new HashSet<>(r.stream().map(x -> x.header("idempotency-key")).collect(Collectors.toList())).size());
        assertEquals(1, new HashSet<>(r.stream().map(x -> x.header("x-request-id")).collect(Collectors.toList())).size());
    }

    @Test
    void retryAfterTemTetoDe60s() {
        BfocusClient bf = client(fail(429, "RATE_LIMITED", "Retry-After", "120"), ok(obj("id", "x")));
        bf.products().get("erp");
        assertEquals(Arrays.asList(Duration.ofSeconds(60)), sleeps);
    }

    @Test
    void retryAfterEm503EDecimal() {
        BfocusClient bf = client(fail(503, "X", "Retry-After", "3"), fail(502, "X", "Retry-After", "1.5"), ok(obj("id", "x")));
        bf.products().get("erp");
        assertEquals(Arrays.asList(Duration.ofSeconds(3), Duration.ofMillis(1500)), sleeps);
    }

    @Test
    void retryAfterEmDataHttp() {
        String when = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(30));
        BfocusClient bf = client(fail(429, "RATE_LIMITED", "Retry-After", when), ok(obj("id", "x")));
        bf.products().get("erp");
        assertEquals(1, sleeps.size());
        assertBetweenMillis(25_000, 31_000, sleeps.get(0));
    }

    @Test
    void semRetryAfterUsaBackoff() {
        BfocusClient bf = client(fail(429, "RATE_LIMITED"), ok(obj("id", "x")));
        bf.products().get("erp");
        assertBetweenMillis(500, 625, sleeps.get(0));
    }

    @Test
    void erro500E4xxNaoRepetem() {
        BfocusClient bf = client(fail(500, "INTERNAL_ERROR"));
        assertThrows(ServerException.class, () -> bf.products().get("erp"));
        assertEquals(1, requests().size());
        assertEquals(0, sleeps.size());

        BfocusClient bf2 = client(fail(400, "BAD_REQUEST"));
        BfocusException e = assertThrows(BfocusException.class, () -> bf2.products().get("erp"));
        assertSame(BfocusException.class, e.getClass());
        assertEquals(1, requests().size());
        assertEquals(0, sleeps.size());

        BfocusClient bf3 = client(fail(409, "CONFLICT"));
        assertThrows(ConflictException.class, () -> bf3.kb().articles().publish("git:x"));
        assertEquals(1, requests().size());
    }

    @Test
    void maxRetriesZero() {
        BfocusClient bf = client(b -> b.maxRetries(0), fail(429, "RATE_LIMITED", "Retry-After", "2"));
        RateLimitException e = assertThrows(RateLimitException.class, () -> bf.products().get("erp"));
        assertEquals(Duration.ofSeconds(2), e.getRetryAfter());
        assertEquals(1, requests().size());
        assertEquals(0, sleeps.size());
    }

    @Test
    void retryAfterDoErroSoEm429() {
        BfocusClient bf = client(b -> b.maxRetries(0), fail(503, "X", "Retry-After", "3"));
        ServerException e = assertThrows(ServerException.class, () -> bf.products().get("erp"));
        assertNull(e.getRetryAfter());
    }

    @Test
    void naoJsonEsgotado() {
        FakeServer.Reply bad = raw(502, "Bad Gateway");
        BfocusClient bf = client(bad, bad, bad);
        ServerException e = assertThrows(ServerException.class, () -> bf.products().get("erp"));
        assertEquals("HTTP_502", e.getCode());
        assertEquals(502, e.getStatus());
        assertEquals(3, requests().size());
        assertEquals(requests().get(2).header("x-request-id"), e.getRequestId(), "request_id cai para o enviado");
    }

    @Test
    void parseRetryAfter() {
        assertEquals(Duration.ofSeconds(7), Transport.parseRetryAfter("7"));
        assertEquals(Duration.ofMillis(1500), Transport.parseRetryAfter(" 1.5 "));
        assertNull(Transport.parseRetryAfter(null));
        assertNull(Transport.parseRetryAfter("amanhã"));
        assertNull(Transport.parseRetryAfter("-1"));
        String future = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(30));
        assertBetweenMillis(25_000, 31_000, Transport.parseRetryAfter(future));
        String past = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(30));
        assertEquals(Duration.ZERO, Transport.parseRetryAfter(past));
    }

    @Test
    void backoff() {
        for (int i = 0; i < 50; i++) {
            assertBetweenMillis(500, 625, Transport.backoff(0, null));
            assertBetweenMillis(1000, 1250, Transport.backoff(1, null));
            assertBetweenMillis(8000, 10_000, Transport.backoff(10, null));   // teto de 8 s + jitter
        }
        assertEquals(Duration.ofSeconds(60), Transport.backoff(0, Duration.ofSeconds(120)));
        assertEquals(Duration.ofSeconds(3), Transport.backoff(3, Duration.ofSeconds(3)));
        assertEquals(Duration.ZERO, Transport.backoff(0, Duration.ofSeconds(-5)));
    }
}
