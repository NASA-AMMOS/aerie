package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class DeleteActivityInstanceFailureException extends ActionFailureException {
    public DeleteActivityInstanceFailureException(String reason) {
        super("Delete Activity", reason);
    }
}
