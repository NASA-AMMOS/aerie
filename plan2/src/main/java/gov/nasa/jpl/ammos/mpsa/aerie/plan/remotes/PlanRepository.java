package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.stream.Stream;

public interface PlanRepository {
  // Queries
  Stream<Pair<String, Plan>> getAllPlans();
  Plan getPlan(String id) throws NoSuchPlanException;
  Stream<Pair<String, ActivityInstance>> getAllActivitiesInPlan(String planId) throws NoSuchPlanException;
  ActivityInstance getActivityInPlanById(String planId, String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException;

  // Mutations
  String createPlan(NewPlan plan);
  PlanTransaction updatePlan(String id);
  void replacePlan(String id, NewPlan plan) throws NoSuchPlanException;
  void deletePlan(String id) throws NoSuchPlanException;

  String createActivity(String planId, ActivityInstance activity) throws NoSuchPlanException;
  ActivityTransaction updateActivity(String planId, String activityId);
  void replaceActivity(String planId, String activityId, ActivityInstance activity) throws NoSuchPlanException, NoSuchActivityInstanceException;
  void deleteActivity(String planId, String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException;
  void deleteAllActivities(String planId) throws NoSuchPlanException;

  interface PlanTransaction {
    void commit() throws NoSuchPlanException;

    PlanTransaction setName(String name);
    PlanTransaction setStartTimestamp(String timestamp);
    PlanTransaction setEndTimestamp(String timestamp);
    PlanTransaction setAdaptationId(String adaptationId);
  }

  interface ActivityTransaction {
    void commit() throws NoSuchPlanException, NoSuchActivityInstanceException;

    ActivityTransaction setType(String type);
    ActivityTransaction setStartTimestamp(String timestamp);
    ActivityTransaction setParameters(Map<String, SerializedParameter> parameters);
  }
}
