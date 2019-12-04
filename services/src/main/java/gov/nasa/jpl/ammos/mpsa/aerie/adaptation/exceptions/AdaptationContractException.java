package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions;

public class AdaptationContractException extends Exception {
    public AdaptationContractException(final String message) {
        super(message);
    }

    public AdaptationContractException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
