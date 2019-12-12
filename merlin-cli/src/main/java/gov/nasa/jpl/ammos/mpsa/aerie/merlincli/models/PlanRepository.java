package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.*;

public interface PlanRepository {

    String createPlan(String planJson) throws PlanCreateFailureException;
    void updatePlan(String planId, String planJson) throws PlanUpdateFailureException;
    void deletePlan(String planId) throws PlanDeleteFailureException;
    void downloadPlan(String planId, String outName) throws PlanDownloadFailureException;
    String getPlanList() throws GetPlanListFailureException;
    void appendActivityInstances(String planId, String instanceListJson) throws AppendActivityInstancesFailureException;
    String getActivityInstance(String planId, String activityId) throws GetActivityInstanceFailureException;
    void updateActivityInstance(String planId, String activityId, String activityInstanceJson) throws UpdateActivityInstanceFailureException;
    void deleteActivityInstance(String planId, String activityId) throws DeleteActivityInstanceFailureException;
}
