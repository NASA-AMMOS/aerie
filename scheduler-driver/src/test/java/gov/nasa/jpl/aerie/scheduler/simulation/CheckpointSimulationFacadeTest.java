package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.MissionModelId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationEngineConfiguration;
import gov.nasa.jpl.aerie.merlin.framework.ThreadedTask;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.SimulationUtility;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CheckpointSimulationFacadeTest {
  private SimulationFacade newSimulationFacade;
  private final static PlanningHorizon H = new PlanningHorizon(TimeUtility.fromDOY("2025-001T00:00:00.000"), TimeUtility.fromDOY("2025-005T00:00:00.000"));

  private final static Duration t0 = H.getStartAerie();
  private final static Duration d1min = Duration.of(1, Duration.MINUTE);
  private final static Duration d1hr = Duration.of(1, Duration.HOUR);
  private final static Duration t1hr = t0.plus(d1hr);
  private final static Duration t2hr = t0.plus(d1hr.times(2));
  private final static Duration t3hr = t0.plus(d1hr.times(3));
  private static PlanInMemory makePlanA012(Map<String, ActivityType> activityTypeMap) {
    final var plan = new PlanInMemory();
    final var actTypeA = activityTypeMap.get("BasicActivity");
    plan.add(SchedulingActivityDirective.of(actTypeA, t0, null, null, true));
    plan.add(SchedulingActivityDirective.of(actTypeA, t1hr, null, null, true));
    plan.add(SchedulingActivityDirective.of(actTypeA, t2hr, null, null, true));
    return plan;
  }
  @BeforeEach
  public void before(){
    ThreadedTask.CACHE_READS = true;
  }
  @Test
  public void planUpdateTest() throws SimulationFacade.SimulationException, SchedulingInterruptedException {
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    final Map<String, ActivityType> activityTypes = new HashMap<>();
    for(var taskType : fooMissionModel.getDirectiveTypes().directiveTypes().entrySet()){
      activityTypes.put(taskType.getKey(), new ActivityType(taskType.getKey(), taskType.getValue(), SimulationUtility.getFooSchedulerModel().getDurationTypes().get(taskType.getKey())));
    }
    final var plan = makePlanA012(activityTypes);
    newSimulationFacade = new SimulationFacade(
        fooMissionModel,
        SimulationUtility.getFooSchedulerModel(),
        new InMemoryCachedEngineStore(10),
        H,
        new SimulationEngineConfiguration(Map.of(), Instant.EPOCH, new MissionModelId(1)),
        () -> false);
    newSimulationFacade.addActivityTypes(activityTypes.values());
    newSimulationFacade.simulateNoResults(plan, t2hr);
    //we are stopping at 2hr, at the start of the last activity so it will not have a duraiton in the plan
    assertNull(plan.getActivities().stream().filter(a -> a.startOffset().isEqualTo(t2hr)).findFirst().get().duration());
  }

  @Test
  public void planUpdateTest2() throws SimulationFacade.SimulationException, SchedulingInterruptedException {
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    final Map<String, ActivityType> activityTypes = new HashMap<>();
    for(var taskType : fooMissionModel.getDirectiveTypes().directiveTypes().entrySet()){
      activityTypes.put(taskType.getKey(), new ActivityType(taskType.getKey(), taskType.getValue(), SimulationUtility.getFooSchedulerModel().getDurationTypes().get(taskType.getKey())));
    }
    final var plan = makePlanA012(activityTypes);
    newSimulationFacade = new SimulationFacade(
        fooMissionModel,
        SimulationUtility.getFooSchedulerModel(),
        new InMemoryCachedEngineStore(10),
        H,
        new SimulationEngineConfiguration(Map.of(),Instant.EPOCH, new MissionModelId(1)),
        () -> false);
    newSimulationFacade.addActivityTypes(activityTypes.values());
    newSimulationFacade.simulateNoResults(plan, t3hr);
    //we are stopping at 2hr, at the start of the last activity so it will not have a duraiton in the plan
    assertNotNull(plan
                      .getActivities()
                      .stream()
                      .filter(a -> a.startOffset().isEqualTo(t2hr))
                      .findFirst()
                      .get()
                      .duration());
  }

  @Test
  public void secondIteration() throws SimulationFacade.SimulationException, SchedulingInterruptedException {
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    final Map<String, ActivityType> activityTypes = new HashMap<>();
    for(var taskType : fooMissionModel.getDirectiveTypes().directiveTypes().entrySet()){
      activityTypes.put(taskType.getKey(), new ActivityType(taskType.getKey(), taskType.getValue(), SimulationUtility.getFooSchedulerModel().getDurationTypes().get(taskType.getKey())));
    }
    final var plan = makePlanA012(activityTypes);
    final var cachedEngines = new InMemoryCachedEngineStore(10);
    newSimulationFacade = new SimulationFacade(
        fooMissionModel,
        SimulationUtility.getFooSchedulerModel(),
        cachedEngines,
        H,
        new SimulationEngineConfiguration(Map.of(),Instant.EPOCH, new MissionModelId(1)),
        () -> false
    );
    newSimulationFacade.addActivityTypes(activityTypes.values());
    final var ret = newSimulationFacade.simulateWithResults(plan, t2hr);
    final var ret2 = newSimulationFacade.simulateWithResults(plan, t2hr);
    //TODO: equality on two checkpoint is difficult, although the plan are the same and the startoffset is the same, they are not equal (cells...)
    //so when saving, we don't know if we already have the same checkpoint
    //we are stopping at 2hr, at the start of the last activity so it will not have a duraiton in the plan
    SimulationResultsComparisonUtils.assertEqualsSimulationResults(ret.driverResults(), ret2.driverResults());
  }
}
