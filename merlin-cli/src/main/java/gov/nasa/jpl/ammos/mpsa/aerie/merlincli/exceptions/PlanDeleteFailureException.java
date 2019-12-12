package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class PlanDeleteFailureException extends ActionFailureException {
    public PlanDeleteFailureException(String reason) {
        super("Delete Plan", reason);
    }
}
