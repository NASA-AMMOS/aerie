package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class AppendActivityInstancesFailureException extends ActionFailureException {
    public AppendActivityInstancesFailureException(String reason) {
        super("Append Activity Instnaces", reason);
    }
}
