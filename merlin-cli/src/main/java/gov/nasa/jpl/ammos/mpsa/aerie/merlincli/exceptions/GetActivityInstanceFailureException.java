package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class GetActivityInstanceFailureException extends ActionFailureException {
    public GetActivityInstanceFailureException(String reason) {
        super("Get Activity Instance", reason);
    }
}
