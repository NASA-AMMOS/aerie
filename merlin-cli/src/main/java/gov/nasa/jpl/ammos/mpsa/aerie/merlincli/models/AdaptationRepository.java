package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import java.io.File;

public interface AdaptationRepository {
    String createAdaptation(Adaptation adaptation, File adaptationJar) throws InvalidAdaptationException;
    void deleteAdaptation(String adaptationId) throws AdaptationNotFoundException;
    Adaptation getAdaptation(String adaptationId) throws AdaptationNotFoundException;
    String getAdaptationList();
    String getActivityTypes(String adaptationId) throws AdaptationNotFoundException;
    String getActivityType(String adaptationId, String activityType) throws AdaptationNotFoundException, ActivityTypeNotDefinedException;

    class InvalidAdaptationException extends Exception {
        public InvalidAdaptationException(String message) {
            super(message);
        }
    }
    class AdaptationNotFoundException extends Exception {
        public AdaptationNotFoundException(String message) {
            super(message);
        }
    }
    class ActivityTypeNotDefinedException extends Exception {
        public ActivityTypeNotDefinedException(String message) {
            super(message);
        }
    }
}
