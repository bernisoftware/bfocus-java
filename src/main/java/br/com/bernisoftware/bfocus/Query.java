package br.com.bernisoftware.bfocus;

import java.util.ArrayList;
import java.util.List;

/** Query string: omite o que não foi informado; booleanos {@code true}/{@code false}. */
final class Query {
    private final List<String[]> pairs = new ArrayList<>();

    Query add(String name, String value) {
        if (value != null) {
            pairs.add(new String[] {name, value});
        }
        return this;
    }

    Query add(String name, Integer value) {
        return value == null ? this : add(name, value.toString());
    }

    Query add(String name, Boolean value) {
        return value == null ? this : add(name, value ? "true" : "false");
    }

    @Override
    public String toString() {
        if (pairs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String[] pair : pairs) {
            sb.append(sb.length() == 0 ? '?' : '&').append(Paths.encode(pair[0])).append('=').append(Paths.encode(pair[1]));
        }
        return sb.toString();
    }
}
