package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.*;

public interface PlanRepository {

    String createPlan(String planJson) throws InvalidJsonException, InvalidPlanException;
    void updatePlan(String planId, String planJson) throws PlanNotFoundException, InvalidJsonException, InvalidPlanException;
    void deletePlan(String planId) throws PlanNotFoundException;
    void downloadPlan(String planId, String outName) throws PlanNotFoundException;
    String getPlanList();
    void appendActivityInstances(String planId, String instanceListJson) throws PlanNotFoundException, InvalidJsonException, InvalidPlanException;
    String getActivityInstance(String planId, String activityId) throws PlanNotFoundException, ActivityInstanceNotFoundException;
    void updateActivityInstance(String planId, String activityId, String activityInstanceJson) throws PlanNotFoundException, ActivityInstanceNotFoundException, InvalidJsonException, InvalidPlanException;
    void deleteActivityInstance(String planId, String activityId) throws PlanNotFoundException, ActivityInstanceNotFoundException;

    class InvalidJsonException extends Exception {}
    class InvalidPlanException extends Exception {}
    class PlanNotFoundException extends Exception {}
    class ActivityInstanceNotFoundException extends Exception {}
}
