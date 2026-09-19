package br.com.bernisoftware.bfocus;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Leitura tipada de um objeto JSON da resposta (já imutável, vindo do codec). Campo ausente ou {@code null} →
 * valor padrão; campo com TIPO errado → {@link InvalidDataException} (vira {@code INVALID_RESPONSE}). Campos
 * desconhecidos são ignorados (a API ganha campos sem aviso).
 */
final class Wire {
    private final Map<String, Object> map;
    private final String type;

    private Wire(Map<String, Object> map, String type) {
        this.map = map;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    static Wire of(Object json, String type) {
        if (!(json instanceof Map)) {
            throw new InvalidDataException(type + ": esperava um objeto JSON, veio " + describe(json));
        }
        return new Wire((Map<String, Object>) json, type);
    }

    static <T> List<T> list(Object json, Function<Object, T> item, String what) {
        if (!(json instanceof List)) {
            throw new InvalidDataException(what + ": esperava uma lista JSON, veio " + describe(json));
        }
        List<?> source = (List<?>) json;
        List<T> out = new ArrayList<>(source.size());
        for (Object element : source) {
            out.add(item.apply(element));
        }
        return Collections.unmodifiableList(out);
    }

    static String stringItem(Object json) {
        if (json instanceof String) {
            return (String) json;
        }
        throw new InvalidDataException("esperava string na lista, veio " + describe(json));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> objectItem(Object json) {
        if (json instanceof Map) {
            return (Map<String, Object>) json;
        }
        throw new InvalidDataException("esperava objeto na lista, veio " + describe(json));
    }

    Map<String, Object> map() {
        return map;
    }

    String string(String key) {
        Object v = map.get(key);
        if (v == null || v instanceof String) {
            return (String) v;
        }
        throw bad(key, "string", v);
    }

    String string(String key, String fallback) {
        String s = string(key);
        return s == null ? fallback : s;
    }

    boolean bool(String key, boolean fallback) {
        Object v = map.get(key);
        if (v == null) {
            return fallback;
        }
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        throw bad(key, "booleano", v);
    }

    int integer(String key) {
        Object v = map.get(key);
        if (v == null) {
            return 0;
        }
        if (v instanceof Long) {
            long l = (Long) v;
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            }
        } else if (v instanceof BigDecimal) {
            try {
                return ((BigDecimal) v).intValueExact();
            } catch (ArithmeticException ignored) {
                // cai no erro abaixo
            }
        } else if (v instanceof BigInteger) {
            throw bad(key, "inteiro de 32 bits", v);
        }
        throw bad(key, "inteiro", v);
    }

    /** Inteiro que pode vir {@code null} (ausente ou {@code null} → {@code null}). */
    Integer integerOrNull(String key) {
        return map.get(key) == null ? null : integer(key);
    }

    Double decimal(String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        throw bad(key, "número", v);
    }

    OffsetDateTime date(String key) {
        String s = string(key);
        if (s == null) {
            return null;
        }
        OffsetDateTime parsed = parseDate(s);
        if (parsed == null) {
            throw new InvalidDataException(type + "." + key + ": data inválida '" + s + "'");
        }
        return parsed;
    }

    <T> List<T> list(String key, Function<Object, T> item) {
        Object v = map.get(key);
        if (v == null) {
            return Collections.emptyList();
        }
        return list(v, item, type + "." + key);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> object(String key) {
        Object v = map.get(key);
        if (v == null) {
            return Collections.emptyMap();
        }
        if (v instanceof Map) {
            return (Map<String, Object>) v;
        }
        throw bad(key, "objeto", v);
    }

    <T> T nested(String key, Function<Object, T> decoder) {
        Object v = map.get(key);
        return v == null ? null : decoder.apply(v);
    }

    Object any(String key) {
        return map.get(key);
    }

    Map<String, Object> extras(Set<String> known) {
        Map<String, Object> extras = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!known.contains(entry.getKey())) {
                extras.put(entry.getKey(), entry.getValue());
            }
        }
        return extras.isEmpty() ? Collections.<String, Object>emptyMap() : Collections.unmodifiableMap(extras);
    }

    /** ISO 8601 com offset; sem offset = UTC; aceita espaço no lugar do {@code T}. {@code null} se inválida. */
    static OffsetDateTime parseDate(String s) {
        String text = s.trim();
        if (text.length() > 10 && text.charAt(10) == ' ') {
            text = text.substring(0, 10) + 'T' + text.substring(11);
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // tenta sem offset
        }
        try {
            return LocalDateTime.parse(text).atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private InvalidDataException bad(String key, String expected, Object actual) {
        return new InvalidDataException(type + "." + key + ": esperava " + expected + ", veio " + describe(actual));
    }

    static String describe(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Map) {
            return "objeto";
        }
        if (v instanceof List) {
            return "lista";
        }
        if (v instanceof String) {
            String s = (String) v;
            return "string \"" + (s.length() > 40 ? s.substring(0, 40) + "…" : s) + "\"";
        }
        return v.getClass().getSimpleName() + " " + v;
    }
}
