package br.com.bernisoftware.bfocus;

import java.util.Objects;

/**
 * Release notes — {@code client.releaseNotes()}. Escopos: {@code release_notes:read} /
 * {@code release_notes:write}.
 */
public final class ReleaseNotesResource {
    private final Transport transport;

    ReleaseNotesResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * Primeira página das release notes do produto ({@code GET /products/{slug}/release-notes}).
     *
     * @param productSlug slug do produto
     * @return a página
     */
    public Page<ReleaseNote> list(String productSlug) {
        return list(productSlug, null, null);
    }

    /**
     * Uma página das release notes do produto.
     *
     * @param productSlug slug do produto
     * @param params filtros e paginação; pode ser {@code null}
     * @return a página
     */
    public Page<ReleaseNote> list(String productSlug, ReleaseNoteListParams params) {
        return list(productSlug, params, null);
    }

    /**
     * Uma página das release notes do produto — com opções da chamada.
     *
     * @param productSlug slug do produto
     * @param params filtros e paginação; pode ser {@code null}
     * @param options opções da chamada; pode ser {@code null}
     * @return a página
     */
    public Page<ReleaseNote> list(String productSlug, ReleaseNoteListParams params, RequestOptions options) {
        ReleaseNoteListParams p = params == null ? ReleaseNoteListParams.builder().build() : params;
        return transport.page(notesPath(productSlug), p.query(p.getPage(), p.getPageSize()), options, ReleaseNote::from);
    }

    /**
     * TODAS as release notes do produto, página a página sob demanda (100 por página).
     *
     * @param productSlug slug do produto
     * @return iterável preguiçoso
     */
    public PagedIterable<ReleaseNote> listAll(String productSlug) {
        return listAll(productSlug, null, null);
    }

    /**
     * TODAS as release notes do produto que batem com os filtros.
     *
     * @param productSlug slug do produto
     * @param params filtros ({@code page} não é aceito; {@code pageSize} padrão 100); pode ser {@code null}
     * @return iterável preguiçoso
     */
    public PagedIterable<ReleaseNote> listAll(String productSlug, ReleaseNoteListParams params) {
        return listAll(productSlug, params, null);
    }

    /**
     * TODAS as release notes do produto — com opções aplicadas a cada página.
     *
     * @param productSlug slug do produto
     * @param params filtros; pode ser {@code null}
     * @param options opções de cada chamada; pode ser {@code null}
     * @return iterável preguiçoso
     */
    public PagedIterable<ReleaseNote> listAll(String productSlug, ReleaseNoteListParams params, RequestOptions options) {
        ReleaseNoteListParams p = params == null ? ReleaseNoteListParams.builder().build() : params;
        int size = Paths.pageSizeForAll(p.getPage(), p.getPageSize());
        String path = notesPath(productSlug);
        return new PagedIterable<>(page -> transport.page(path, p.query(page, size), options, ReleaseNote::from));
    }

    /**
     * Busca a release note da versão ({@code GET /products/{slug}/release-notes/{version}}).
     *
     * @param productSlug slug do produto
     * @param version versão SemVer {@code X.Y.Z} (aceita {@code v} na frente)
     * @return a release note
     */
    public ReleaseNote get(String productSlug, String version) {
        return get(productSlug, version, null);
    }

    /**
     * Busca a release note da versão — com opções da chamada.
     *
     * @param productSlug slug do produto
     * @param version versão SemVer
     * @param options opções da chamada; pode ser {@code null}
     * @return a release note
     */
    public ReleaseNote get(String productSlug, String version, RequestOptions options) {
        return transport.call("GET", notePath(productSlug, version), null, null, options, ReleaseNote::from);
    }

    /**
     * Cria ou atualiza a release note da versão ({@code PUT /products/{slug}/release-notes/{version}}). Com
     * {@code publish(true)} no corpo, publica na mesma chamada — o jeito de publicar direto do CI (rodar de novo
     * com a mesma versão atualiza a nota).
     *
     * @param productSlug slug do produto
     * @param version versão SemVer {@code X.Y.Z} (aceita {@code v} na frente)
     * @param releaseNote campos a gravar
     * @return a release note gravada
     * @throws ConflictException {@code RELEASE_NOTE_CONFLICT} (gravação concorrente da mesma versão)
     */
    public ReleaseNote upsert(String productSlug, String version, ReleaseNoteUpsert releaseNote) {
        return upsert(productSlug, version, releaseNote, null);
    }

    /**
     * Cria ou atualiza a release note da versão — com opções da chamada.
     *
     * @param productSlug slug do produto
     * @param version versão SemVer
     * @param releaseNote campos a gravar
     * @param options opções da chamada; pode ser {@code null}
     * @return a release note gravada
     */
    public ReleaseNote upsert(String productSlug, String version, ReleaseNoteUpsert releaseNote, RequestOptions options) {
        Objects.requireNonNull(releaseNote, "releaseNote");
        return transport.call("PUT", notePath(productSlug, version), null, releaseNote.body(), options, ReleaseNote::from);
    }

    /**
     * Publica a release note ({@code POST /products/{slug}/release-notes/{version}/publish}).
     *
     * @param productSlug slug do produto
     * @param version versão SemVer
     * @return a release note publicada
     */
    public ReleaseNote publish(String productSlug, String version) {
        return publish(productSlug, version, null);
    }

    /**
     * Publica a release note — com opções da chamada.
     *
     * @param productSlug slug do produto
     * @param version versão SemVer
     * @param options opções da chamada; pode ser {@code null}
     * @return a release note publicada
     */
    public ReleaseNote publish(String productSlug, String version, RequestOptions options) {
        return transport.call("POST", notePath(productSlug, version) + "/publish", null, null, options, ReleaseNote::from);
    }

    private static String notesPath(String productSlug) {
        return ProductsResource.path(productSlug) + "/release-notes";
    }

    private static String notePath(String productSlug, String version) {
        return notesPath(productSlug) + "/" + Paths.segment(version, "version");
    }
}
