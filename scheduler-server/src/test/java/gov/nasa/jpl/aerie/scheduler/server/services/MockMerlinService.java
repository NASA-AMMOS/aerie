package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityInstanceId;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.scheduler.server.models.MerlinActivityInstance;
import gov.nasa.jpl.aerie.scheduler.server.models.MerlinPlan;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MockMerlinService implements MissionModelService, PlanService.OwnerRole {

  private final Path modelPath;
  private final String modelName;
  private List<PlannedActivityInstance> initialPlan;
  Collection<PlannedActivityInstance> updatedPlan;

  MockMerlinService(final Path modelPath, final String modelName) {
    this.modelPath = modelPath;
    this.modelName = modelName;
    this.initialPlan = List.of();
  }

  void setInitialPlan(final List<PlannedActivityInstance> initialPlan) {
    this.initialPlan = initialPlan;
  }

  @Override
  public long getPlanRevision(final PlanId planId) {
    return 1L;
  }

  @Override
  public PlanMetadata getPlanMetadata(final PlanId planId)
  throws IOException, NoSuchPlanException, PlanServiceException
  {
    // Checked that revision matches
    // Uses model version info to load mission model jar
    // The PlanningHorizon here is not used for scheduling (at least not directly)
    return new PlanMetadata(
        new PlanId(1L),
        1L,
        new PlanningHorizon(
            TimeUtility.fromDOY("2021-001T00:00:00"),
            TimeUtility.fromDOY("2021-005T00:00:00")),
        1L,
        modelPath,
        modelName,
        "1.0.0",
        Map.of("initialDataPath", SerializedValue.of("/etc/hosts")));
  }

  @Override
  public MerlinPlan getPlanActivities(final PlanMetadata planMetadata, final Problem mission)
  {
    // TODO this gets the planMetadata from above

    return makePlan(initialPlan, mission);
  }

  @Override
  public Pair<PlanId, Map<ActivityInstance, ActivityInstanceId>> createNewPlanWithActivities(
      final PlanMetadata planMetadata,
      final Plan plan) throws IOException, NoSuchPlanException, PlanServiceException
  {
    return null;
  }

  @Override
  public PlanId createEmptyPlan(final String name, final long modelId, final Instant startTime, final Duration duration)
  throws IOException, NoSuchPlanException, PlanServiceException
  {
    return null;
  }

  @Override
  public void createSimulationForPlan(final PlanId planId)
  throws IOException, NoSuchPlanException, PlanServiceException
  {

  }

  @Override
  public Map<ActivityInstance, ActivityInstanceId> updatePlanActivities(
      final PlanId planId,
      final Map<SchedulingActivityInstanceId, ActivityInstanceId> idsFromInitialPlan,
      final MerlinPlan initialPlan,
      final Plan plan
  )
  throws IOException, NoSuchPlanException, PlanServiceException, NoSuchActivityInstanceException
  {
    this.updatedPlan = extractPlannedActivityInstances(plan);
    final var res = new HashMap<ActivityInstance, ActivityInstanceId>();
    for (final var activity : plan.getActivities()) {
      var id = 0L;
      res.put(activity, new ActivityInstanceId(id++));
    }
    return res;
  }

  @Override
  public void ensurePlanExists(final PlanId planId) throws IOException, NoSuchPlanException, PlanServiceException {

  }

  @Override
  public void clearPlanActivities(final PlanId planId)
  throws IOException, NoSuchPlanException, PlanServiceException
  {

  }

  @Override
  public Map<ActivityInstance, ActivityInstanceId> createAllPlanActivities(final PlanId planId, final Plan plan)
  throws IOException, NoSuchPlanException, PlanServiceException
  {
    return null;
  }

  @Override
  public TypescriptCodeGenerationService.MissionModelTypes getMissionModelTypes(final PlanId planId)
  throws IOException, MissionModelServiceException
  {
    return SchedulingIntegrationTests.MISSION_MODEL_TYPES;
  }

  @Override
  public TypescriptCodeGenerationService.MissionModelTypes getMissionModelTypes(final MissionModelId missionModelId)
  throws IOException, MissionModelServiceException, NoSuchMissionModelException
  {
    return SchedulingIntegrationTests.MISSION_MODEL_TYPES;
  }

  record PlannedActivityInstance(String type, Map<String, SerializedValue> args, Duration startTime) {}

  private static Collection<PlannedActivityInstance> extractPlannedActivityInstances(Plan plan) {
    final var plannedActivityInstances = new ArrayList<PlannedActivityInstance>();
    for (final var activity : plan.getActivities()) {
      plannedActivityInstances.add(new PlannedActivityInstance(
          activity.getType().getName(),
          activity.getArguments(),
          activity.getStartTime()));
    }
    return plannedActivityInstances;
  }

  private static MerlinPlan makePlan(final Iterable<MockMerlinService.PlannedActivityInstance> activities, final Problem problem) {
    final var initialPlan = new MerlinPlan();

    var id = 0L;
    for (final var activity : activities) {
      final var activityInstance = new MerlinActivityInstance(activity.type(), activity.startTime(), activity.args());
      initialPlan.addActivity(new ActivityInstanceId(id++), activityInstance);
    }
    return initialPlan;
  }
}
