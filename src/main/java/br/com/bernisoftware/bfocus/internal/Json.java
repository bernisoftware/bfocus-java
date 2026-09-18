package br.com.bernisoftware.bfocus.internal;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Codec JSON (RFC 8259) pequeno e sem dependências. Uso interno da SDK.
 *
 * <p>Leitura ({@link #parse}) produz tipos padrão do Java, todos IMUTÁVEIS:
 * <ul>
 * <li>objeto → {@code Map<String, Object>} (ordem preservada; chave repetida: vale a última);</li>
 * <li>array → {@code List<Object>};</li>
 * <li>string → {@link String};</li>
 * <li>número inteiro → {@link Long} (ou {@link BigInteger} fora da faixa); com fração/expoente →
 * {@link BigDecimal} (exato, sem arredondamento de ponto flutuante);</li>
 * <li>{@code true}/{@code false} → {@link Boolean}; {@code null} → {@code null}.</li>
 * </ul>
 *
 * <p>Escrita ({@link #write}) aceita esses mesmos tipos e ainda: qualquer {@link Number} finito,
 * {@link CharSequence}, {@link Character}, {@link Iterable}, arrays, {@link UUID}, {@link Enum} (pelo
 * {@code name()}) e datas {@code java.time}/{@link Date} (ISO 8601). Não-ASCII sai sem escape (UTF-8);
 * caracteres de controle e surrogates órfãos saem como {@code \\uXXXX}.
 */
public final class Json {
    /** Aninhamento máximo aceito na leitura (protege a pilha contra entrada maliciosa). */
    public static final int MAX_DEPTH = 512;

    private static final Pattern JSON_NUMBER = Pattern.compile("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final char BOM = (char) 0xFEFF;

    private Json() {
    }

    // ── leitura ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Lê um documento JSON.
     *
     * @param text o texto (um BOM inicial é ignorado)
     * @return o valor lido (ver tipos na documentação da classe)
     * @throws JsonException texto inválido
     */
    public static Object parse(String text) {
        if (text == null) {
            throw new JsonException("texto nulo", 0);
        }
        Parser parser = new Parser(text);
        return parser.document();
    }

    private static final class Parser {
        private final String s;
        private final int n;
        private int i;
        private int depth;

        Parser(String s) {
            this.s = s;
            this.n = s.length();
            this.i = (n > 0 && s.charAt(0) == BOM) ? 1 : 0;
        }

        Object document() {
            skipWhitespace();
            Object value = value();
            skipWhitespace();
            if (i < n) {
                throw error("conteúdo depois do fim do JSON");
            }
            return value;
        }

        private Object value() {
            if (i >= n) {
                throw error("fim inesperado do texto");
            }
            char c = s.charAt(i);
            switch (c) {
                case '{':
                    return object();
                case '[':
                    return array();
                case '"':
                    return string();
                case 't':
                    literal("true");
                    return Boolean.TRUE;
                case 'f':
                    literal("false");
                    return Boolean.FALSE;
                case 'n':
                    literal("null");
                    return null;
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return number();
                    }
                    throw error("caractere inesperado '" + printable(c) + "'");
            }
        }

        private Map<String, Object> object() {
            enter();
            i++; // {
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                i++;
                depth--;
                return Collections.unmodifiableMap(map);
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw error("esperava o nome (string) de um campo");
                }
                String key = string();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                map.put(key, value());
                skipWhitespace();
                char c = next("esperava ',' ou '}'");
                if (c == '}') {
                    break;
                }
                if (c != ',') {
                    i--;
                    throw error("esperava ',' ou '}'");
                }
            }
            depth--;
            return Collections.unmodifiableMap(map);
        }

        private List<Object> array() {
            enter();
            i++; // [
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                i++;
                depth--;
                return Collections.unmodifiableList(list);
            }
            while (true) {
                skipWhitespace();
                list.add(value());
                skipWhitespace();
                char c = next("esperava ',' ou ']'");
                if (c == ']') {
                    break;
                }
                if (c != ',') {
                    i--;
                    throw error("esperava ',' ou ']'");
                }
            }
            depth--;
            return Collections.unmodifiableList(list);
        }

        private String string() {
            i++; // "
            StringBuilder sb = null;
            int start = i;
            while (true) {
                if (i >= n) {
                    throw error("string sem aspas de fechamento");
                }
                char c = s.charAt(i);
                if (c == '"') {
                    String result = sb == null ? s.substring(start, i) : sb.append(s, start, i).toString();
                    i++;
                    return result;
                }
                if (c < 0x20) {
                    throw error("caractere de controle sem escape dentro de string");
                }
                if (c != '\\') {
                    i++;
                    continue;
                }
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(s, start, i);
                i++; // barra
                if (i >= n) {
                    throw error("escape incompleto");
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        sb.append(hex4());
                        break;
                    default:
                        i--;
                        throw error("escape inválido '\\" + printable(e) + "'");
                }
                start = i;
            }
        }

        private char hex4() {
            if (i + 4 > n) {
                throw error("escape \\u incompleto");
            }
            int value = 0;
            for (int k = 0; k < 4; k++) {
                int d = Character.digit(s.charAt(i + k), 16);
                if (d < 0) {
                    i += k;
                    throw error("dígito hexadecimal inválido em \\u");
                }
                value = (value << 4) | d;
            }
            i += 4;
            return (char) value;
        }

        private Object number() {
            int start = i;
            if (s.charAt(i) == '-') {
                i++;
            }
            if (i >= n) {
                throw error("número incompleto");
            }
            char c = s.charAt(i);
            if (c == '0') {
                i++;
            } else if (c >= '1' && c <= '9') {
                while (i < n && isDigit(s.charAt(i))) {
                    i++;
                }
            } else {
                throw error("número inválido");
            }
            boolean integral = true;
            if (i < n && s.charAt(i) == '.') {
                integral = false;
                i++;
                digits();
            }
            if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                integral = false;
                i++;
                if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                    i++;
                }
                digits();
            }
            String literal = s.substring(start, i);
            if (integral) {
                if (literal.length() <= 18) {
                    return Long.parseLong(literal);
                }
                BigInteger big = new BigInteger(literal);
                return big.bitLength() < 64 ? (Object) big.longValue() : big;
            }
            try {
                return new BigDecimal(literal);
            } catch (NumberFormatException ex) {
                throw new JsonException("número fora da faixa suportada: " + literal, start);
            }
        }

        private void digits() {
            int start = i;
            while (i < n && isDigit(s.charAt(i))) {
                i++;
            }
            if (i == start) {
                throw error("número inválido (faltam dígitos)");
            }
        }

        private void literal(String word) {
            if (!s.startsWith(word, i)) {
                throw error("literal inválido (esperava " + word + ")");
            }
            i += word.length();
        }

        private void enter() {
            if (++depth > MAX_DEPTH) {
                throw error("aninhamento acima de " + MAX_DEPTH + " níveis");
            }
        }

        private void skipWhitespace() {
            while (i < n) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    i++;
                } else {
                    return;
                }
            }
        }

        private char peek() {
            return i < n ? s.charAt(i) : '\0';
        }

        private char next(String whatIfEnd) {
            if (i >= n) {
                throw error(whatIfEnd + " (fim do texto)");
            }
            return s.charAt(i++);
        }

        private void expect(char c) {
            if (i >= n || s.charAt(i) != c) {
                throw error("esperava '" + c + "'");
            }
            i++;
        }

        private JsonException error(String message) {
            return new JsonException("JSON inválido: " + message, i);
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private static String printable(char c) {
            return c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c);
        }
    }

    // ── escrita ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Serializa um valor em JSON compacto.
     *
     * @param value o valor (ver tipos aceitos na documentação da classe)
     * @return o texto JSON
     * @throws IllegalArgumentException tipo não suportado, número não finito ou chave de objeto não-string
     */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, 0);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("JSON: aninhamento acima de " + MAX_DEPTH + " níveis (referência circular?)");
        }
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            quote(sb, (String) value);
        } else if (value instanceof Boolean) {
            sb.append(((Boolean) value).booleanValue() ? "true" : "false");
        } else if (value instanceof Number) {
            number(sb, (Number) value);
        } else if (value instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof CharSequence)) {
                    throw new IllegalArgumentException("JSON: chave de objeto precisa ser String, veio " + (key == null ? "null" : key.getClass().getName()));
                }
                if (!first) {
                    sb.append(',');
                }
                first = false;
                quote(sb, key.toString());
                sb.append(':');
                write(sb, entry.getValue(), depth + 1);
            }
            sb.append('}');
        } else if (value instanceof Iterable) {
            sb.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, item, depth + 1);
            }
            sb.append(']');
        } else if (value.getClass().isArray()) {
            sb.append('[');
            int length = Array.getLength(value);
            for (int k = 0; k < length; k++) {
                if (k > 0) {
                    sb.append(',');
                }
                write(sb, Array.get(value, k), depth + 1);
            }
            sb.append(']');
        } else if (value instanceof CharSequence || value instanceof Character || value instanceof UUID) {
            quote(sb, value.toString());
        } else if (value instanceof Enum) {
            quote(sb, ((Enum<?>) value).name());
        } else if (value instanceof Instant || value instanceof OffsetDateTime || value instanceof LocalDate
                || value instanceof LocalDateTime || value instanceof LocalTime) {
            quote(sb, value.toString());
        } else if (value instanceof ZonedDateTime) {
            quote(sb, ((ZonedDateTime) value).toOffsetDateTime().toString());
        } else if (value instanceof Date) {
            quote(sb, ((Date) value).toInstant().toString());
        } else {
            throw new IllegalArgumentException("JSON: tipo não suportado: " + value.getClass().getName());
        }
    }

    private static void number(StringBuilder sb, Number number) {
        if (number instanceof Double || number instanceof Float) {
            double d = number.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("JSON: número não finito (" + d + ")");
            }
            if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                sb.append((long) d);   // 1500.0 → 1500
            } else {
                sb.append(number instanceof Float ? Float.toString(number.floatValue()) : Double.toString(d));
            }
            return;
        }
        if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte
                || number instanceof BigInteger || number instanceof AtomicInteger || number instanceof AtomicLong) {
            sb.append(number.toString());
            return;
        }
        if (number instanceof BigDecimal) {
            sb.append(((BigDecimal) number).toString());   // "1E+3" é JSON válido
            return;
        }
        String text = number.toString();
        if (!JSON_NUMBER.matcher(text).matches()) {
            throw new IllegalArgumentException("JSON: número em formato não suportado: " + text + " (" + number.getClass().getName() + ")");
        }
        sb.append(text);
    }

    /**
     * Escreve {@code text} como string JSON (com aspas).
     *
     * @param sb destino
     * @param text texto
     */
    public static void quote(StringBuilder sb, String text) {
        sb.append('"');
        int n = text.length();
        for (int k = 0; k < n; k++) {
            char c = text.charAt(k);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        unicodeEscape(sb, c);
                    } else if (Character.isHighSurrogate(c)) {
                        if (k + 1 < n && Character.isLowSurrogate(text.charAt(k + 1))) {
                            sb.append(c).append(text.charAt(++k));   // par válido: sai como veio (UTF-8)
                        } else {
                            unicodeEscape(sb, c);   // órfão: não dá para codificar em UTF-8
                        }
                    } else if (Character.isLowSurrogate(c)) {
                        unicodeEscape(sb, c);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void unicodeEscape(StringBuilder sb, char c) {
        sb.append("\\u").append(HEX[(c >> 12) & 0xF]).append(HEX[(c >> 8) & 0xF]).append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
    }
}
