package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class PlanUpdateFailureException extends ActionFailureException {
    public PlanUpdateFailureException(String reason) {
        super("Update Plan", reason);
    }
}
