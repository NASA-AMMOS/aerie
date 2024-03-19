package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.CheckpointSimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivityId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationEngineConfiguration;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class SimulationFacade {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimulationFacade.class);
  private final MissionModel<?> missionModel;
  private final InMemoryCachedEngineStore cachedEngines;
  private final PlanningHorizon planningHorizon;
  private final Map<String, ActivityType> activityTypes;
  private int itSimActivityId;
  private final SimulationEngineConfiguration configuration;
  private SimulationData initialSimulationResults;
  private final Supplier<Boolean> canceledListener;
  private final SchedulerModel schedulerModel;
  private Duration totalSimulationTime = Duration.ZERO;

  /**
   * Loads initial simulation results into the simulation. They will be served until initialSimulationResultsAreStale()
   * is called.
   * @param simulationData the initial simulation results
   */
  public void setInitialSimResults(final SimulationData simulationData){
    this.initialSimulationResults = simulationData;
  }


  public SimulationFacade(
      final MissionModel<?> missionModel,
      final SchedulerModel schedulerModel,
      final InMemoryCachedEngineStore cachedEngines,
      final PlanningHorizon planningHorizon,
      final SimulationEngineConfiguration simulationEngineConfiguration,
      final Supplier<Boolean> canceledListener){
    this.itSimActivityId = 0;
    this.missionModel = missionModel;
    this.schedulerModel = schedulerModel;
    this.cachedEngines = cachedEngines;
    this.planningHorizon = planningHorizon;
    this.activityTypes = new HashMap<>();
    this.configuration = simulationEngineConfiguration;
    this.canceledListener = canceledListener;
  }

  /**
   * Returns the total simulated time
   * @return
   */
  public Duration totalSimulationTime(){
    return totalSimulationTime;
  }

  public SimulationFacade(
      final PlanningHorizon planningHorizon,
      final MissionModel<?> missionModel,
      final SchedulerModel schedulerModel
      ){
    this.itSimActivityId = 0;
    this.missionModel = missionModel;
    this.cachedEngines = new InMemoryCachedEngineStore(0);
    this.planningHorizon = planningHorizon;
    this.activityTypes = new HashMap<>();
    this.configuration = new SimulationEngineConfiguration(Map.of(), Instant.now(), new MissionModelId(1));
    this.canceledListener = () -> false;
    this.schedulerModel = schedulerModel;
  }

  public Supplier<Boolean> getCanceledListener(){
    return this.canceledListener;
  }

  public void addActivityTypes(final Collection<ActivityType> activityTypes){
    activityTypes.forEach(at -> this.activityTypes.put(at.getName(), at));
  }

  private <K,V> void replaceValue(final Map<K,V> map, final V value, final V replacement){
    for (final Map.Entry<K, V> entry : map.entrySet()) {
      if (entry.getValue().equals(value)) {
        entry.setValue(replacement);
        break;
      }
    }
  }

  private void replaceIds(
      final PlanSimCorrespondence planSimCorrespondence,
      final Map<ActivityDirectiveId, ActivityDirectiveId> updates){
    for(final var replacements : updates.entrySet()){
      replaceValue(planSimCorrespondence.planActDirectiveIdToSimulationActivityDirectiveId,replacements.getKey(), replacements.getValue());
      if(planSimCorrespondence.directiveIdActivityDirectiveMap.containsKey(replacements.getKey())){
        final var value = planSimCorrespondence.directiveIdActivityDirectiveMap.remove(replacements.getKey());
        planSimCorrespondence.directiveIdActivityDirectiveMap.put(replacements.getValue(), value);
      }
    }
  }

  /**
   * Simulates until the end of the last activity of a plan. Updates the input plan with child activities and activity durations.
   * @param plan the plan to simulate
   * @return the inputs needed to compute simulation results
   * @throws SimulationException if an exception happens during simulation
   */
  public CheckpointSimulationDriver.SimulationResultsComputerInputs simulateNoResultsUntilEndPlan(final Plan plan)
  throws SimulationException, SchedulingInterruptedException
  {
    return simulateNoResults(plan, planningHorizon.getEndAerie(), null).simulationResultsComputerInputs();
  }

  public static class SimulationException extends Exception {
    SimulationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Simulates a plan until the end of one of its activities
   * Do not use to update the plan as decomposing activities may not finish
   * @param plan
   * @param activity
   * @return
   * @throws SimulationException
   */

  public CheckpointSimulationDriver.SimulationResultsComputerInputs simulateNoResultsUntilEndAct(
      final Plan plan,
      final SchedulingActivityDirective activity) throws SimulationException, SchedulingInterruptedException
  {
    return simulateNoResults(plan, null, activity).simulationResultsComputerInputs();
  }

  public record AugmentedSimulationResultsComputerInputs(
      CheckpointSimulationDriver.SimulationResultsComputerInputs simulationResultsComputerInputs,
      PlanSimCorrespondence planSimCorrespondence){}

  public AugmentedSimulationResultsComputerInputs simulateNoResults(
      final Plan plan,
      final Duration until) throws SimulationException, SchedulingInterruptedException
  {
    return simulateNoResults(plan, until, null);
  }


    /**
     * Simulates and updates plan
     * @param plan
     * @param until can be null
     * @param activity can be null
     */
  private AugmentedSimulationResultsComputerInputs simulateNoResults(
      final Plan plan,
      final Duration until,
      final SchedulingActivityDirective activity) throws SimulationException, SchedulingInterruptedException
  {
    final var planSimCorrespondence = scheduleFromPlan(plan, planningHorizon.getEndAerie(), planningHorizon.getStartInstant(), planningHorizon.getStartInstant());

    final var best = CheckpointSimulationDriver.bestCachedEngine(
        planSimCorrespondence.directiveIdActivityDirectiveMap(),
        cachedEngines.getCachedEngines(configuration));
    CheckpointSimulationDriver.CachedSimulationEngine engine = null;
    Duration from = Duration.ZERO;
    if(best.isPresent()){
      engine = best.get().getKey();
      replaceIds(planSimCorrespondence, best.get().getRight());
      from = engine.endsAt();
    }

    //Configuration
    //Three modes : (1) until a specific end time (2) until end of one specific activity (3) until end of last activity in plan
    Duration simulationDuration;
    TriFunction<SimulationEngine, Map<ActivityDirectiveId, ActivityDirective>, Map<ActivityDirectiveId, TaskId>, Boolean>
        stoppingCondition;
    //(1)
    if(until != null && activity == null){
      simulationDuration = until;
      stoppingCondition = CheckpointSimulationDriver.noCondition();
    }
    //(2)
    else if(activity != null && until == null){
      simulationDuration = planningHorizon.getEndAerie();
      stoppingCondition = CheckpointSimulationDriver.stopOnceActivityHasFinished(planSimCorrespondence.planActDirectiveIdToSimulationActivityDirectiveId.get(activity));
    //(3)
    } else if(activity == null && until == null){
      simulationDuration = planningHorizon.getEndAerie();
      stoppingCondition = CheckpointSimulationDriver.stopOnceAllActivitiessAreFinished();
    } else {
      throw new SimulationException("Bad configuration", null);
    }

    if(engine == null) engine = CheckpointSimulationDriver.CachedSimulationEngine.empty(missionModel);

    if(best.isPresent()) cachedEngines.registerUsed(engine);
    try {
      final var simulation = CheckpointSimulationDriver.simulateWithCheckpoints(
          missionModel,
          planSimCorrespondence.directiveIdActivityDirectiveMap(),
          planningHorizon.getStartInstant(),
          simulationDuration,
          planningHorizon.getStartInstant(),
          planningHorizon.getEndAerie(),
          $ -> {},
          canceledListener,
          engine,
          new ResourceAwareSpreadCheckpointPolicy(
              cachedEngines.capacity(),
              Duration.ZERO,
              planningHorizon.getEndAerie(),
              Duration.max(engine.endsAt(), Duration.ZERO),
              simulationDuration,
              1,
              true),
          stoppingCondition,
          cachedEngines,
          configuration);
      if(canceledListener.get()) throw new SchedulingInterruptedException("simulating");
      this.totalSimulationTime = this.totalSimulationTime.plus(simulation.elapsedTime().minus(from));
      final var activityResults =
          CheckpointSimulationDriver.computeActivitySimulationResults(simulation);

      updatePlanWithChildActivities(
          activityResults,
          activityTypes,
          plan,
          planSimCorrespondence);
      pullActivityDurationsIfNecessary(
          plan,
          planSimCorrespondence,
          activityResults
      );
      //plan has been updated
      return new AugmentedSimulationResultsComputerInputs(simulation, planSimCorrespondence);
    } catch(Exception e){
      if(e instanceof SchedulingInterruptedException sie){
        throw sie;
      }
      throw new SimulationException("An exception happened during simulation", e);
    }
  }

  public SimulationData simulateWithResults(
      final Plan plan,
      final Duration until) throws SimulationException, SchedulingInterruptedException
  {
    return simulateWithResults(plan, until, missionModel.getResources().keySet());
  }

  public SimulationData simulateWithResults(
      final Plan plan,
      final Duration until,
      final Set<String> resourceNames) throws SimulationException, SchedulingInterruptedException
  {
    if(this.initialSimulationResults != null) {
      final var inputPlan = scheduleFromPlan(plan, planningHorizon.getEndAerie(), planningHorizon.getStartInstant(), planningHorizon.getStartInstant());
      final var initialPlanA = scheduleFromPlan(this.initialSimulationResults.plan(), planningHorizon.getEndAerie(), planningHorizon.getStartInstant(), planningHorizon.getStartInstant());
      if (initialPlanA.equals(inputPlan)) {
        return new SimulationData(
            plan,
            initialSimulationResults.driverResults(),
            SimulationResultsConverter.convertToConstraintModelResults(initialSimulationResults.driverResults()),
            this.initialSimulationResults.mapping());
      }
    }
    final var resultsInput = simulateNoResults(plan, until);
    final var driverResults = CheckpointSimulationDriver.computeResults(resultsInput.simulationResultsComputerInputs, resourceNames);
    return new SimulationData(plan, driverResults, SimulationResultsConverter.convertToConstraintModelResults(driverResults), resultsInput.planSimCorrespondence.planActDirectiveIdToSimulationActivityDirectiveId);
  }

  private record PlanSimCorrespondence(
      BidiMap<SchedulingActivityDirectiveId, ActivityDirectiveId> planActDirectiveIdToSimulationActivityDirectiveId,
      Map<ActivityDirectiveId, ActivityDirective> directiveIdActivityDirectiveMap){
    @Override
    public boolean equals(Object other){
      if(other instanceof PlanSimCorrespondence planSimCorrespondenceAs){
        return directiveIdActivityDirectiveMap.size() == planSimCorrespondenceAs.directiveIdActivityDirectiveMap.size() &&
               new HashSet<>(directiveIdActivityDirectiveMap.values()).containsAll(new HashSet<>(((PlanSimCorrespondence) other).directiveIdActivityDirectiveMap.values()));
      }
      return false;
    }
  }

  private PlanSimCorrespondence scheduleFromPlan(
      final Plan plan,
      final Duration planDuration,
      final Instant planStartTime,
      final Instant simulationStartTime){
    final var activities = plan.getActivities();
    final var planActDirectiveIdToSimulationActivityDirectiveId = new DualHashBidiMap<SchedulingActivityDirectiveId, ActivityDirectiveId>();
    if(activities.isEmpty()) return new PlanSimCorrespondence(new DualHashBidiMap<>(), Map.of());
    //filter out child activities
    final var activitiesWithoutParent = activities.stream().filter(a -> a.topParent() == null).toList();
    final Map<ActivityDirectiveId, ActivityDirective> directivesToSimulate = new HashMap<>();

    for(final var activity : activitiesWithoutParent){
      final var activityIdSim = new ActivityDirectiveId(itSimActivityId++);
      planActDirectiveIdToSimulationActivityDirectiveId.put(activity.getId(), activityIdSim);
    }

    for(final var activity : activitiesWithoutParent) {
      final var activityDirective = schedulingActToActivityDir(activity, planActDirectiveIdToSimulationActivityDirectiveId);
      directivesToSimulate.put(
          planActDirectiveIdToSimulationActivityDirectiveId.get(activity.getId()),
          activityDirective);
    }
    return new PlanSimCorrespondence(planActDirectiveIdToSimulationActivityDirectiveId, directivesToSimulate);
  }

  /**
   * For activities that have a null duration (in an initial plan for example) and that have been simulated, we pull the duration and
   * replace the original instance with a new instance that includes the duration, both in the plan and the simulation facade
   */
  private void pullActivityDurationsIfNecessary(
      final Plan plan,
      final PlanSimCorrespondence correspondence,
      final SimulationEngine.SimulationActivityExtract activityExtract
      ) {
    final var toReplace = new HashMap<SchedulingActivityDirective, SchedulingActivityDirective>();
    for (final var activity : plan.getActivities()) {
      if (activity.duration() == null) {
        final var activityDirective = findSimulatedActivityById(
            activityExtract.simulatedActivities().values(),
            correspondence.planActDirectiveIdToSimulationActivityDirectiveId.get(activity.getId()));
        if (activityDirective.isPresent()) {
          final var replacementAct = SchedulingActivityDirective.copyOf(
              activity,
              activityDirective.get().duration()
          );
          toReplace.put(activity, replacementAct);
        }
        //if not, maybe the activity is not finished
      }
    }
    toReplace.forEach(plan::replace);
  }

  private final Optional<SimulatedActivity> findSimulatedActivityById(Collection<SimulatedActivity> simulatedActivities, final ActivityDirectiveId activityDirectiveId){
    return simulatedActivities.stream().filter(a -> a.directiveId().isPresent() && a.directiveId().get().equals(activityDirectiveId)).findFirst();
  }

  private void updatePlanWithChildActivities(
      final SimulationEngine.SimulationActivityExtract activityExtract,
      final Map<String, ActivityType> activityTypes,
      final Plan plan,
      final PlanSimCorrespondence planSimCorrespondence)
  {
    //remove all activities with parents
    final var toRemove = plan.getActivities().stream().filter(a -> a.topParent() != null).toList();
    toRemove.forEach(plan::remove);
    //pull child activities
    activityExtract.simulatedActivities().forEach( (activityInstanceId, activity) -> {
      if (activity.parentId() == null) return;
      final var rootParent = getIdOfRootParent(activityExtract, activityInstanceId);
      if(rootParent.isPresent()) {
        final var activityInstance = SchedulingActivityDirective.of(
            activityTypes.get(activity.type()),
            planningHorizon.toDur(activity.start()),
            activity.duration(),
            activity.arguments(),
            planSimCorrespondence.planActDirectiveIdToSimulationActivityDirectiveId.getKey(rootParent.get()),
            null,
            true);
        plan.add(activityInstance);
      }
    });
    //no need to replace in Evaluation because child activities are not referenced in it
  }

  private static Optional<ActivityDirectiveId> getIdOfRootParent(
      final SimulationEngine.SimulationActivityExtract results,
      final SimulatedActivityId instanceId){
    if(!results.simulatedActivities().containsKey(instanceId)){
      if(!results.unfinishedActivities().containsKey(instanceId)){
        LOGGER.debug("The simulation of the parent of activity with id "+ instanceId.id() + " has been finished");
      }
      return Optional.empty();
    }
    final var act = results.simulatedActivities().get(instanceId);
    if(act.parentId() == null){
      // SAFETY: any activity that has no parent must have a directive id.
      return Optional.of(act.directiveId().get());
    } else {
      return getIdOfRootParent(results, act.parentId());
    }
  }

  private static Optional<Duration> getActivityDuration(
      final ActivityDirectiveId activityDirectiveId,
      final CheckpointSimulationDriver.SimulationResultsComputerInputs simulationResultsInputs){
      return simulationResultsInputs.engine().getTaskDuration(simulationResultsInputs.activityDirectiveIdTaskIdMap().get(activityDirectiveId));
  }

  private ActivityDirective schedulingActToActivityDir(
      final SchedulingActivityDirective activity,
      final Map<SchedulingActivityDirectiveId, ActivityDirectiveId> planActDirectiveIdToSimulationActivityDirectiveId) {
    if(activity.getParentActivity().isPresent()) {
      throw new Error("This method should not be called with a generated activity but with its top-level parent.");
    }
    final var arguments = new HashMap<>(activity.arguments());
    if (activity.duration() != null) {
      final var durationType = activity.getType().getDurationType();
      if (durationType instanceof DurationType.Controllable dt) {
        arguments.put(dt.parameterName(), this.schedulerModel.serializeDuration(activity.duration()));
      } else if (
          durationType instanceof DurationType.Uncontrollable
          || durationType instanceof DurationType.Fixed
          || durationType instanceof DurationType.Parametric
      ) {
        // If an activity has already been simulated, it will have a duration, even if its DurationType is Uncontrollable.
      } else {
        throw new Error("Unhandled variant of DurationType: " + durationType);
      }
    }
    final var serializedActivity = new SerializedActivity(activity.getType().getName(), arguments);
    return new ActivityDirective(
        activity.startOffset(),
        serializedActivity,
        planActDirectiveIdToSimulationActivityDirectiveId.get(activity.anchorId()),
        activity.anchoredToStart());
  }
}
