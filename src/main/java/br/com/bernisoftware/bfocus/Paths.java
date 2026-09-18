package br.com.bernisoftware.bfocus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Parâmetros de caminho (percent-encoding por segmento) e datas de query. */
final class Paths {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private Paths() {
    }

    /**
     * Um segmento de caminho percent-encoded ({@code ERP 1042} → {@code ERP%201042}). Vazio, {@code "."} e
     * {@code ".."} são recusados antes de qualquer requisição (o cliente HTTP resolveria {@code %2E%2E}).
     */
    static String segment(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("'" + name + "' não pode ser vazio.");
        }
        if (value.equals(".") || value.equals("..")) {
            throw new IllegalArgumentException("'" + name + "' não pode ser '.' nem '..'.");
        }
        return encode(value);
    }

    /** {@code external_id} de artigo não aceita {@code /} (a API recusa) — use {@code :} para hierarquia. */
    static String kbExternalId(String value, String name) {
        if (value != null && value.indexOf('/') >= 0) {
            throw new IllegalArgumentException("'" + name + "' de artigo não aceita '/' — use ':' para hierarquia (ex.: 'git:guia:instalacao').");
        }
        return segment(value, name);
    }

    /** RFC 3986: mantém só {@code A-Z a-z 0-9 - . _ ~}; o resto vira {@code %XX} dos bytes UTF-8. */
    static String encode(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length + 8);
        for (byte b : bytes) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                sb.append((char) c);
            } else {
                sb.append('%').append(HEX[c >> 4]).append(HEX[c & 0xF]);
            }
        }
        return sb.toString();
    }

    /** {@code 2026-09-01T03:00:00Z} (UTC; fração de segundo só quando houver). */
    static String date(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(Objects.requireNonNull(instant, "updatedSince"));
    }

    static String date(OffsetDateTime value) {
        return date(Objects.requireNonNull(value, "updatedSince").toInstant());
    }

    static String date(ZonedDateTime value) {
        return date(Objects.requireNonNull(value, "updatedSince").toInstant());
    }

    static int pageSizeForAll(Integer page, Integer pageSize) {
        if (page != null) {
            throw new IllegalArgumentException("listAll percorre as páginas sozinho — não informe page (use list para uma página).");
        }
        int size = pageSize == null ? 100 : pageSize;
        if (size < 1) {
            throw new IllegalArgumentException("pageSize precisa ser ≥ 1.");
        }
        return size;
    }
}
