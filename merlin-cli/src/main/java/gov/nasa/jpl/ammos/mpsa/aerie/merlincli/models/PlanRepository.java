package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

public interface PlanRepository {
    String createPlan(String planJson) throws InvalidJsonException, InvalidPlanException;
    void updatePlan(String planId, String planJson) throws PlanNotFoundException, InvalidJsonException, InvalidPlanException;
    void deletePlan(String planId) throws PlanNotFoundException;
    void downloadPlan(String planId, String outName) throws PlanNotFoundException;
    String getPlanList();
    void appendActivityInstances(String planId, String instanceListJson) throws PlanNotFoundException, InvalidJsonException, InvalidPlanException;
    String getActivityInstance(String planId, String activityId) throws PlanNotFoundException, ActivityInstanceNotFoundException;
    void updateActivityInstance(String planId, String activityId, String activityInstanceJson) throws PlanNotFoundException, ActivityInstanceNotFoundException, InvalidJsonException, InvalidActivityInstanceException;
    void deleteActivityInstance(String planId, String activityId) throws PlanNotFoundException, ActivityInstanceNotFoundException;

    class InvalidJsonException extends Exception {
        public InvalidJsonException(String message) {
            super(message);
        }
    }
    class InvalidPlanException extends Exception {
        public InvalidPlanException(String message) {
            super(message);
        }
    }
    class PlanNotFoundException extends Exception {
        public PlanNotFoundException(String message) {
            super(message);
        }
    }
    class ActivityInstanceNotFoundException extends Exception {
        public ActivityInstanceNotFoundException(String message) {
            super(message);
        }
    }
    class InvalidActivityInstanceException extends Exception {
        public InvalidActivityInstanceException(String message) {
            super(message);
        }
    }
}
