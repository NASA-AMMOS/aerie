package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public class PlanDownloadFailureException extends ActionFailureException {
    public PlanDownloadFailureException(String reason) {
        super("Download Plan", reason);
    }
}
