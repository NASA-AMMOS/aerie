package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface PlanRepository {
  PlanTransaction newPlan();
  Optional<PlanTransaction> getPlan(String id);
  Stream<PlanTransaction> getAllPlans();

  interface PlanTransaction {
    // Queries
    String getId();
    Plan get();

    // Mutators
    PlanTransaction setName(String name);
    PlanTransaction setStartTimestamp(String timestamp);
    PlanTransaction setEndTimestamp(String timestamp);
    PlanTransaction setAdaptationId(String adaptationId);

    // Transaction operations
    ActivityTransaction newActivity();
    Optional<ActivityTransaction> getActivity(String activityId);
    Stream<ActivityTransaction> getAllActivities();

    String save();
    void delete();
  }

  interface ActivityTransaction {
    // Queries
    String getId();
    ActivityInstance get();

    // Mutators
    ActivityTransaction setType(String type);
    ActivityTransaction setStartTimestamp(String timestamp);
    ActivityTransaction setParameters(Map<String, ActivityParameter> parameters);

    // Transaction operations
    String save();
    void delete();
  }
}
