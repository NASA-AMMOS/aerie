package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class PlanCreateFailureException extends ActionFailureException {
    public PlanCreateFailureException(String reason) {
        super("Create Plan", reason);
    }
}
