package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanDatasetException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.DatasetId;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.services.PlanService;
import gov.nasa.jpl.aerie.merlin.server.services.RevisionData;
import org.apache.commons.lang3.tuple.Pair;

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
  Map<PlanId, Plan> getAllPlans();
  Plan getPlan(PlanId planId) throws NoSuchPlanException;
  long getPlanRevision(PlanId planId) throws NoSuchPlanException;
  RevisionData getPlanRevisionData(PlanId planId) throws NoSuchPlanException;
  Map<ActivityDirectiveId, ActivityDirective> getAllActivitiesInPlan(PlanId planId) throws NoSuchPlanException;
  PlanService.SimulationArguments getSimulationArguments(PlanId planId, Timestamp startTimestamp, Duration planDuration);

  Map<String, Constraint> getAllConstraintsInPlan(PlanId planId) throws NoSuchPlanException;

  long addExternalDataset(PlanId planId, Timestamp datasetStart, ProfileSet profileSet) throws NoSuchPlanException;
  void extendExternalDataset(DatasetId datasetId, ProfileSet profileSet) throws NoSuchPlanDatasetException;
  List<Pair<Duration, ProfileSet>> getExternalDatasets(PlanId planId) throws NoSuchPlanException;
  Map<String, ValueSchema> getExternalResourceSchemas(PlanId planId) throws NoSuchPlanException;

  record CreatedPlan(PlanId planId, List<ActivityDirectiveId> activityIds) {}

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
    ActivityTransaction setStartOffset(Duration offset);
    ActivityTransaction setParameters(Map<String, SerializedValue> parameters);
  }
}
