package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions;

public class NoSuchActivityTypeException extends Exception {
    private final String adaptationId;
    private final String activityTypeId;

    public NoSuchActivityTypeException(final String adaptationId, final String activityTypeId) {
        super("No activity with id `" + activityTypeId + "` exists in adaptation with id `" + adaptationId + "`");
        this.adaptationId = adaptationId;
        this.activityTypeId = activityTypeId;
    }

    public String getAdaptationId() {
        return adaptationId;
    }

    public String getInvalidActivityTypeId() {
        return activityTypeId;
    }
}
