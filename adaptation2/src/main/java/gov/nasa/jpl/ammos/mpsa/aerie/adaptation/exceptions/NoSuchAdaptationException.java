package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions;

public class NoSuchAdaptationException extends Exception {
    private final String id;

    public NoSuchAdaptationException(final String id) {
        super("No adaptation exists with id `" + id + "`");
        this.id = id;
    }

    public String getInvalidAdaptationId() {
        return this.id;
    }
}
