package br.com.bernisoftware.bfocus;

/** Campo de {@code data} com tipo inesperado. Vira {@link BfocusException} {@code INVALID_RESPONSE} no transporte. */
final class InvalidDataException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    InvalidDataException(String message) {
        super(message);
    }
}
