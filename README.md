# bfocus-java

SDK oficial em **Java** da API pública do [bFocus](https://bfocus.com.br): clientes, pessoas dos clientes, produtos,
release notes, base de conhecimento e agentes de IA.

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
| `customers:read` | Ler clientes, contatos, pessoas, produtos vinculados e interações |
| `customers:write` | Cadastrar, atualizar e excluir clientes, contatos, pessoas, interações e identificadores extras; lotes de clientes e pessoas |
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
## Pessoas

Pessoa = quem usa o sistema do seu cliente e abre chamados/conversas. O `external_id` dela é o mesmo
`user.externalId` que o widget recebe — por isso **não pode ter `:`** (use `-`, ex.: `app-77`).

```java
PersonUpsertResult p = bf.people().upsert("erp-1042", "app-77", PersonUpsert.builder()
        .name("Paula Reis")
        .email("paula@padaria.example")
        .role("Financeiro")
        .isPrimary(true)
        .extraEmails("paula.reis@pessoal.example")   // somam aos e-mails que ela já tem
        .build());
System.out.println(p.getStatus());   // "created", "updated" ou "unchanged"

for (Person pessoa : bf.people().list("erp-1042")) {
    System.out.println(pessoa.getName() + " acesso=" + pessoa.hasAccess());
}

bf.people().delete("erp-1042", "app-77");   // retira o acesso; devolve a pessoa com hasAccess() == false
bf.people().upsert("erp-1042", "app-77", PersonUpsert.builder().access(true).build());   // devolve o acesso
```

- O **e-mail (ou o telefone)** acha a pessoa que já chegou por e-mail ou por outro sistema: ela é **adotada**
  (passa a ter o seu `external_id`), nunca duplicada.
- A mesma pessoa enviada com **outro cliente** é **transferida** para ele.
- `delete` não apaga: retira o acesso ao widget/portal. A pessoa continua no histórico (chamados, conversas).
- Como nos outros upserts, só o que você informa muda (setter não chamado = omitido; `null`/`clear(...)` = vai
  como `null`).

### Campos personalizados da pessoa

`customFields` leva o que só existe no seu sistema (matrícula, centro de custo, filial). É a
**exceção** ao "só o que vier muda": a lista enviada **substitui a lista inteira** — campo que
ficar de fora é **removido**. Mande sempre a lista que o seu sistema tem hoje; não chamar o setter não mexe
em nada, como em qualquer outro campo.

A `visibility` é decidida no bFocus e **preservada entre sincronizações** — por isso ela não vai
no envio, só volta na resposta: o seu ERP não rebaixa nem promove a exposição de um dado sem
querer.

Vale no upsert de pessoa, no lote de pessoas e na listagem de pessoas do cliente.

```java
PersonUpsertResult p = bf.people().upsert("erp-1042", "app-77", PersonUpsert.builder()
        .customFields(   // a lista INTEIRA do seu sistema
                CustomFieldInput.builder("matricula").label("Matrícula").value("4471").build(),
                CustomFieldInput.builder("filial").label("Filial").value("Centro").build())
        .build());

for (CustomField campo : p.getCustomFields()) {
    System.out.println(campo.getKey() + " " + campo.getValue() + " " + campo.getVisibility());
}
```

### Apagar o e-mail ou o telefone da pessoa

Um contato gravado errado ficava preso para sempre: enquanto a ficha errada segurasse o
telefone, nenhum reenvio o soltava. `erase(...)` apaga (é o campo `clear` da API pública).

```java
bf.people().upsert("erp-1042", "app-77", PersonUpsert.builder()
        .erase("phone")          // ou .erase("email", "phone")
        .build());
```

Três regras que parecem contraintuitivas e são de propósito:

- **Apagar é explícito, e `erase` é o único jeito.** `.phone(null)` e `.clear("phone")` mandam
  `phone: null`, e em pessoa `null` (como a lista vazia e não chamar o setter) quer dizer **"não
  mexe"** — a SDK não traduz `null` em apagar. Fazer o `null` apagar teria apagado, em silêncio e
  na primeira carga seguinte, o dado de todo sistema que manda `null` para "não tenho esse valor".
- **Campo fora da lista é recusado, não ignorado**: hoje só `"email"` e `"phone"`; qualquer outro
  devolve 422 `PERSON_CLEAR_FIELD_INVALID` (`ValidationException`).
- **Só se limpa a própria ficha.** Se você alcançou a pessoa por um identificador **extra**, a API
  recusa com 409 `PERSON_CLEAR_NOT_OWN_RECORD` (`ConflictException`): apagar o contato de uma ficha
  alcançada por apelido seria apagar dado de outro sistema. Para saber se o id que você tem em mãos
  é o principal ou um extra, use `bf.people().identifiers().list(...)`.

Vale no `people().upsert(...)` e no `people().batch(...)`.

### Contato já usado: um 409 que você consegue resolver

`PERSON_EMAIL_TAKEN` e `PERSON_PHONE_TAKEN` (409) não são "tente de novo": o e-mail (ou o
telefone) já é de outra pessoa da conta. O erro diz **de quem**, em `getData()` (a API repete o mesmo
detalhe em `getValidation()`, por compatibilidade):

| campo | o que é |
| --- | --- |
| `field` | `email` ou `phone` — qual contato está tomado |
| `owner_external_id` | o identificador da pessoa que já usa esse contato |
| `owner_name` | o nome dela |
| `owner_customer_external_id` | o cliente a que ela pertence |

**É o `owner_customer_external_id` que decide a ação**, e os dois casos pedem coisas opostas:

- **mesmo cliente que você enviou** → é quase sempre a MESMA pessoa em dois sistemas. Uma pessoa
  tem **N identificadores**: registre o seu como **extra** dela. A partir daí o seu id encontra
  essa pessoa.
- **outro cliente** → ninguém decide sozinho a quem a pessoa pertence. Não force: registre o caso
  e leve para quem conhece o cadastro. Unificar dois clientes é decisão de gente, não de um
  casamento por e-mail.

```java
try {
    bf.people().upsert("erp-1042", "app-77", PersonUpsert.builder()
            .name("Paula Reis").email("paula@padaria.example").build());
} catch (ConflictException e) {
    if (!"PERSON_EMAIL_TAKEN".equals(e.getCode()) && !"PERSON_PHONE_TAKEN".equals(e.getCode())) {
        throw e;
    }
    Map<String, Object> dono = e.getData();
    if ("erp-1042".equals(dono.get("owner_customer_external_id"))) {
        // A mesma pessoa, com dois ids: o seu vira mais um identificador dela.
        bf.people().identifiers().add((String) dono.get("owner_external_id"), "app-77", "ERP");
    } else {
        // Dono em OUTRO cliente: não decida sozinho — registre e leve para o cadastro.
        avisarCadastro(e.getCode(), dono);
    }
}
```

`PERSON_CONTACT_OTHER_CUSTOMER` (409) é o mesmo assunto pelo outro lado, e é **recusa
definitiva**: a API não move mais uma pessoa de um cliente para outro só porque o e-mail (ou o
telefone) casou. Repetir a chamada não resolve — trate como caso para o cadastro, nunca como
falha temporária.

## Lotes — clientes e pessoas

`bf.customers().batch(...)` e `bf.people().batch(...)` gravam até **500 itens por chamada**
(`BfocusClient.BATCH_MAX`). Acima disso a SDK lança `IllegalArgumentException` **antes** de qualquer requisição —
ela não divide sozinha, porque o `index` de cada resultado é a posição no lote que **você** enviou. Divida assim:

```java
List<CustomerBatchItem> todos = new ArrayList<>();
for (Cliente c : meusClientes) {                        // o seu modelo
    todos.add(CustomerBatchItem.of("erp-" + c.id, CustomerUpsert.builder()
            .name(c.nome).document(c.cnpj).email(c.email).build()));
}
for (int i = 0; i < todos.size(); i += BfocusClient.BATCH_MAX) {
    List<CustomerBatchItem> fatia = todos.subList(i, Math.min(i + BfocusClient.BATCH_MAX, todos.size()));
    BatchResult r = bf.customers().batch(fatia);
    for (BatchItemResult item : r.getResults()) {
        if (item.isError()) {
            CustomerBatchItem enviado = fatia.get(item.getIndex());   // index = posição NESTA fatia
            log.warn("cliente {} não gravou: {} (HTTP {})", enviado.getExternalId(), item.getError(), item.getCode());
        }
    }
}
```

`CustomerBatchItem.of(externalId, CustomerUpsert)` e `PersonBatchItem.of(customerExternalId, externalId, PersonUpsert)`
reaproveitam os mesmos corpos do upsert (o código que monta o `CustomerUpsert` do dia a dia serve para a carga).

Cada resultado (`BatchItemResult`) traz `getIndex()`, `getStatus()` (`created`, `updated`, `unchanged` ou
`error`), `getExternalId()`, `getMergedInto()` (o id enviado era um identificador extra: este é o principal do
cadastro — atualize do seu lado), `getError()` (código estável, ex.: `NAME_REQUIRED`) e `getCode()` (o status HTTP
que o item teria sozinho). `getSummary()` soma `created`, `updated`, `unchanged` e `error`. **Um item com erro não
desfaz os outros.** Lista vazia devolve o resultado zerado sem fazer requisição.

## Identificadores extras

Liga o id de **outro sistema seu** (CRM, loja…) ao mesmo cadastro, que passa a ser achado por qualquer um deles:

```java
CustomerWithIdentifiers c = bf.customers().identifiers().add("erp-1042", "crm-88", "CRM");   // rótulo opcional
c.getIdentifiers();                                              // [crm-88 (CRM, api)]
bf.customers().identifiers().remove("erp-1042", "crm-88");

bf.people().identifiers().add("app-77", "crm-p5");              // sem rótulo: sem corpo
bf.people().identifiers().remove("app-77", "crm-p5");
```

É idempotente (ligar de novo não muda nada). Se o id já é de **outro** cadastro, a API devolve 409
`IDENTIFIER_IN_USE` (`ConflictException`).

### Ler os identificadores da pessoa (para reconciliar)

`bf.people().list(...)` mostra só o identificador **principal** de cada pessoa. Quando dois cadastros
seus eram a mesma pessoa, um dos ids virou **extra** — e some da listagem sem ter sumido do cadastro.
É isso que faz a sua conferência fechar "633 de 636" sem explicar os 3.

`identifiers().list(...)` é a fonte de verdade dessa conferência, e é **leitura**: antes dela era
preciso ESCREVER (tentar um `add`) para descobrir o que tinha acontecido. Aceita no caminho o id
principal **ou qualquer um dos extras**.

```java
PersonIdentifiers ids = bf.people().identifiers().list("crm-p5");   // o id extra que "sumiu"
System.out.println(ids.getExternalId());                            // "app-77" — o principal
for (Identifier i : ids.getIdentifiers()) {
    System.out.println(i.getExternalId() + " " + i.getLabel() + " " + i.getSource());
}
```

## Sincronizar clientes e usuários do seu sistema

**Ids com o prefixo do sistema, sem `:`.** A assinatura do widget recusa `:`, então use `-` como separador —
`erp-1042` para clientes, `app-77` para pessoas — ou UUIDs puros. Assim vários sistemas seus convivem no mesmo
bFocus sem colisão.

**Carga inicial (no deploy):** clientes em fatias de 500 → vincule cada cliente ao produto → pessoas em fatias de
500. Confira `getSummary().getError()` e registre os itens com erro.

```java
void cargaInicial(BfocusClient bf, List<CustomerBatchItem> clientes, List<PersonBatchItem> pessoas) {
    for (int i = 0; i < clientes.size(); i += BfocusClient.BATCH_MAX) {
        List<CustomerBatchItem> fatia = clientes.subList(i, Math.min(i + BfocusClient.BATCH_MAX, clientes.size()));
        registrarErros("clientes", bf.customers().batch(fatia));
        for (CustomerBatchItem c : fatia) {
            bf.customers().products().attach(c.getExternalId(), "erp-cloud");   // liga o cliente ao produto
        }
    }
    for (int i = 0; i < pessoas.size(); i += BfocusClient.BATCH_MAX) {
        registrarErros("pessoas", bf.people().batch(pessoas.subList(i, Math.min(i + BfocusClient.BATCH_MAX, pessoas.size()))));
    }
}

void registrarErros(String tipo, BatchResult r) {
    for (BatchItemResult item : r.getResults()) {
        if (item.isError()) {
            log.warn("{} #{} ({}): {}", tipo, item.getIndex(), item.getExternalId(), item.getError());
        }
    }
}
```

**Depois, no dia a dia**, espelhe cada evento do seu sistema:

| No seu sistema | No bFocus |
| --- | --- |
| criou/alterou cliente | `bf.customers().upsert(id, ...)` |
| criou/alterou usuário | `bf.people().upsert(clienteId, usuarioId, ...)` |
| excluiu/desativou usuário | `bf.people().delete(clienteId, usuarioId)` |
| excluiu cliente | `bf.customers().delete(id)` |
| cliente passou a usar um produto | `bf.customers().products().attach(id, slug)` |

Se um resultado de lote trouxer `getMergedInto()`, atualize o id do seu lado.

**Nunca bloqueie a requisição do seu usuário esperando o bFocus.** Grave o evento numa fila (job/outbox) e mande
de lá, com novas tentativas e backoff. A SDK já repete 429/5xx com a mesma `Idempotency-Key`; a fila cobre as
indisponibilidades longas:

```java
// No seu serviço: só enfileira (mesma transação do banco, se for outbox).
outbox.enfileirar("bfocus.person.upsert", usuario.getId());

// No worker: executa; se lançar, a fila tenta de novo mais tarde.
void processar(Evento ev) {
    Usuario u = usuarios.buscar(ev.id());
    bf.people().upsert("erp-" + u.getEmpresaId(), "app-" + u.getId(),
            PersonUpsert.builder().name(u.getNome()).email(u.getEmail()).build(),
            RequestOptions.idempotencyKey("person-upsert-" + ev.id()));
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

Além de `getCode()`, `getStatus()`, `getRequestId()`, `getValidation()`, `getRetryAfter()` e
`getRequiredScope()`, a exceção tem **`getData()`**: o `data` do corpo, com o detalhe estruturado que alguns erros
trazem (vazio quando não há). É por ele que um 409 de contato tomado diz de **quem** é o contato — veja
[Pessoas](#pessoas).

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
de um artigo; mais de 500 itens num `batch`; `:` no id do usuário da identidade v2) lança
`IllegalArgumentException`/`NullPointerException` na hora, sem chamar a API.

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
como veio e os seguintes `"<chave>:2"`, `"<chave>:3"`… (sem chave, cada lote gera a sua). `customers().batch` e
`people().batch` são uma chamada só: a chave vale para o lote inteiro.

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

### Identidade v2 (com validade)

A v2 carimba o instante da assinatura, então uma assinatura vazada deixa de valer sozinha:

```java
String userHash = WidgetIdentity.signV2(
        System.getenv("BFOCUS_WIDGET_SECRET"),
        "app-77",      // o usuário no seu sistema — sem ':'
        "erp-1042");   // a empresa (cliente) dele
// "v2.<ts>.<hex>": ts = segundos unix de agora; hex = HMAC-SHA256 de "v2:<ts>:app-77:erp-1042"
```

- Vai no mesmo lugar da v1 (o `userHash` do widget).
- Vale de **7 dias atrás até 5 minutos à frente**: gere a cada renderização da página, **nunca guarde**.
- O id do **usuário** não pode ter `:` (é o separador; a SDK lança `IllegalArgumentException`).
- `WidgetIdentity.signV2(secret, usuario, cliente, instant)` assina num instante dado (testes);
  `BfocusClient.signWidgetIdentityV2(...)` faz o mesmo.
- A v1 continua aceita.

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
