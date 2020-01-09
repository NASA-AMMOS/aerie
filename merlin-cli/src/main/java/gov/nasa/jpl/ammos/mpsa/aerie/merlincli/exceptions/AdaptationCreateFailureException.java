package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class AdaptationCreateFailureException extends ActionFailureException {
    public AdaptationCreateFailureException(String reason) {
        super("Create Adaptation", reason);
    }
}
