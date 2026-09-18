package br.com.bernisoftware.bfocus.internal;

/** Texto que não é JSON válido (com a posição do problema). Uso interno da SDK. */
public final class JsonException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int position;

    JsonException(String message, int position) {
        super(message + " (posição " + position + ")");
        this.position = position;
    }

    /** Posição (índice de caractere) onde o parser parou. */
    public int getPosition() {
        return position;
    }
}
