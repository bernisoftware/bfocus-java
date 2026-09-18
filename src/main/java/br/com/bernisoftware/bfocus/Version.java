package br.com.bernisoftware.bfocus;

/**
 * Versão desta SDK. Bumpada pelo {@code scripts/release-sdks.sh} junto com o {@code pom.xml} (um teste trava
 * a igualdade entre os dois).
 *
 * <p>Não é cosmética: vai no header {@code X-Bfocus-Client} de toda requisição, e é por ele que a API sabe a
 * quem avisar quando uma correção exige atualizar a SDK.
 */
public final class Version {
    private Version() {
    }

    /** Versão do artefato {@code br.com.bernisoftware:bfocus}. */
    public static final String VERSION = "0.1.0";

    /** Identificação enviada em {@code X-Bfocus-Client} e {@code User-Agent}. */
    public static final String CLIENT_ID = "bfocus-java/" + VERSION;
}
