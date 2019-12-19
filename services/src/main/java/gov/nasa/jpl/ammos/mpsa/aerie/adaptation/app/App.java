package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;

import java.util.List;
import java.util.Map;

public interface App {
    Map<String, AdaptationJar> getAdaptations();

    String addAdaptation(NewAdaptation adaptation)
        throws AdaptationRejectedException;
    AdaptationJar getAdaptationById(String adaptationId)
        throws NoSuchAdaptationException;
    void removeAdaptation(String adaptationId)
        throws NoSuchAdaptationException;

    Map<String, ActivityType> getActivityTypes(String adaptationId)
        throws NoSuchAdaptationException, Adaptation.AdaptationContractException;
    ActivityType getActivityType(String adaptationId, String activityTypeId)
        throws NoSuchAdaptationException, Adaptation.AdaptationContractException, NoSuchActivityTypeException;
    // TODO: Provide a finer-scoped validation return type. Mere strings make all validations equally severe.
    List<String> validateActivityParameters(String adaptationId, SerializedActivity activityParameters)
        throws NoSuchAdaptationException, Adaptation.AdaptationContractException, NoSuchActivityTypeException;

    class AdaptationRejectedException extends Exception {
        public AdaptationRejectedException(final String message) { super(message); }
        public AdaptationRejectedException(final Throwable cause) { super(cause); }
    }

    class NoSuchAdaptationException extends Exception {
        private final String id;

        public NoSuchAdaptationException(final String id, final Throwable cause) {
            super("No adaptation exists with id `" + id + "`", cause);
            this.id = id;
        }

        public NoSuchAdaptationException(final String id) { this(id, null); }

        public String getInvalidAdaptationId() { return this.id; }
    }

    class NoSuchActivityTypeException extends Exception {
        private final String activityTypeId;

        public NoSuchActivityTypeException(final String activityTypeId, final Throwable cause) {
            super(cause);
            this.activityTypeId = activityTypeId;
        }

        public NoSuchActivityTypeException(final String activityTypeId) { this(activityTypeId, null); }

        public String getInvalidActivityTypeId() { return activityTypeId; }
    }
}
