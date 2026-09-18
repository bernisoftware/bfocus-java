package br.com.bernisoftware.bfocus;

import java.util.Map;

/**
 * Corpo de {@link ReleaseNotesResource#upsert(String, String, ReleaseNoteUpsert)}. Parcial — veja
 * {@link PatchRequest}: setter não chamado = omitido; setter com {@code null} ou {@link Builder#clear(String...)}
 * = limpa. Com {@code publish(true)}, grava e publica na mesma chamada — o jeito de publicar direto do CI.
 */
public final class ReleaseNoteUpsert extends PatchRequest {
    static final PatchState.Spec SPEC = new PatchState.Spec("ReleaseNoteUpsert")
            .field("title", "title")
            .field("descriptionHtml", "description_html")
            .field("descriptionMarkdown", "description_markdown")
            .field("audience", "audience")
            .field("requireAckInternal", "require_ack_internal")
            .field("requireAckExternal", "require_ack_external")
            .fixed("publish", "publish");

    private ReleaseNoteUpsert(Map<String, Object> body) {
        super(body);
    }

    /**
     * Novo builder.
     *
     * @return o builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder de {@link ReleaseNoteUpsert}. */
    public static final class Builder {
        private final PatchState state = new PatchState(SPEC);

        private Builder() {
        }

        /**
         * Título (1–255; obrigatório ao criar).
         *
         * @param title o título ({@code null} = limpar)
         * @return este builder
         */
        public Builder title(String title) {
            state.set("title", title);
            return this;
        }

        /**
         * Descrição em HTML (use este OU {@link #descriptionMarkdown(String)}).
         *
         * @param descriptionHtml o HTML ({@code null} = limpar)
         * @return este builder
         */
        public Builder descriptionHtml(String descriptionHtml) {
            state.set("description_html", descriptionHtml);
            return this;
        }

        /**
         * Descrição em Markdown (a API converte para HTML).
         *
         * @param descriptionMarkdown o Markdown ({@code null} = limpar)
         * @return este builder
         */
        public Builder descriptionMarkdown(String descriptionMarkdown) {
            state.set("description_markdown", descriptionMarkdown);
            return this;
        }

        /**
         * Público: {@code internal} (equipe), {@code external} (clientes, no widget) ou {@code both} (padrão ao
         * criar).
         *
         * @param audience o público ({@code null} = limpar)
         * @return este builder
         */
        public Builder audience(String audience) {
            state.set("audience", audience);
            return this;
        }

        /**
         * Exige ciência da equipe interna.
         *
         * @param requireAckInternal exige ({@code null} = limpar)
         * @return este builder
         */
        public Builder requireAckInternal(Boolean requireAckInternal) {
            state.set("require_ack_internal", requireAckInternal);
            return this;
        }

        /**
         * Exige ciência dos clientes (widget).
         *
         * @param requireAckExternal exige ({@code null} = limpar)
         * @return este builder
         */
        public Builder requireAckExternal(Boolean requireAckExternal) {
            state.set("require_ack_external", requireAckExternal);
            return this;
        }

        /**
         * {@code true} publica depois de salvar (já publicada = nada muda). Não pode ser limpo.
         *
         * @param publish publicar
         * @return este builder
         */
        public Builder publish(boolean publish) {
            state.set("publish", publish);
            return this;
        }

        /**
         * Campos a LIMPAR (enviados como {@code null}), pelo nome Java ou JSON. {@code publish} não pode ser limpo.
         *
         * @param fields os campos
         * @return este builder
         * @throws IllegalArgumentException nome desconhecido ou campo que não aceita null
         */
        public Builder clear(String... fields) {
            state.clear(fields);
            return this;
        }

        /**
         * Monta o corpo.
         *
         * @return o corpo (imutável)
         */
        public ReleaseNoteUpsert build() {
            return new ReleaseNoteUpsert(state.snapshot());
        }
    }
}
