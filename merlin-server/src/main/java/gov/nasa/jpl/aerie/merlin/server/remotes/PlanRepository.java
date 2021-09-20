package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.util.List;
import java.util.Map;

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
  Map<String, Plan> getAllPlans();
  Plan getPlan(String id) throws NoSuchPlanException;
  long getPlanRevision(String id) throws NoSuchPlanException;
  Map<String, ActivityInstance> getAllActivitiesInPlan(String planId) throws NoSuchPlanException;
  ActivityInstance getActivityInPlanById(String planId, String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException;

  // Mutations
  CreatedPlan createPlan(NewPlan plan) throws AdaptationRepository.NoSuchAdaptationException;
  PlanTransaction updatePlan(String id) throws NoSuchPlanException;
  List<String> replacePlan(String id, NewPlan plan) throws NoSuchPlanException;
  void deletePlan(String id) throws NoSuchPlanException;

  String createActivity(String planId, ActivityInstance activity) throws NoSuchPlanException;
  ActivityTransaction updateActivity(String planId, String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException;
  void replaceActivity(String planId, String activityId, ActivityInstance activity) throws NoSuchPlanException, NoSuchActivityInstanceException;
  void deleteActivity(String planId, String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException;
  void deleteAllActivities(String planId) throws NoSuchPlanException;

  Map<String, Constraint> getAllConstraintsInPlan(String planId) throws NoSuchPlanException;
  void replacePlanConstraints(String planId, Map<String, Constraint> constraints) throws NoSuchPlanException;
  void deleteConstraintInPlanById(String planId, String constraintId) throws NoSuchPlanException;

  record CreatedPlan(String planId, List<String> activityIds) {}

  interface PlanTransaction {
    void commit() throws NoSuchPlanException;

    PlanTransaction setName(String name);
    PlanTransaction setStartTimestamp(Timestamp timestamp);
    PlanTransaction setEndTimestamp(Timestamp timestamp);
    PlanTransaction setConfiguration(Map<String, SerializedValue> configuration);
  }

  interface ActivityTransaction {
    void commit() throws NoSuchPlanException, NoSuchActivityInstanceException;

    ActivityTransaction setType(String type);
    ActivityTransaction setStartTimestamp(Timestamp timestamp);
    ActivityTransaction setParameters(Map<String, SerializedValue> parameters);
  }
}
