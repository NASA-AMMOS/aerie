package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.stream.Stream;

/**
 * An owned interface to a concurrency-safe store of plans.
 *
 * A {@code PlanRepository} provides access to a shared store of plans, each indexed by a unique ID.
 * To support concurrent access, updates to the store must be concurrency-controlled. Every concurrent agent must have its
 * own {@code PlanRepository} reference, so that the reads and writes of each agent may be tracked analogously to
 * <a href="https://en.wikipedia.org/wiki/Load-link/store-conditional">load-link/store-conditional</a> semantics.
 */
public interface PlanRepository {
  // Queries
  Stream<Pair<String, Plan>> getAllPlans();
  Plan getPlan(String id) throws NoSuchPlanException;
  Stream<Pair<String, ActivityInstance>> getAllActivitiesInPlan(String planId) throws NoSuchPlanException;
  ActivityInstance getActivityInPlanById(String planId, String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException;

  // Mutations
  String createPlan(NewPlan plan);
  PlanTransaction updatePlan(String id) throws NoSuchPlanException;
  void replacePlan(String id, NewPlan plan) throws NoSuchPlanException;
  void deletePlan(String id) throws NoSuchPlanException;

  String createActivity(String planId, ActivityInstance activity) throws NoSuchPlanException;
  ActivityTransaction updateActivity(String planId, String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException;
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
    ActivityTransaction setParameters(Map<String, SerializedValue> parameters);
  }
}
