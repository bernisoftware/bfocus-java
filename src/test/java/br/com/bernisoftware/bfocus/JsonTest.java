package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.arr;
import static br.com.bernisoftware.bfocus.TestSupport.map;
import static br.com.bernisoftware.bfocus.TestSupport.obj;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.bernisoftware.bfocus.internal.Json;
import br.com.bernisoftware.bfocus.internal.JsonException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** O codec JSON interno (a SDK não depende de Jackson/Gson). */
class JsonTest {

    @Test
    void leTiposPadrao() {
        Map<String, Object> m = map(Json.parse("\uFEFF{\"a\":1,\"b\":1.5,\"c\":\"x\\u00e7\\n\",\"d\":true,\"e\":null,\"f\":[1,2],"
                + "\"g\":12345678901234567890,\"h\":-0.0e1,\"a\":2}"));
        assertEquals(2L, m.get("a"), "chave repetida: vale a última");
        assertEquals(new BigDecimal("1.5"), m.get("b"));
        assertEquals("xç\n", m.get("c"));
        assertEquals(Boolean.TRUE, m.get("d"));
        assertTrue(m.containsKey("e") && m.get("e") == null);
        assertEquals(arr(1L, 2L), m.get("f"));
        assertEquals(new BigInteger("12345678901234567890"), m.get("g"));
        assertEquals(new BigDecimal("-0.0e1"), m.get("h"));
        assertThrows(UnsupportedOperationException.class, () -> m.put("z", 1));
    }

    @Test
    void escreveEVolta() {
        Map<String, Object> value = obj("texto", "ação ✓ \"aspas\" \\ \u0001 😀", "n", 1500.0, "i", 7, "l", arr(true, null, 2.5),
                "vazio", obj(), "data", Instant.parse("2026-09-01T03:00:00Z"));
        String text = Json.write(value);
        assertTrue(text.contains("\"n\":1500,"), text);
        assertTrue(text.contains("ação ✓"), "não-ASCII sai sem escape: " + text);
        assertTrue(text.contains("\\u0001"), text);
        assertTrue(text.contains("\"data\":\"2026-09-01T03:00:00Z\""), text);
        Map<String, Object> back = map(Json.parse(text));
        assertEquals("ação ✓ \"aspas\" \\ \u0001 😀", back.get("texto"));
        assertEquals(arr(true, null, new BigDecimal("2.5")), back.get("l"));
    }

    @Test
    void recusaJsonInvalido() {
        for (String bad : new String[] {"", "{", "[1,]", "tru", "\"\\x\"", "01", "1 2", "{\"a\" 1}", "\"sem fim", "[\"\u0001\"]", "NaN"}) {
            assertThrows(JsonException.class, () -> Json.parse(bad), bad);
        }
        assertThrows(JsonException.class, () -> Json.parse("[".repeat(Json.MAX_DEPTH + 1)));
    }

    @Test
    void recusaValorQueNaoViraJson() {
        assertThrows(IllegalArgumentException.class, () -> Json.write(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Json.write(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Json.write(new Object()));
        assertThrows(IllegalArgumentException.class, () -> Json.write(Map.of(1, "chave não-string")));
    }
}
