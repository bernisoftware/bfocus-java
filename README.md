# bfocus-java

SDK oficial em **Java** da API pública do [bFocus](https://bfocus.com.br): clientes, produtos, release notes,
base de conhecimento e agentes de IA.

Zero dependências de runtime (JSON próprio, `java.net.http`) · Java 11+ · modelos tipados e imutáveis · novas
tentativas e idempotência automáticas.

## Instalação

Maven:

```xml
<dependency>
  <groupId>br.com.bernisoftware</groupId>
  <artifactId>bfocus</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle: `implementation("br.com.bernisoftware:bfocus:0.1.0")`

## Hello world

```java
import br.com.bernisoftware.bfocus.*;

BfocusClient bf = new BfocusClient("bf_live_...");

Customer cliente = bf.customers().upsert("ERP 1042",
        CustomerUpsert.builder().name("Padaria Estrela").email("contato@padaria.example").build());
System.out.println(cliente.getId() + " " + cliente.getName());
```

`upsert` cria ou atualiza pelo `external_id` do **seu** sistema — rodar de novo não duplica.

## Autenticação

Crie a chave no bFocus em **Integrações → Chaves de API**, marcando só os escopos de que a integração precisa.
Ela vai em `Authorization: Bearer <chave>` em toda requisição (a SDK cuida disso).

| Escopo | Permite |
| --- | --- |
| `customers:read` | Ler clientes, contatos, produtos vinculados e interações |
| `customers:write` | Cadastrar, atualizar e excluir clientes, contatos e interações |
| `products:read` | Ler o catálogo de produtos |
| `products:write` | Cadastrar, atualizar e arquivar produtos |
| `kb:read` | Ler e buscar artigos da base de conhecimento |
| `kb:write` | Criar, atualizar, publicar e excluir artigos da base de conhecimento |
| `ai_agents:read` | Ler os agentes de IA |
| `ai_agents:preview` | Testar a resposta de um agente de IA (consome IA da conta) |
| `release_notes:read` | Ler release notes |
| `release_notes:write` | Criar, atualizar e publicar release notes |

A chave legada (`bf_sk_…`) só alcança clientes (`customers:*`). Guarde a chave fora do código:

```java
BfocusClient bf = BfocusClient.builder(System.getenv("BFOCUS_API_KEY"))
        .baseUrl("https://api.bfocus.com.br")   // padrão; em dev: "http://localhost:8000"
        .timeout(Duration.ofSeconds(30))        // por tentativa
        .maxRetries(2)                          // novas tentativas além da primeira (0 desliga)
        .build();
```

Construir o cliente não faz nenhuma chamada de rede. Ele é thread-safe: crie **um** por chave e reaproveite (ele
mantém o pool de conexões). Chave vazia lança `IllegalArgumentException` na hora. Quer um proxy ou SSL próprio?
Passe o seu `java.net.http.HttpClient` em `.httpClient(...)`.

## Como os métodos funcionam

- **Retorno desembrulhado e tipado**: o método devolve o `data` da resposta como um modelo imutável
  (`Customer`, `Product`, `KbArticle`…) com getters. Campo novo que a API passar a devolver nunca vira erro: fica
  disponível em `getRaw("campo")`, `toMap()` e `toJson()` até a SDK ganhar o getter.
- **Listas paginadas** devolvem `Page<T>` (`getItems()`, `getPageNumber()`, `getPageSize()`, `getTotal()`,
  `getPages()`), que é `Iterable`. O número da página chama `getPageNumber()` porque um membro não pode se chamar
  como a classe; no JSON continua `page`. Para percorrer tudo, use `listAll(...)` (clientes, interações, release
  notes e artigos): um `Iterable` preguiçoso (também tem `stream()`) que busca página por página (`pageSize` padrão
  100) e para na última página ou numa página vazia.
- **Só o que você informa muda.** Os upserts são parciais e montados por builder: setter **não chamado** = campo
  omitido (fica como está); setter chamado com `null`, ou `clear("campo")`, = o campo vai como `null` e é
  **limpo**.

  ```java
  bf.customers().upsert("ERP 1042", CustomerUpsert.builder().phone("11 3333-4444").build()); // só o telefone muda
  bf.customers().upsert("ERP 1042", CustomerUpsert.builder().clear("phone").build());        // apaga o telefone
  ```

- Todo método tem uma sobrecarga com `RequestOptions` no fim: `timeout` só daquela chamada e, nas escritas,
  `idempotencyKey` (veja [Novas tentativas](#novas-tentativas-e-idempotência)).
- Datas (`updatedSince`) aceitam `Instant`, `OffsetDateTime` ou `ZonedDateTime` — enviados em ISO 8601 UTC com
  `Z` — ou `String`, que passa como veio. As datas das respostas são `OffsetDateTime`.

## Clientes

```java
bf.customers().upsert("ERP 1042", CustomerUpsert.builder()
        .name("Padaria Estrela")
        .document("12.345.678/0001-90")
        .customFields(CustomFieldInput.builder("plano").label("Plano").value("ouro").build()) // substitui a lista
        .build());

Customer cliente = bf.customers().get("ERP 1042");

Page<Customer> pagina = bf.customers().list(CustomerListParams.builder().q("padaria").page(1).pageSize(50).build());
System.out.println(pagina.getTotal() + " clientes");

// Sincronização incremental: tudo o que mudou desde a última rodada, todas as páginas.
Instant desde = Instant.now().minus(Duration.ofHours(1));
for (Customer c : bf.customers().listAll(CustomerListParams.builder().updatedSince(desde).build())) {
    System.out.println(c.getExternalId() + " " + c.getUpdatedAt());
}

bf.customers().delete("ERP 1042");
```

### Contatos, produtos vinculados e interações

```java
bf.customers().contacts().upsert("ERP 1042", "CT-1", ContactUpsert.builder()
        .name("Ana Souza").role("Financeiro").email("ana@padaria.example").isPrimary(true).build());
bf.customers().contacts().list("ERP 1042");
bf.customers().contacts().delete("ERP 1042", "CT-1");

bf.customers().products().attach("ERP 1042", "erp-cloud");
bf.customers().products().list("ERP 1042");
bf.customers().products().detach("ERP 1042", "erp-cloud");

bf.customers().interactions().create("ERP 1042",
        InteractionCreate.builder("Pedido 1042 faturado.").authorEmail("carla@suaempresa.com.br").build());
for (Interaction i : bf.customers().interactions().listAll("ERP 1042")) {
    System.out.println(i.getCreatedAt() + " " + i.getContent());
}
```

## Produtos

```java
bf.products().upsert("erp-cloud", ProductUpsert.builder()
        .name("ERP Cloud").description("Gestão na nuvem").color("#6366F1").build());
bf.products().get("erp-cloud");
bf.products().list(true);            // inclui os arquivados
bf.products().archive("erp-cloud");  // arquiva, não apaga
```

## Release notes — publicar direto do CI

Um passo no pipeline de release: cria ou atualiza a nota da versão e já publica.

```java
// PublicarReleaseNote.java — roda no CI a cada tag (escopo release_notes:write)
BfocusClient bf = new BfocusClient(System.getenv("BFOCUS_API_KEY"));
String versao = System.getenv("GITHUB_REF_NAME");   // "v2.3.0" — o "v" na frente é aceito

bf.releaseNotes().upsert("erp-cloud", versao, ReleaseNoteUpsert.builder()
        .title("Versão " + versao.replaceFirst("^v", ""))
        .descriptionMarkdown(Files.readString(Path.of("release-notes/" + versao + ".md")))
        .audience("external")   // "internal" | "external" | "both"
        .publish(true)          // cria/atualiza e publica numa chamada só
        .build());
```

Rodar de novo para a mesma versão atualiza a nota (é upsert). Também há:

```java
bf.releaseNotes().get("erp-cloud", "2.3.0");
bf.releaseNotes().list("erp-cloud", ReleaseNoteListParams.builder().published(false).build()); // rascunhos
bf.releaseNotes().publish("erp-cloud", "2.3.0");
```

## Base de conhecimento — sincronizar a partir de arquivos Markdown

Mantenha a documentação no repositório e sincronize a cada push. `batchUpsert` aceita **qualquer quantidade** de
artigos: a SDK divide em lotes de 100 (o limite da API), envia em sequência e devolve um único resultado.

```java
BfocusClient bf = new BfocusClient(System.getenv("BFOCUS_API_KEY"));   // escopos kb:read e kb:write
Path docs = Path.of("docs");

List<KbBatchArticle> artigos = new ArrayList<>();
try (Stream<Path> arquivos = Files.walk(docs)) {
    for (Path arquivo : (Iterable<Path>) arquivos.filter(p -> p.toString().endsWith(".md")).sorted()::iterator) {
        String texto = Files.readString(arquivo);
        String relativo = docs.relativize(arquivo).toString().replaceAll("\\.md$", "");
        String titulo = texto.lines().filter(l -> l.startsWith("# ")).map(l -> l.substring(2).trim())
                .findFirst().orElse(relativo);
        // id estável e SEM "/": o caminho do arquivo com ":" no lugar das barras.
        // Aceita letras, números e . _ : ~ @ + = -
        artigos.add(KbBatchArticle.builder("git:" + relativo.replace(File.separatorChar, ':'))
                .title(titulo)
                .bodyMarkdown(texto)
                .product("erp-cloud")   // ou .product(null) para um artigo global
                .build());
    }
}

KbBatchResult res = bf.kb().articles().batchUpsert(artigos);
System.out.printf("%d criados, %d atualizados, %d sem mudança, %d com falha%n",
        res.getCreated(), res.getUpdated(), res.getUnchanged(), res.getFailed());

for (KbBatchItemResult r : res.getResults()) {          // na mesma ordem enviada
    if (!r.isOk()) {
        System.out.println("falhou: " + r.getExternalId() + " " + r.getError()); // ex.: KB_ARTICLE_TITLE_REQUIRED
    } else if ("created".equals(r.getAction()) || "updated".equals(r.getAction())) {
        bf.kb().articles().publish(r.getExternalId());   // publica o que entrou ou mudou
    }
}

// Remove do bFocus o que saiu do repositório.
Set<String> locais = artigos.stream().map(KbBatchArticle::getExternalId).collect(Collectors.toSet());
for (KbArticleSummary a : bf.kb().articles().listAll(KbArticleListParams.builder().product("erp-cloud").build())) {
    String ext = a.getExternalId() == null ? "" : a.getExternalId();
    if (ext.startsWith("git:") && !locais.contains(ext)) {
        bf.kb().articles().delete(ext);
    }
}
```

Um item com problema não derruba os outros: ele volta com `isOk() == false` e o motivo em `getError()`. Para
publicar já no lote, use `.status("published")` em cada item. Artigo a artigo:

```java
bf.kb().articles().upsert("notion:emitir-nfse", KbArticleUpsert.builder()
        .title("Como emitir NFS-e")
        .bodyMarkdown("# Passo a passo\n\n1. Abra o menu **Fiscal**")
        .product(null)          // null explícito = artigo global
        .status("published")
        .build());
bf.kb().articles().get("notion:emitir-nfse");     // KbArticle, com getBodyHtml()
bf.kb().articles().list(KbArticleListParams.builder().status("draft").q("nota").build()); // Page de resumos
bf.kb().articles().unpublish("notion:emitir-nfse");
bf.kb().articles().delete("notion:emitir-nfse");
```

### Busca

```java
for (KbSearchHit hit : bf.kb().search("como emitir nota fiscal", "erp-cloud", 3)) {
    System.out.println(hit.getTitle() + " — " + hit.getExcerpt());
}
```

Base de conhecimento e agentes de IA exigem o módulo de **Atendimento**. Sem ele, a API responde 403
`MODULE_NOT_CONTRACTED` — um `PermissionDeniedException` comum.

## Agentes de IA

```java
List<AiAgent> agentes = bf.aiAgents().list();
AiAgent agente = bf.aiAgents().get(agentes.get(0).getId());

AiAgentPreview resposta = bf.aiAgents().preview(agente.getId(), "Como emito uma NFS-e?", List.of(
        AiAgentPreviewTurn.customer("Oi"),
        AiAgentPreviewTurn.bot("Olá! Como posso ajudar?")));
System.out.println(resposta.getAction() + " " + resposta.getAnswerHtml() + " " + resposta.getSources());
```

`preview` consome IA da conta (escopo `ai_agents:preview`).

## Erros

Qualquer resposta fora de 2xx lança `BfocusException` (unchecked) ou uma subclasse:

| Classe | Quando |
| --- | --- |
| `AuthenticationException` | 401 — chave ausente, inválida ou revogada |
| `PermissionDeniedException` | 403 — chave desligada, IP não liberado, escopo faltando (`getRequiredScope()`) ou módulo não contratado |
| `NotFoundException` | 404 |
| `ConflictException` | 409 — ex.: `KB_ARTICLE_EMPTY`, `AI_DISABLED` |
| `ValidationException` | 422 — motivos por campo em `getValidation()` |
| `RateLimitException` | 429 — `getRetryAfter()` (depois de esgotar as novas tentativas) |
| `ServerException` | 5xx |
| `NetworkException` | conexão/tempo esgotado — `getStatus() == 0`, `getCode()` = `"NETWORK_ERROR"` |

**Decida pelo `getCode()`** — ele é estável (`CUSTOMER_NOT_FOUND`, `INTEGRATION_SCOPE_MISSING`,
`VALIDATION_ERROR`…). A mensagem é texto para humanos e pode mudar. Ao falar com o suporte, informe o
`getRequestId()`: ele vem do corpo da resposta, senão do header `X-Request-Id`, senão é o id que a própria SDK
enviou (a API ecoa o do cliente) — então está sempre preenchido, inclusive em `NetworkException`.

Se uma resposta 2xx chegar sem o envelope JSON da API (um proxy devolvendo HTML, corpo vazio), a SDK não devolve
`null` calado: lança `BfocusException` com `getCode()` = `"INVALID_RESPONSE"` e o status recebido. Corpo de erro que
não é JSON vira `"HTTP_<status>"`.

```java
try {
    bf.customers().get("ERP 9999");
} catch (NotFoundException e) {
    System.out.println("não existe");
} catch (BfocusException e) {
    switch (e.getCode()) {
        case "INTEGRATION_SCOPE_MISSING":
            System.out.println("a chave não tem o escopo " + e.getRequiredScope());
            break;
        case "VALIDATION_ERROR":
            System.out.println(e.getValidation());   // {email=value is not a valid email address}
            break;
        default:
            System.out.println(e.getCode() + " " + e.getStatus() + " " + e.getRequestId());
    }
}
```

Argumento inválido no seu código (chave vazia; parâmetro de caminho vazio, `"."` ou `".."`; `/` no `external_id`
de um artigo) lança `IllegalArgumentException`/`NullPointerException` na hora, sem chamar a API.

## Novas tentativas e idempotência

A SDK tenta de novo sozinha em **erro de rede/tempo esgotado, 429, 502, 503 e 504** — até `maxRetries` vezes
(padrão 2). Espera o `Retry-After` quando a API manda (segundos ou data HTTP, teto de 60 s); senão 0,5 s, 1 s,
2 s… (teto de 8 s) + até 25% de variação aleatória. Um 500 ou outro 4xx volta na hora.

Toda escrita (POST/PUT/DELETE) leva um `Idempotency-Key`, e **a mesma chave vai em todas as tentativas** da
chamada: se a primeira chegou a executar e só a resposta se perdeu, a API devolve a resposta original
(`Idempotent-Replayed: true`) em vez de executar de novo. O `X-Request-Id` também se repete, para o suporte ver as
tentativas como uma chamada só.

Para que a proteção valha também quando o **seu** processo roda de novo (um job reexecutado), passe uma chave
derivada do evento:

```java
bf.customers().interactions().create("ERP 1042", "Pedido 1042 faturado.",
        RequestOptions.idempotencyKey("pedido-1042-faturado"));
```

A mesma chave com outra requisição volta `IDEMPOTENCY_KEY_REUSED`. No `batchUpsert`, o 1º lote usa a sua chave
como veio e os seguintes `"<chave>:2"`, `"<chave>:3"`… (sem chave, cada lote gera a sua).

## Identidade do widget

Para o widget de atendimento reconhecer o usuário logado, o **seu backend** assina a identidade dele com o segredo
do widget (que nunca vai para o navegador). É local — sem rede e sem chave de API:

```java
String assinatura = WidgetIdentity.sign(
        System.getenv("BFOCUS_WIDGET_SECRET"),
        "USR-1",       // o usuário no seu sistema
        "ERP 1042");   // a empresa (cliente) dele
// HMAC-SHA256 em hex minúsculo de "v1:USR-1:ERP 1042" — entregue junto dos dois ids à página que abre o widget.
```

`BfocusClient.signWidgetIdentity(...)` faz o mesmo.

## Versões

**Fixe a versão exata** (`<version>0.1.0</version>`, sem faixas nem `LATEST`) e suba de uma versão para a outra de
propósito. Cada release declara se muda a superfície pública (`additive` ou `breaking: …`), então dá para saber o
que revisar antes de subir.

A SDK se identifica em toda requisição (`X-Bfocus-Client: bfocus-java/<versão>`, também em
`BfocusClient.VERSION`): quando uma correção exigir atualizar, o bFocus avisa as contas que rodam a versão afetada.

## Exemplo

Um programa rodável está em [`examples/Quickstart.java`](examples/Quickstart.java) (Java 11+ roda o arquivo
direto, sem compilar à parte):

```bash
mvn -q -DskipTests package
BFOCUS_API_KEY=bf_live_... java -cp target/bfocus-0.1.0.jar examples/Quickstart.java
```

## Desenvolvimento

```bash
mvn verify                  # conformidade + unitários + versão
mvn -P artifact verify      # a mesma suíte contra o jar empacotado
```

Os casos de conformidade (`src/test/resources/conformance/cases.json`) são gerados no monorepo do bFocus — não edite
à mão.

## Licença

MIT © Berni Software
