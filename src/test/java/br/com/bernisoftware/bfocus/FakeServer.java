package br.com.bernisoftware.bfocus;

import br.com.bernisoftware.bfocus.internal.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP local (JDK {@code com.sun.net.httpserver}) que responde, em ordem, as respostas enfileiradas e grava
 * cada requisição como chegou (caminho CRU, query, headers, corpo).
 *
 * <p>Requisição a mais (fila vazia) recebe 418 {@code UNEXPECTED_REQUEST} — status que a SDK não repete — e fica
 * gravada para o teste acusar.
 */
final class FakeServer implements AutoCloseable {

    /** Uma requisição recebida. */
    static final class Recorded {
        final String method;
        final String path;
        final String rawQuery;
        final List<String[]> query;
        final Map<String, String> headers;
        final byte[] body;

        Recorded(String method, String path, String rawQuery, List<String[]> query, Map<String, String> headers, byte[] body) {
            this.method = method;
            this.path = path;
            this.rawQuery = rawQuery;
            this.query = query;
            this.headers = headers;
            this.body = body;
        }

        /** Header pelo nome (qualquer caixa); {@code null} se ausente. */
        String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }

        Map<String, String> queryMap() {
            Map<String, String> out = new LinkedHashMap<>();
            for (String[] pair : query) {
                out.put(pair[0], pair[1]);
            }
            return out;
        }

        /** Pares {@code k=v} decodificados, ordenados (compara o conjunto de pares). */
        List<String> sortedQuery() {
            List<String> out = new ArrayList<>();
            for (String[] pair : query) {
                out.add(pair[0] + "=" + pair[1]);
            }
            Collections.sort(out);
            return out;
        }

        String text() {
            return new String(body, StandardCharsets.UTF_8);
        }

        /** Corpo como JSON; {@code null} quando não há corpo. */
        Object json() {
            return body.length == 0 ? null : Json.parse(text());
        }

        @Override
        public String toString() {
            return method + " " + path + (rawQuery == null ? "" : "?" + rawQuery);
        }
    }

    /** Uma resposta: {@code body} JSON (Map/List/…), {@code String} (texto puro) ou {@code null} (sem corpo). */
    static final class Reply {
        final int status;
        final Map<String, String> headers;
        final Object body;

        Reply(int status, Map<String, String> headers, Object body) {
            this.status = status;
            this.headers = headers == null ? Collections.<String, String>emptyMap() : headers;
            this.body = body;
        }

        /** A partir do {@code response} de uma troca de {@code cases.json}. */
        static Reply fromCase(Map<String, Object> response) {
            Map<String, String> headers = new LinkedHashMap<>();
            Object raw = response.get("headers");
            if (raw instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
                    headers.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
            return new Reply(((Number) response.get("status")).intValue(), headers, response.get("body"));
        }
    }

    private final HttpServer server;
    private final ExecutorService executor;
    private final Deque<Reply> replies = new ArrayDeque<>();
    private final List<Recorded> requests = new ArrayList<>();

    FakeServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "fake-bfocus-api");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    String baseUrl() {
        InetSocketAddress address = server.getAddress();
        return "http://" + address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    /** Zera as requisições gravadas e enfileira as próximas respostas. */
    synchronized void reset(List<Reply> next) {
        replies.clear();
        replies.addAll(next);
        requests.clear();
    }

    synchronized List<Recorded> requests() {
        return new ArrayList<>(requests);
    }

    synchronized int pendingReplies() {
        return replies.size();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            byte[] body = exchange.getRequestBody().readAllBytes();
            URI uri = exchange.getRequestURI();
            Map<String, String> headers = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> e : exchange.getRequestHeaders().entrySet()) {
                headers.put(e.getKey().toLowerCase(Locale.ROOT), String.join(",", e.getValue()));
            }
            Recorded record = new Recorded(exchange.getRequestMethod(), uri.getRawPath(), uri.getRawQuery(),
                    parseQuery(uri.getRawQuery()), headers, body);

            Reply reply;
            synchronized (this) {
                requests.add(record);
                reply = replies.poll();
            }
            if (reply == null) {
                Map<String, Object> unexpected = new LinkedHashMap<>();
                unexpected.put("code", 418L);
                unexpected.put("data", null);
                unexpected.put("message", "UNEXPECTED_REQUEST");
                unexpected.put("error", "UNEXPECTED_REQUEST");
                reply = new Reply(418, null, unexpected);
            }

            byte[] payload;
            String contentType;
            if (reply.body == null) {
                payload = new byte[0];
                contentType = null;
            } else if (reply.body instanceof String) {
                payload = ((String) reply.body).getBytes(StandardCharsets.UTF_8);
                contentType = "text/plain; charset=utf-8";
            } else {
                payload = Json.write(reply.body).getBytes(StandardCharsets.UTF_8);
                contentType = "application/json";
            }
            if (contentType != null) {
                exchange.getResponseHeaders().add("Content-Type", contentType);
            }
            for (Map.Entry<String, String> e : reply.headers.entrySet()) {
                exchange.getResponseHeaders().add(e.getKey(), e.getValue());
            }
            exchange.sendResponseHeaders(reply.status, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(payload);
                }
            }
        } finally {
            exchange.close();
        }
    }

    private static List<String[]> parseQuery(String rawQuery) {
        List<String[]> pairs = new ArrayList<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return pairs;
        }
        for (String part : rawQuery.split("&", -1)) {
            int eq = part.indexOf('=');
            String name = eq < 0 ? part : part.substring(0, eq);
            String value = eq < 0 ? "" : part.substring(eq + 1);
            pairs.add(new String[] {URLDecoder.decode(name, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8)});
        }
        return pairs;
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
