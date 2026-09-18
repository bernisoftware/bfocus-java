import br.com.bernisoftware.bfocus.BfocusClient;
import br.com.bernisoftware.bfocus.BfocusException;
import br.com.bernisoftware.bfocus.CustomFieldInput;
import br.com.bernisoftware.bfocus.Customer;
import br.com.bernisoftware.bfocus.CustomerListParams;
import br.com.bernisoftware.bfocus.CustomerUpsert;
import br.com.bernisoftware.bfocus.KbSearchHit;
import br.com.bernisoftware.bfocus.Product;
import br.com.bernisoftware.bfocus.WidgetIdentity;

/**
 * bFocus — quickstart da SDK Java.
 *
 * <p>Rodar (Java 11+, arquivo único, sem compilar à parte):
 *
 * <pre>
 * mvn -q -DskipTests package                      # ou baixe o jar do Maven Central
 * BFOCUS_API_KEY=bf_live_... java -cp target/bfocus-0.1.0.jar examples/Quickstart.java
 * </pre>
 *
 * <p>Opcional: {@code BFOCUS_BASE_URL=http://localhost:8000} para apontar para a API local e
 * {@code BFOCUS_WIDGET_SECRET} para ver a assinatura do widget. A chave precisa dos escopos {@code customers:write},
 * {@code products:read} e {@code kb:read}.
 */
public final class Quickstart {
    private Quickstart() {
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("BFOCUS_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Defina BFOCUS_API_KEY (Integrações → Chaves de API no bFocus).");
            System.exit(2);
        }
        String baseUrl = System.getenv().getOrDefault("BFOCUS_BASE_URL", BfocusClient.DEFAULT_BASE_URL);

        try (BfocusClient bf = BfocusClient.builder(apiKey).baseUrl(baseUrl).build()) {
            // 1) Cliente: cria ou atualiza pelo id do SEU sistema. Só o que você informa muda.
            Customer customer = bf.customers().upsert("ERP 1042", CustomerUpsert.builder()
                    .name("Padaria Estrela")
                    .email("contato@padaria.example")
                    .customFields(CustomFieldInput.builder("plano").label("Plano").value("ouro").build())
                    .build());
            System.out.println("cliente: " + customer.getId() + " " + customer.getName());

            // 2) Registro no histórico do cliente.
            bf.customers().interactions().create("ERP 1042", "Cliente sincronizado pelo quickstart.");

            // 3) Catálogo de produtos.
            for (Product product : bf.products().list()) {
                System.out.println("produto: " + product.getSlug() + " - " + product.getName());
            }

            // 4) Busca na base de conhecimento.
            for (KbSearchHit hit : bf.kb().search("como emitir nota fiscal", null, 3)) {
                System.out.println("artigo: " + hit.getTitle());
            }

            // 5) Todos os clientes com "padaria" (percorre as páginas sozinho, sob demanda).
            long total = bf.customers().listAll(CustomerListParams.builder().q("padaria").build()).stream().count();
            System.out.println("clientes com 'padaria': " + total);
        } catch (BfocusException e) {
            // Decida pelo code (estável); informe o requestId ao suporte.
            System.err.println("erro " + e.getCode() + " (HTTP " + e.getStatus() + ") request_id=" + e.getRequestId());
            System.exit(1);
        }

        // 6) Identidade do widget: assinada no SEU backend, sem rede e sem chave de API.
        String secret = System.getenv("BFOCUS_WIDGET_SECRET");
        if (secret != null && !secret.isEmpty()) {
            System.out.println("assinatura do widget: " + WidgetIdentity.sign(secret, "USR-1", "ERP 1042"));
        }
    }
}
