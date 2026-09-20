package br.com.bernisoftware.bfocus;

import br.com.bernisoftware.bfocus.internal.Json;
import br.com.bernisoftware.bfocus.internal.JsonException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Transporte HTTP: headers, novas tentativas (com o MESMO X-Request-Id/Idempotency-Key), tempo limite por
 * tentativa e conversão de erro em {@link BfocusException}.
 */
final class Transport implements AutoCloseable {
    static final String API_PREFIX = "/api/v1/integration";
    static final Duration MAX_RETRY_AFTER = Duration.ofSeconds(60);
    private static final Pattern SECONDS = Pattern.compile("\\d+(\\.\\d+)?");

    /** Espera entre tentativas. Substituível nos testes (a suíte não pode dormir de verdade). */
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    static final Sleeper REAL_SLEEP = duration -> {
        long millis = duration.toMillis();
        if (millis > 0) {
            Thread.sleep(millis);
        }
    };

    private final HttpClient http;
    private final boolean ownsHttp;
    private final String authorization;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;
    private volatile Sleeper sleeper = REAL_SLEEP;
    private volatile boolean closed;

    Transport(String apiKey, String baseUrl, Duration timeout, int maxRetries, HttpClient http) {
        this.authorization = "Bearer " + apiKey;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        if (http != null) {
            this.http = http;
            this.ownsHttp = false;
        } else {
            // HTTP/1.1: o mais compatível com proxies corporativos; redirecionamento desligado (um 301 num POST
            // viraria GET em silêncio).
            this.http = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            this.ownsHttp = true;
        }
    }

    void setSleeper(Sleeper sleeper) {
        this.sleeper = sleeper == null ? REAL_SLEEP : sleeper;
    }

    int maxRetries() {
        return maxRetries;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        // HttpClient só é AutoCloseable a partir do Java 21; antes disso as threads dele morrem com o GC.
        if (ownsHttp && http instanceof AutoCloseable) {
            try {
                ((AutoCloseable) http).close();
            } catch (Exception ignored) {
                // nada a fazer
            }
        }
    }

    // ── chamadas ─────────────────────────────────────────────────────────────────────────────────────────

    <T> T call(String method, String path, Query query, Object body, RequestOptions options, Function<Object, T> decode) {
        Envelope envelope = send(method, path, query, body, options);
        Object data = envelope.data();
        try {
            return decode.apply(data);
        } catch (InvalidDataException e) {
            throw invalidResponse(envelope.status, envelope.requestId, "formato inesperado em 'data': " + e.getMessage(), e);
        }
    }

    <T> Page<T> page(String path, Query query, RequestOptions options, Function<Object, T> item) {
        Envelope envelope = send("GET", path, query, null, options);
        Object data = envelope.data();
        try {
            List<T> items = Wire.list(data, item, "data");
            int page = 1;
            int pageSize = items.size();
            int total = items.size();
            int pages = 1;
            Object pagination = envelope.root.get("pagination");
            if (pagination instanceof Map) {
                Wire p = Wire.of(pagination, "pagination");
                page = p.any("page") == null ? page : p.integer("page");
                pageSize = p.any("page_size") == null ? pageSize : p.integer("page_size");
                total = p.any("total") == null ? total : p.integer("total");
                pages = p.any("pages") == null ? pages : p.integer("pages");
            }
            return new Page<>(items, page, pageSize, total, pages);
        } catch (InvalidDataException e) {
            throw invalidResponse(envelope.status, envelope.requestId, "formato inesperado em 'data': " + e.getMessage(), e);
        }
    }

    private Envelope send(String method, String path, Query query, Object body, RequestOptions options) {
        if (closed) {
            throw new IllegalStateException("Este BfocusClient já foi fechado (close()).");
        }
        URI uri = URI.create(baseUrl + API_PREFIX + path + (query == null ? "" : query.toString()));
        byte[] payload = body == null ? null : Json.write(body).getBytes(StandardCharsets.UTF_8);

        // Gerados UMA vez por chamada lógica e repetidos em toda nova tentativa: é o que torna a repetição
        // segura (a API devolve a resposta original com Idempotent-Replayed: true).
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String idempotencyKey = null;
        if (isWrite(method)) {
            String own = options == null ? null : options.getIdempotencyKey();
            idempotencyKey = own != null ? own : UUID.randomUUID().toString();
        }
        Duration attemptTimeout = options != null && options.getTimeout() != null ? options.getTimeout() : timeout;
        HttpRequest request = buildRequest(method, uri, payload, requestId, idempotencyKey, attemptTimeout);

        for (int attempt = 0; ; attempt++) {
            BfocusException error;
            Duration retryAfter = null;
            try {
                HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
                int status = response.statusCode();
                byte[] raw = response.body();
                String text = raw == null ? "" : new String(raw, StandardCharsets.UTF_8);
                String headerRequestId = header(response, "X-Request-Id");
                // request_id: corpo → header X-Request-Id → o que a SDK enviou (a API ecoa o do cliente).
                String fallbackRequestId = headerRequestId != null ? headerRequestId : requestId;
                if (status >= 200 && status < 300) {
                    return Envelope.parse(text, status, fallbackRequestId);
                }
                retryAfter = parseRetryAfter(header(response, "Retry-After"));
                error = fromResponse(status, text, fallbackRequestId, header(response, "X-Required-Scope"),
                        header(response, "X-Required-Module"), retryAfter);
            } catch (HttpTimeoutException e) {
                error = new NetworkException(NetworkException.NETWORK_ERROR + ": tempo esgotado após " + seconds(attemptTimeout)
                        + " s em " + method + " " + path + " (request_id " + requestId + ")", requestId, e);
            } catch (IOException e) {
                error = new NetworkException(NetworkException.NETWORK_ERROR + ": falha de conexão em " + method + " " + path
                        + ": " + e + " (request_id " + requestId + ")", requestId, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NetworkException(NetworkException.NETWORK_ERROR + ": chamada interrompida em " + method + " " + path
                        + " (request_id " + requestId + ")", requestId, e);
            }

            if (attempt >= maxRetries || !isRetryable(error)) {
                throw error;
            }
            try {
                sleeper.sleep(backoff(attempt, retryAfter));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw error;   // interrompida durante a espera: devolve o último erro real
            }
        }
    }

    private HttpRequest buildRequest(String method, URI uri, byte[] payload, String requestId, String idempotencyKey, Duration attemptTimeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(attemptTimeout)
                .header("Authorization", authorization)
                .header("Accept", "application/json")
                .header("X-Bfocus-Client", Version.CLIENT_ID)
                .header("User-Agent", Version.CLIENT_ID)
                .header("X-Request-Id", requestId);
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        if (payload != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofByteArray(payload));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return builder.build();
    }

    /** Espera antes da nova tentativa {@code attempt} (0 = a primeira nova tentativa). */
    static Duration backoff(int attempt, Duration retryAfter) {
        if (retryAfter != null) {
            if (retryAfter.isNegative()) {
                return Duration.ZERO;
            }
            return retryAfter.compareTo(MAX_RETRY_AFTER) > 0 ? MAX_RETRY_AFTER : retryAfter;
        }
        double seconds = Math.min(8.0, 0.5 * Math.pow(2, attempt));
        double jitter = ThreadLocalRandom.current().nextDouble() * 0.25 * seconds;
        return Duration.ofNanos(Math.round((seconds + jitter) * 1_000_000_000L));
    }

    private static boolean isWrite(String method) {
        return method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE");
    }

    private static boolean isRetryable(BfocusException error) {
        int s = error.getStatus();
        return error instanceof NetworkException || s == 429 || s == 502 || s == 503 || s == 504;
    }

    private static String header(HttpResponse<?> response, String name) {
        for (String value : response.headers().allValues(name)) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    /** {@code Retry-After} em segundos (inteiro ou decimal) ou data HTTP. {@code null} se ausente/ilegível. */
    static Duration parseRetryAfter(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (SECONDS.matcher(value).matches()) {
            double seconds = Double.parseDouble(value);
            return Duration.ofMillis(Math.round(Math.min(seconds, 86_400) * 1000));
        }
        try {
            ZonedDateTime date = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration delta = Duration.between(ZonedDateTime.now(date.getZone()), date);
            return delta.isNegative() ? Duration.ZERO : delta;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String seconds(Duration d) {
        return String.format(Locale.ROOT, "%.3f", d.toMillis() / 1000.0).replaceAll("\\.?0+$", "");
    }

    // ── erros ────────────────────────────────────────────────────────────────────────────────────────────

    /** Converte uma resposta fora de 2xx no {@link BfocusException} certo (BRIEF §4). */
    static BfocusException fromResponse(int status, String text, String fallbackRequestId, String requiredScope,
                                        String requiredModule, Duration retryAfter) {
        String error = null;
        String bodyMessage = null;
        String bodyRequestId = null;
        Map<String, String> validation = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        if (!text.trim().isEmpty()) {
            try {
                Object parsed = Json.parse(text);
                if (parsed instanceof Map) {
                    Map<?, ?> root = (Map<?, ?>) parsed;
                    error = nonEmptyString(root.get("error"));
                    bodyMessage = nonEmptyString(root.get("message"));
                    bodyRequestId = nonEmptyString(root.get("request_id"));
                    // `data`: o detalhe estruturado do erro. A API também o repete em `validation`, mas quem
                    // lê o erro precisa alcançá-lo sem depender dessa duplicação.
                    Object d = root.get("data");
                    if (d instanceof Map) {
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) d).entrySet()) {
                            data.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                    }
                    Object v = root.get("validation");
                    if (v instanceof Map) {
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) v).entrySet()) {
                            Object reason = entry.getValue();
                            validation.put(String.valueOf(entry.getKey()), reason instanceof String ? (String) reason : Json.write(reason));
                        }
                    }
                }
            } catch (JsonException ignored) {
                // Corpo não-JSON (proxy, balanceador…): cai no HTTP_<status> abaixo.
            }
        }

        String code = error != null ? error : bodyMessage != null ? bodyMessage : "HTTP_" + status;
        String requestId = bodyRequestId != null ? bodyRequestId : fallbackRequestId;
        Duration exposedRetryAfter = status == 429 ? retryAfter : null;

        StringBuilder message = new StringBuilder(code);
        if (bodyMessage != null && !bodyMessage.equals(code)) {
            message.append(": ").append(bodyMessage);
        }
        message.append(" (HTTP ").append(status);
        if (requiredScope != null) {
            message.append("; escopo exigido: ").append(requiredScope);
        }
        if (requiredModule != null) {
            message.append("; módulo exigido: ").append(requiredModule);
        }
        for (Map.Entry<String, String> entry : validation.entrySet()) {
            message.append("; ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        if (exposedRetryAfter != null) {
            message.append("; tente de novo em ").append(seconds(exposedRetryAfter)).append(" s");
        }
        if (requestId != null) {
            message.append("; request_id ").append(requestId);
        }
        message.append(')');
        String text2 = message.toString();

        if (status == 401) {
            return new AuthenticationException(code, status, text2, requestId, validation, data, exposedRetryAfter, requiredScope, null);
        } else if (status == 403) {
            return new PermissionDeniedException(code, status, text2, requestId, validation, data, exposedRetryAfter, requiredScope, null);
        } else if (status == 404) {
            return new NotFoundException(code, status, text2, requestId, validation, data, exposedRetryAfter, requiredScope, null);
        } else if (status == 409) {
            return new ConflictException(code, status, text2, requestId, validation, data, exposedRetryAfter, requiredScope, null);
        } else if (status == 422) {
            return new ValidationException(code, status, text2, requestId, validation, data, exposedRetryAfter, requiredScope, null);
        } else if (status == 429) {
            return new RateLimitException(code, status, text2, requestId, validation, data, exposedRetryAfter, requiredScope, null);
        } else if (status >= 500 && status <= 599) {
            return new ServerException(code, status, text2, requestId, validation, data, exposedRetryAfter, requiredScope, null);
        }
        return new BfocusException(code, status, text2, requestId, validation, data, exposedRetryAfter, requiredScope, null);
    }

    static BfocusException invalidResponse(int status, String requestId, String reason, Throwable cause) {
        return new BfocusException(BfocusException.INVALID_RESPONSE, status,
                BfocusException.INVALID_RESPONSE + ": " + reason + " (HTTP " + status
                        + (requestId == null ? "" : "; request_id " + requestId) + ")",
                requestId, null, null, null, cause);
    }

    private static String nonEmptyString(Object value) {
        return value instanceof String && !((String) value).isEmpty() ? (String) value : null;
    }

    /** Envelope de sucesso {@code {"code", "data", "message"[, "pagination"]}}. */
    static final class Envelope {
        final Map<?, ?> root;
        final int status;
        final String requestId;

        private Envelope(Map<?, ?> root, int status, String requestId) {
            this.root = root;
            this.status = status;
            this.requestId = requestId;
        }

        static Envelope parse(String text, int status, String fallbackRequestId) {
            Object parsed;
            try {
                parsed = Json.parse(text);
            } catch (JsonException e) {
                throw invalidResponse(status, fallbackRequestId, "o corpo da resposta não é JSON", e);
            }
            if (!(parsed instanceof Map)) {
                throw invalidResponse(status, fallbackRequestId, "o corpo da resposta não é um objeto JSON", null);
            }
            Map<?, ?> root = (Map<?, ?>) parsed;
            String bodyRequestId = nonEmptyString(root.get("request_id"));
            return new Envelope(root, status, bodyRequestId != null ? bodyRequestId : fallbackRequestId);
        }

        Object data() {
            Object data = root.get("data");
            if (data == null) {
                throw invalidResponse(status, requestId, "a resposta não trouxe 'data'", null);
            }
            return data;
        }
    }
}
