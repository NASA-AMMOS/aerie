package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions;

public class NoSuchActivityTypeException extends Exception {
    private final String activityTypeId;

    public NoSuchActivityTypeException(final String activityTypeId) {
        this.activityTypeId = activityTypeId;
    }

    public String getInvalidActivityTypeId() {
        return activityTypeId;
    }
}
