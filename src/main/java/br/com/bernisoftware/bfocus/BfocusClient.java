package br.com.bernisoftware.bfocus;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Cliente da API pública do bFocus. Thread-safe: crie UM por chave e reaproveite (ele mantém o pool de conexões).
 *
 * <pre>{@code
 * BfocusClient bf = new BfocusClient(System.getenv("BFOCUS_API_KEY"));
 * Customer c = bf.customers().upsert("ERP 1042",
 *         CustomerUpsert.builder().name("Padaria Estrela").email("contato@padaria.example").build());
 * }</pre>
 *
 * <p>Com opções:
 *
 * <pre>{@code
 * BfocusClient bf = BfocusClient.builder(apiKey)
 *         .baseUrl("http://localhost:8000")     // padrão: https://api.bfocus.com.br
 *         .timeout(Duration.ofSeconds(10))      // por tentativa; padrão 30 s
 *         .maxRetries(2)                        // novas tentativas além da primeira; 0 desliga
 *         .build();
 * }</pre>
 *
 * <p>Construir o cliente não faz nenhuma chamada de rede. Erros da API chegam como {@link BfocusException} (ou uma
 * subclasse) — decida pelo {@link BfocusException#getCode()}.
 */
public final class BfocusClient implements AutoCloseable {
    /** Versão desta SDK (a mesma de {@link Version#VERSION}). */
    public static final String VERSION = Version.VERSION;

    /** Endereço padrão da API. */
    public static final String DEFAULT_BASE_URL = "https://api.bfocus.com.br";

    /** Tempo limite padrão por tentativa. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /** Novas tentativas padrão (além da primeira). */
    public static final int DEFAULT_MAX_RETRIES = 2;

    /** Máximo de itens por chamada de {@code customers().batch} e {@code people().batch} (limite da API). */
    public static final int BATCH_MAX = 500;

    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;
    private final Transport transport;
    private final CustomersResource customers;
    private final PeopleResource people;
    private final ProductsResource products;
    private final ReleaseNotesResource releaseNotes;
    private final KbResource kb;
    private final AiAgentsResource aiAgents;

    /**
     * Cliente com as opções padrão.
     *
     * @param apiKey chave de API ({@code bf_live_…}), criada em Integrações → Chaves de API
     * @throws NullPointerException chave {@code null}
     * @throws IllegalArgumentException chave vazia ou só com espaços
     */
    public BfocusClient(String apiKey) {
        this(builder(apiKey));
    }

    private BfocusClient(Builder b) {
        this.baseUrl = b.baseUrl;
        this.timeout = b.timeout;
        this.maxRetries = b.maxRetries;
        this.transport = new Transport(b.apiKey, b.baseUrl, b.timeout, b.maxRetries, b.httpClient);
        this.customers = new CustomersResource(transport);
        this.people = new PeopleResource(transport);
        this.products = new ProductsResource(transport);
        this.releaseNotes = new ReleaseNotesResource(transport);
        this.kb = new KbResource(transport);
        this.aiAgents = new AiAgentsResource(transport);
    }

    /**
     * Builder com opções ({@code baseUrl}, {@code timeout}, {@code maxRetries}).
     *
     * @param apiKey chave de API
     * @return o builder
     * @throws NullPointerException chave {@code null}
     * @throws IllegalArgumentException chave vazia ou só com espaços
     */
    public static Builder builder(String apiKey) {
        return new Builder(apiKey);
    }

    /** @return clientes (com contatos, produtos vinculados e interações) */
    public CustomersResource customers() {
        return customers;
    }

    /** @return pessoas dos clientes (desde 0.2.0) */
    public PeopleResource people() {
        return people;
    }

    /** @return catálogo de produtos */
    public ProductsResource products() {
        return products;
    }

    /** @return release notes */
    public ReleaseNotesResource releaseNotes() {
        return releaseNotes;
    }

    /** @return base de conhecimento */
    public KbResource kb() {
        return kb;
    }

    /** @return agentes de IA */
    public AiAgentsResource aiAgents() {
        return aiAgents;
    }

    /** @return endereço da API (sem barra final) */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** @return tempo limite por tentativa */
    public Duration getTimeout() {
        return timeout;
    }

    /** @return novas tentativas além da primeira */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Assina a identidade do usuário para o widget — o mesmo que {@link WidgetIdentity#sign}. Local: sem rede e
     * sem chave de API.
     *
     * @param secret segredo do widget (nunca o envie ao navegador)
     * @param userExternalId {@code external_id} do usuário logado no seu sistema
     * @param customerExternalId {@code external_id} do cliente (empresa) desse usuário
     * @return HMAC-SHA256 em hexadecimal minúsculo
     */
    public static String signWidgetIdentity(String secret, String userExternalId, String customerExternalId) {
        return WidgetIdentity.sign(secret, userExternalId, customerExternalId);
    }

    /**
     * Assina a identidade v2 (com validade) — o mesmo que {@link WidgetIdentity#signV2(String, String, String)}.
     * Local: sem rede e sem chave de API.
     *
     * @param secret segredo do widget (nunca o envie ao navegador)
     * @param userExternalId {@code external_id} do usuário logado no seu sistema (sem {@code :})
     * @param customerExternalId {@code external_id} do cliente (empresa) desse usuário
     * @return {@code "v2.<ts>.<hex>"}, com o instante de agora
     */
    public static String signWidgetIdentityV2(String secret, String userExternalId, String customerExternalId) {
        return WidgetIdentity.signV2(secret, userExternalId, customerExternalId);
    }

    /**
     * Assina a identidade v2 num instante dado — o mesmo que
     * {@link WidgetIdentity#signV2(String, String, String, Instant)}.
     *
     * @param secret segredo do widget (nunca o envie ao navegador)
     * @param userExternalId {@code external_id} do usuário logado no seu sistema (sem {@code :})
     * @param customerExternalId {@code external_id} do cliente (empresa) desse usuário
     * @param now o instante da assinatura
     * @return {@code "v2.<ts>.<hex>"}
     */
    public static String signWidgetIdentityV2(String secret, String userExternalId, String customerExternalId, Instant now) {
        return WidgetIdentity.signV2(secret, userExternalId, customerExternalId, now);
    }

    /** Espera entre tentativas — substituível nos testes da SDK (a suíte não dorme de verdade). */
    void setSleeper(Transport.Sleeper sleeper) {
        transport.setSleeper(sleeper);
    }

    /**
     * Libera o cliente HTTP criado pela SDK (no Java 21+ encerra as threads dele na hora). Depois disso, as
     * chamadas lançam {@link IllegalStateException}. Opcional: um cliente só é descartado quando a aplicação
     * termina.
     */
    @Override
    public void close() {
        transport.close();
    }

    /** Não inclui a chave de API. */
    @Override
    public String toString() {
        return "BfocusClient{baseUrl=" + baseUrl + ", timeout=" + timeout + ", maxRetries=" + maxRetries
                + ", client=" + Version.CLIENT_ID + "}";
    }

    /** Builder de {@link BfocusClient}. */
    public static final class Builder {
        private final String apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private Duration timeout = DEFAULT_TIMEOUT;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private HttpClient httpClient;

        private Builder(String apiKey) {
            Objects.requireNonNull(apiKey, "apiKey");
            if (apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("A chave de API do bFocus não pode ser vazia.");
            }
            this.apiKey = apiKey;
        }

        /**
         * Endereço da API. Barra final é removida.
         *
         * @param baseUrl ex.: {@code https://api.bfocus.com.br} (padrão) ou {@code http://localhost:8000}
         * @return este builder
         * @throws IllegalArgumentException endereço que não é {@code http(s)://}
         */
        public Builder baseUrl(String baseUrl) {
            Objects.requireNonNull(baseUrl, "baseUrl");
            String url = baseUrl.trim();
            while (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            URI uri;
            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("baseUrl inválida: " + baseUrl, e);
            }
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
                throw new IllegalArgumentException("baseUrl precisa ser http(s)://host[:porta] — veio: " + baseUrl);
            }
            if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("baseUrl não pode ter query nem fragmento — veio: " + baseUrl);
            }
            this.baseUrl = url;
            return this;
        }

        /**
         * Tempo limite por tentativa.
         *
         * @param timeout positivo (padrão 30 s)
         * @return este builder
         * @throws IllegalArgumentException zero ou negativo
         */
        public Builder timeout(Duration timeout) {
            Objects.requireNonNull(timeout, "timeout");
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("timeout precisa ser positivo.");
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * Novas tentativas além da primeira, em erro de rede/tempo esgotado, 429, 502, 503 e 504.
         *
         * @param maxRetries ≥ 0 (padrão 2; {@code 0} desliga)
         * @return este builder
         * @throws IllegalArgumentException negativo
         */
        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries não pode ser negativo.");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * {@link HttpClient} próprio (proxy, SSL, executor…). A SDK não o fecha. Sem ele, a SDK cria um com
         * HTTP/1.1, {@code connectTimeout} = {@code timeout} e redirecionamento desligado.
         *
         * @param httpClient o cliente HTTP; {@code null} = o padrão da SDK
         * @return este builder
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Monta o cliente (sem nenhuma chamada de rede).
         *
         * @return o cliente
         */
        public BfocusClient build() {
            return new BfocusClient(this);
        }
    }
}
