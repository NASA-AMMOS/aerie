package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class UpdateActivityInstanceFailureException extends ActionFailureException {
    public UpdateActivityInstanceFailureException(String reason) {
        super("Update Activity Instance", reason);
    }
}
