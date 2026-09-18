package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.arr;
import static br.com.bernisoftware.bfocus.TestSupport.ok;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Construtor e opções do {@link BfocusClient} (BRIEF §3). */
class ClientTest extends ServerTestBase {

    @Test
    void chaveVaziaEErroDeArgumento() {
        for (String bad : new String[] {"", "   "}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new BfocusClient(bad));
            assertFalse(BfocusException.class.isInstance(e), "erro de argumento, não BfocusException");
            assertThrows(IllegalArgumentException.class, () -> BfocusClient.builder(bad));
        }
        assertThrows(NullPointerException.class, () -> new BfocusClient(null));
    }

    @Test
    void padroesESemRedeNaConstrucao() {
        BfocusClient bf = new BfocusClient("bf_live_unit");
        assertEquals("https://api.bfocus.com.br", bf.getBaseUrl());
        assertEquals(BfocusClient.DEFAULT_BASE_URL, bf.getBaseUrl());
        assertEquals(Duration.ofSeconds(30), bf.getTimeout());
        assertEquals(2, bf.getMaxRetries());

        server.reset(java.util.Collections.emptyList());
        BfocusClient local = BfocusClient.builder("bf_live_unit").baseUrl(server.baseUrl() + "/").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
        assertEquals(server.baseUrl(), local.getBaseUrl(), "barra final removida");
        assertEquals(0, requests().size(), "construir não chama a rede");
        assertTrue(local.customers() != null && local.kb().articles() != null && local.aiAgents() != null
                && local.releaseNotes() != null && local.products() != null && local.customers().contacts() != null);
        assertFalse(bf.toString().contains("bf_live_unit"), "toString não expõe a chave");
        assertEquals("http://localhost:8000", BfocusClient.builder("k").baseUrl("http://localhost:8000//").build().getBaseUrl());
    }

    @Test
    void opcoesInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> BfocusClient.builder("k").maxRetries(-1));
        assertThrows(IllegalArgumentException.class, () -> BfocusClient.builder("k").timeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> BfocusClient.builder("k").timeout(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> BfocusClient.builder("k").baseUrl("ftp://x"));
        assertThrows(IllegalArgumentException.class, () -> BfocusClient.builder("k").baseUrl("localhost:8000"));
        assertThrows(IllegalArgumentException.class, () -> BfocusClient.builder("k").baseUrl("https://x?y=1"));
        assertThrows(IllegalArgumentException.class, () -> RequestOptions.timeout(Duration.ZERO));
    }

    @Test
    void httpClientProprio() {
        BfocusClient bf = client(b -> b.httpClient(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()), ok(arr()));
        assertEquals(0, bf.products().list().size());
        assertEquals(1, requests().size());
        assertEquals("Bearer bf_live_unit", requests().get(0).header("authorization"));
    }

    @Test
    void fecharImpedeNovasChamadas() {
        BfocusClient bf = client(ok(arr()));
        bf.products().list();
        bf.close();
        bf.close();   // idempotente
        assertThrows(IllegalStateException.class, () -> bf.products().list());
        assertEquals(1, requests().size());
    }

    @Test
    void assinaturaDoWidget() {
        String a = WidgetIdentity.sign("bf_whs_x", "USR-1", "ACME-1");
        assertEquals("9a15d2527b855a048094ea7826c3b0f16ae5db3035ac3537324d45007ef15141", a);
        assertEquals(a, BfocusClient.signWidgetIdentity("bf_whs_x", "USR-1", "ACME-1"));
        assertTrue(a.matches("^[0-9a-f]{64}$"));
        assertThrows(IllegalArgumentException.class, () -> WidgetIdentity.sign("", "u", "c"));
        assertThrows(NullPointerException.class, () -> WidgetIdentity.sign(null, "u", "c"));
        assertThrows(NullPointerException.class, () -> WidgetIdentity.sign("s", null, "c"));
    }
}
