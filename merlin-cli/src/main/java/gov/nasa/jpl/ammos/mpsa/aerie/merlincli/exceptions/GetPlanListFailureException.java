package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class GetPlanListFailureException extends ActionFailureException {
    public GetPlanListFailureException(String reason) {
        super("Get Plan List", reason);
    }
}
