package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions;

public class NoSuchAdaptationException extends Exception {
    private final String id;

    public NoSuchAdaptationException(final String id, final Throwable cause) {
        super("No adaptation exists with id `" + id + "`", cause);
        this.id = id;
    }

    public NoSuchAdaptationException(final String id) { this(id, null); }

    public String getInvalidAdaptationId() {
        return this.id;
    }
}
