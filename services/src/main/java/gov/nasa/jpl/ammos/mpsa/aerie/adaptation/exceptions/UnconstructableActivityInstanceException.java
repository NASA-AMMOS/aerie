package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions;

public class UnconstructableActivityInstanceException extends Exception {
    public UnconstructableActivityInstanceException(final String message) {
        super(message);
    }

    public UnconstructableActivityInstanceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
