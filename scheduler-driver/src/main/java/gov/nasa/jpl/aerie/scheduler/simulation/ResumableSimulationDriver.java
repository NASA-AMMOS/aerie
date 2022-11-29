package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.StartOffsetReducer;
import gov.nasa.jpl.aerie.merlin.driver.engine.JobSchedule;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.scheduler.NotNull;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ResumableSimulationDriver<Model> {

  private Duration curTime = Duration.ZERO;
  private SimulationEngine engine = new SimulationEngine();
  private LiveCells cells;
  private TemporalEventSource timeline = new TemporalEventSource();
  private final MissionModel<Model> missionModel;
  private final Duration planDuration;
  private JobSchedule.Batch<SimulationEngine.JobId> batch;

  private final Topic<ActivityDirectiveId> activityTopic = new Topic<>();

  //mapping each activity name to its task id (in String form) in the simulation engine
  private final Map<ActivityDirectiveId, TaskId> plannedDirectiveToTask;

  //simulation results so far
  private SimulationResults lastSimResults;
  //cached simulation results cover the period [Duration.ZERO, lastSimResultsEnd]
  private Duration lastSimResultsEnd = Duration.ZERO;

  //List of activities simulated since the last reset
  private final Map<ActivityDirectiveId, ActivityDirective> activitiesInserted = new HashMap<>();

  public ResumableSimulationDriver(MissionModel<Model> missionModel, Duration planDuration){
    this.missionModel = missionModel;
    plannedDirectiveToTask = new HashMap<>();
    this.planDuration = planDuration;
    initSimulation();
    batch = null;
  }

  // This method is currently only used in one test.
  /*package-private*/ void clearActivitiesInserted() {activitiesInserted.clear();}

  /*package-private*/ void initSimulation(){
    plannedDirectiveToTask.clear();
    lastSimResults = null;
    lastSimResultsEnd = Duration.ZERO;
    if (this.engine != null) this.engine.close();
    this.engine = new SimulationEngine();

    /* The top-level simulation timeline. */
    this.timeline = new TemporalEventSource();
    this.cells = new LiveCells(timeline, missionModel.getInitialCells());
    /* The current real time. */
    curTime = Duration.ZERO;

    // Begin tracking all resources.
    for (final var entry : missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      engine.trackResource(name, resource, curTime);
    }

    // Start daemon task(s) immediately, before anything else happens.
    {
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());

      batch = engine.extractNextJobs(Duration.MAX_VALUE);
      final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE);
      timeline.add(commit);
    }
  }

  private void simulateUntil(Duration endTime){
    assert(endTime.noShorterThan(curTime));
      if(batch == null){
        batch = engine.extractNextJobs(Duration.MAX_VALUE);
      }
      // Increment real time, if necessary.
      while(!batch.offsetFromStart().longerThan(endTime) && !endTime.isEqualTo(Duration.MAX_VALUE)) {
        final var delta = batch.offsetFromStart().minus(curTime);
        curTime = batch.offsetFromStart();
        timeline.add(delta);
        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE);
        timeline.add(commit);

        batch = engine.extractNextJobs(Duration.MAX_VALUE);
      }
    lastSimResults = null;
  }


  /**
   * Simulate an activity directive.
   * @param activity the serialized type and arguments of the activity directive to be simulated
   * @param startOffset the start offset from the activity's anchor
   * @param anchorId the activity id of the anchor (or null if the activity is anchored to the plan)
   * @param anchoredToStart toggle for if the activity is anchored to the start or end of its anchor
   * @param activityId the activity id for the activity to simulate
   */
  public void simulateActivity(final Duration startOffset, final SerializedActivity activity, final ActivityDirectiveId anchorId, final boolean anchoredToStart, final ActivityDirectiveId activityId) {
    simulateActivity(new ActivityDirective(startOffset, activity, anchorId, anchoredToStart), activityId);
  }

  /**
   * Simulate an activity directive.
   * @param activityToSimulate the activity directive to simulate
   * @param activityId the ActivityDirectiveId for the activity to simulate
   */
  public void simulateActivity(ActivityDirective activityToSimulate, ActivityDirectiveId activityId)
  {
    activitiesInserted.put(activityId, activityToSimulate);
    if(activityToSimulate.startOffset().noLongerThan(curTime)){
      initSimulation();
      simulateSchedule(activitiesInserted);
    } else {
      simulateSchedule(Map.of(activityId, activityToSimulate));
    }
  }

  public void simulateActivities(@NotNull Map<ActivityDirectiveId, ActivityDirective> activitiesToSimulate) {
    if(activitiesToSimulate.isEmpty()) return;

    activitiesInserted.putAll(activitiesToSimulate);

    final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(planDuration, activitiesToSimulate).compute();
    resolved.get(null).sort(Comparator.comparing(Pair::getRight));
    final var earliestStartOffset = resolved.get(null).get(0);

    if(earliestStartOffset.getRight().noLongerThan(curTime)){
      initSimulation();
      simulateSchedule(activitiesInserted);
    } else {
      simulateSchedule(activitiesToSimulate);
    }
  }


  /**
   * Get the simulation results from the Duration.ZERO to the current simulation time point
   * @param startTimestamp the timestamp for the start of the planning horizon. Used as epoch for computing SimulationResults.
   * @return the simulation results
   */
  public SimulationResults getSimulationResults(Instant startTimestamp){
    return getSimulationResultsUpTo(startTimestamp, curTime);
  }

  public Duration getCurrentSimulationEndTime(){
    return curTime;
  }

  /**
   * Get the simulation results from the Duration.ZERO to a specified end time point.
   * The provided simulation results might cover more than the required time period.
   * @param startTimestamp the timestamp for the start of the planning horizon. Used as epoch for computing SimulationResults.
   * @param endTime the end timepoint. The simulation results will be computed up to this point.
   * @return the simulation results
   */
  public SimulationResults getSimulationResultsUpTo(Instant startTimestamp, Duration endTime){
    //if previous results cover a bigger period, we return do not regenerate
    if(endTime.longerThan(curTime)){
      simulateUntil(endTime);
    }

    if(lastSimResults == null || endTime.longerThan(lastSimResultsEnd) || startTimestamp.compareTo(lastSimResults.startTime) != 0) {
      lastSimResults = SimulationEngine.computeResults(
          engine,
          startTimestamp,
          endTime,
          activityTopic,
          timeline,
          missionModel.getTopics());
      lastSimResultsEnd = endTime;
      //while sim results may not be up to date with curTime, a regeneration has taken place after the last insertion
    }
    return lastSimResults;
  }

  private void simulateSchedule(final Map<ActivityDirectiveId, ActivityDirective> schedule)
  {
    if (schedule.isEmpty()) {
      throw new IllegalArgumentException("simulateSchedule() called with empty schedule, use simulateUntil() instead");
    }

    // Get all activities as close as possible to absolute time, then schedule all activities.
    // Using HashMap explicitly because it allows `null` as a key.
    // `null` key means that an activity is not waiting on another activity to finish to know its start time
    final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(
        planDuration,
        schedule).compute();

    scheduleActivities(
        schedule,
        resolved,
        missionModel,
        engine,
        activityTopic
    );

    var allTaskFinished = false;

    if (batch == null) {
      batch = engine.extractNextJobs(Duration.MAX_VALUE);
    }
    // Increment real time, if necessary.
    Duration delta = batch.offsetFromStart().minus(curTime);

    //once all tasks are finished, we need to wait for events triggered at the same time
    while (!allTaskFinished || delta.isZero()) {
      curTime = batch.offsetFromStart();
      timeline.add(delta);
      // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
      //   even if they occur at the same real time.

      // Run the jobs in this batch.
      final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE);
      timeline.add(commit);

      // all tasks are complete : do not exit yet, there might be event triggered at the same time
      if (!plannedDirectiveToTask.isEmpty() && plannedDirectiveToTask
          .values()
          .stream()
          .allMatch(engine::isTaskComplete)) {
        allTaskFinished = true;
      }

      // Update batch and increment real time, if necessary.
      batch = engine.extractNextJobs(Duration.MAX_VALUE);
      delta = batch.offsetFromStart().minus(curTime);
    }
    lastSimResults = null;
  }

  /**
   * Returns the duration of a terminated simulated activity
   * @param activityDirectiveId the activity id
   * @return its duration if the activity has been simulated and has finished simulating, an IllegalArgumentException otherwise
   */
  public Optional<Duration> getActivityDuration(ActivityDirectiveId activityDirectiveId){
    return engine.getTaskDuration(plannedDirectiveToTask.get(activityDirectiveId));
  }

  private void scheduleActivities(
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final SimulationEngine engine,
      final Topic<ActivityDirectiveId> activityTopic
  )
  {
    if(resolved.get(null) == null) { return; } // Nothing to simulate

    for (final Pair<ActivityDirectiveId, Duration> directivePair : resolved.get(null)) {
      final var directiveId = directivePair.getLeft();
      final var startOffset = directivePair.getRight();
      final var serializedDirective = schedule.get(directiveId).serializedActivity();

      final TaskFactory<Unit, ?> task;
      try {
        task = missionModel.getTaskFactory(serializedDirective);
      } catch (final InstantiationException ex) {
        // All activity instantiations are assumed to be validated by this point
        throw new Error("Unexpected state: activity instantiation %s failed with: %s"
                            .formatted(serializedDirective.getTypeName(), ex.toString()));
      }

      final var taskId = engine.scheduleTask(startOffset, makeTaskFactory(
          directiveId,
          task,
          schedule,
          resolved,
          missionModel,
          activityTopic
      ));
      plannedDirectiveToTask.put(directiveId,taskId);
    }
  }

  private static <Model, Output> TaskFactory<Unit, Unit> makeTaskFactory(
      final ActivityDirectiveId directiveId,
      final TaskFactory<Unit, Output> task,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final Topic<ActivityDirectiveId> activityTopic
  )
  {
    // Emit the current activity (defined by directiveId)
    return executor -> (scheduler0, input0) -> TaskStatus.calling((TaskFactory<Unit, Output>) (executor1 -> (scheduler1, input1) -> {
      scheduler1.emit(directiveId, activityTopic);
      return task.create(executor1).step(scheduler1, input1);
    }), (scheduler2, input2) -> {
      // When the current activity finishes, get the list of the activities that needed this activity to finish to know their start time
      final List<Pair<ActivityDirectiveId, Duration>> dependents = resolved.get(directiveId) == null ? List.of() : resolved.get(directiveId);
      // Iterate over the dependents
      for (final var dependent : dependents) {
        scheduler2.spawn(executor2 -> (scheduler3, input3) ->
            // Delay until the dependent starts
            TaskStatus.delayed(dependent.getRight(), (scheduler4, input4) -> {
              final var dependentDirectiveId = dependent.getLeft();
              final var serializedDependentDirective = schedule.get(dependentDirectiveId).serializedActivity();

              // Initialize the Task for the dependent
              final TaskFactory<Unit, ?> dependantTask;
              try {
                dependantTask = missionModel.getTaskFactory(serializedDependentDirective);
              } catch (final InstantiationException ex) {
                // All activity instantiations are assumed to be validated by this point
                throw new Error("Unexpected state: activity instantiation %s failed with: %s"
                                    .formatted(serializedDependentDirective.getTypeName(), ex.toString()));
              }

              // Schedule the dependent
              // When it finishes, it will schedule the activities depending on it to know their start time
              scheduler4.spawn(makeTaskFactory(
                  dependentDirectiveId,
                  dependantTask,
                  schedule,
                  resolved,
                  missionModel,
                  activityTopic
              ));
              return TaskStatus.completed(Unit.UNIT);
            }));
      }
      return TaskStatus.completed(Unit.UNIT);
    });
  }
}
