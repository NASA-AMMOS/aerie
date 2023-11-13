package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.ResourceTracker;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsInterface;
import gov.nasa.jpl.aerie.merlin.driver.StartOffsetReducer;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.scheduler.NotNull;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ResumableSimulationDriver<Model> implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(ResumableSimulationDriver.class);

  private static boolean debug = false;

  public Duration curTime() {
    if (engine == null) {
      return Duration.ZERO;
    }
    return engine.curTime();
  }

  public void setCurTime(Duration time) {
    this.engine.setCurTime(time);
  }


  private SimulationEngine engine;
  private final boolean useResourceTracker;
  private final MissionModel<Model> missionModel;
  private Instant startTime;
  private final Duration planDuration;

  private static final Topic<ActivityDirectiveId> activityTopic = SimulationEngine.defaultActivityTopic;

  //mapping each activity name to its task id (in String form) in the simulation engine
  private final Map<ActivityDirectiveId, TaskId> plannedDirectiveToTask;

  //simulation results so far
  private SimulationResultsInterface lastSimResults;
  //cached simulation results cover the period [Duration.ZERO, lastSimResultsEnd]
  private Duration lastSimResultsEnd = Duration.ZERO;

  //List of activities simulated since the last reset
  private final Map<ActivityDirectiveId, ActivityDirective> activitiesInserted = new HashMap<>();
  private Topic<Topic<?>> queryTopic = new Topic<>();

  //counts the number of simulation restarts, used as performance metric in the scheduler
  //effectively counting the number of calls to initSimulation()
  private int countSimulationRestarts;

  // Whether we're rerunning the simulation, in which case we can be lazy about starting up stuff, like daemons
  private boolean rerunning = false;

  public ResumableSimulationDriver(MissionModel<Model> missionModel, PlanningHorizon horizon, boolean useResourceTracker){
    this(missionModel, horizon.getStartInstant(), horizon.getAerieHorizonDuration(), useResourceTracker);
  }

  public ResumableSimulationDriver(MissionModel<Model> missionModel, Duration planDuration, boolean useResourceTracker){
    this(missionModel, Instant.now(), planDuration, useResourceTracker);
  }

  private ResourceTracker resourceTracker = null;

  public ResumableSimulationDriver(MissionModel<Model> missionModel, Instant startTime, Duration planDuration, boolean useResourceTracker){
    this.useResourceTracker = useResourceTracker;
    this.missionModel = missionModel;
    plannedDirectiveToTask = new HashMap<>();
    this.startTime = startTime;
    this.planDuration = planDuration;
    countSimulationRestarts = 0;
    initSimulation();
  }

  // This method is currently only used in one test.
  /*package-private*/ void clearActivitiesInserted() {activitiesInserted.clear();}

  /*package-private*/ void initSimulation(){
    logger.warn("Reinitialization of the scheduling simulation");
    if (debug) System.out.println("ResumableSimulationDriver.initSimulation()");
    plannedDirectiveToTask.clear();
    lastSimResults = null;
    lastSimResultsEnd = Duration.ZERO;
    // If rerunning the simulation, reuse the existing SimulationEngine to avoid redundant computation
    this.rerunning = this.engine != null && this.engine.timeline.commitsByTime.size() > 1;
    if (this.engine != null) this.engine.close();
    SimulationEngine oldEngine = rerunning ? this.engine : null;
    this.engine = new SimulationEngine(startTime, missionModel, oldEngine, resourceTracker);
    if (useResourceTracker) {
      this.resourceTracker = new ResourceTracker(engine, missionModel.getInitialCells());
      engine.resourceTracker = this.resourceTracker;
    }
    //assert useResourceTracker;
    // TODO: For the scheduler, it only simulates up to the end of the last activity added.  Make sure we don't assume a full simulation exists.

    /* The current real time. */
    //setCurTime(Duration.ZERO);

    // Begin tracking any resources that have not already been simulated.
    trackResources();

    // Start daemon task(s) immediately, before anything else happens.
    startDaemons(curTime());

    // The sole purpose of this task is to make sure the simulation has "stuff to do" until the simulationDuration.
    //engine.scheduleTask(planDuration, executor -> $ -> TaskStatus.completed(Unit.UNIT), null);

    countSimulationRestarts++;
    if (debug) System.out.println("ResumableSimulationDriver::countSimulationRestarts incremented to " + countSimulationRestarts);

  }

  private void trackResources() {
    // Begin tracking all resources.
    for (final var entry : missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      if (useResourceTracker) {
        resourceTracker.track(name, resource);
      } else {
        engine.trackResource(name, resource, Duration.ZERO);
      }
    }
  }

  /**
   * Return the number of simulation restarts
   * @return the number of simulation restarts
   */
  public int getCountSimulationRestarts(){
    return countSimulationRestarts;
  }

  private void startDaemons(Duration time) {
    if (!rerunning && missionModel.hasDaemons()) {
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon(), null);
    }
  }

  @Override
  public void close() {
    this.engine.close();
  }

  private void simulateUntil(Duration endTime){
    if (debug) System.out.println("simulateUntil(" + endTime + ")");
    assert(endTime.noShorterThan(curTime()));
    if (endTime.isEqualTo(Duration.MAX_VALUE)) return;
    // The sole purpose of this task is to make sure the simulation has "stuff to do" until the endTime.
    //engine.scheduleTask(endTime, executor -> $ -> TaskStatus.completed(Unit.UNIT), null);
    while(engine.hasJobsScheduledThrough(endTime)) {
      // Run the jobs in this batch.
      engine.step(Duration.MAX_VALUE, queryTopic, $ -> {});
    }
    if (useResourceTracker) {
      // Replay the timeline to collect resource profiles
      engine.generateResourceProfiles(endTime);
    }
    lastSimResults = null;
  }


  /**
   * Simulate an activity directive.  We assume that the original plan activities have
   * been scheduled in the SimulationEngine and may be partially simulated.
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
    if(activityToSimulate.startOffset().noLongerThan(curTime())){
      initSimulation();
      simulateSchedule(Map.of(activityId, activityToSimulate));
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

    if(earliestStartOffset.getRight().noLongerThan(curTime())){
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
  public SimulationResultsInterface getSimulationResults(Instant startTimestamp){
    return getSimulationResultsUpTo(startTimestamp, curTime());
  }

  public Duration getCurrentSimulationEndTime(){
    return curTime();
  }

  /**
   * Get the simulation results from the Duration.ZERO to a specified end time point.
   * The provided simulation results might cover more than the required time period.
   * @param startTimestamp the timestamp for the start of the planning horizon. Used as epoch for computing SimulationResults.
   * @param endTime the end timepoint. The simulation results will be computed up to this point.
   * @return the simulation results
   */
  public SimulationResultsInterface getSimulationResultsUpTo(Instant startTimestamp, Duration endTime){
    if (debug) System.out.println("getSimulationResultsUpTo(startTimestamp=" + startTimestamp + ", endTime=" + endTime + ")");
    //if previous results cover a bigger period, we return do not regenerate
    if(endTime.longerThan(curTime())){
      simulateUntil(endTime);
    }

    if(lastSimResults == null || endTime.longerThan(lastSimResultsEnd) || startTimestamp.compareTo(lastSimResults.getStartTime()) != 0) {
      lastSimResults = engine.computeResults(
            startTimestamp,
            endTime,
            activityTopic);
      lastSimResultsEnd = endTime;
      //while sim results may not be up to date with curTime, a regeneration has taken place after the last insertion
    }
    return lastSimResults;
  }

  /**
   * Simulate the input activities.  We assume that the original plan activities have
   * been scheduled in the SimulationEngine and may be partially simulated.
   * @param schedule the activities to schedule with the times to schedule them
   */
  private void simulateSchedule(final Map<ActivityDirectiveId, ActivityDirective> schedule)
  {
    diffAndSimulate(schedule);
  }
  private void reallySimulateSchedule(final Map<ActivityDirectiveId, ActivityDirective> schedule)
  {
    if (debug) System.out.println("ResumableSimulationDriver.simulate(" + schedule + ")");

    if (schedule.isEmpty()) {
      throw new IllegalArgumentException("simulateSchedule() called with empty schedule, use simulateUntil() instead");
    }

    // Get all activities as close as possible to absolute time, then schedule all activities.
    // Using HashMap explicitly because it allows `null` as a key.
    // `null` key means that an activity is not waiting on another activity to finish to know its start time
    HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(
        planDuration,
        schedule).compute();
    // Filter out activities that are before the plan start
    resolved = StartOffsetReducer.filterOutNegativeStartOffset(resolved);

    scheduleActivities(
        schedule,
        resolved,
        missionModel,
        engine,
        activityTopic
    );

    var allTaskFinished = false;

    // Increment real time, if necessary.

    //once all tasks are finished, we need to wait for events triggered at the same time
    while (!allTaskFinished) {
      // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
      //   even if they occur at the same real time.

      // Run the jobs in this batch.
      engine.step(Duration.MAX_VALUE, queryTopic, $ -> {});

      // all tasks are complete : do not exit yet, there might be event triggered at the same time
      if (!plannedDirectiveToTask.isEmpty() && engine.timeOfNextJobs().longerThan(curTime()) &&
          plannedDirectiveToTask
          .values()
          .stream()
          .allMatch(engine::isTaskComplete)) {
        allTaskFinished = true;
      }

      if(engine.timeOfNextJobs().longerThan(planDuration)){
        break;
      }

    }
    if (useResourceTracker) {
      // Replay the timeline to collect resource profiles
      engine.generateResourceProfiles(curTime());
    }
    lastSimResults = null;
  }

  public void diffAndSimulate(
      Map<ActivityDirectiveId, ActivityDirective> activityDirectives) {
    Map<ActivityDirectiveId, ActivityDirective> directives = activityDirectives;
    engine.scheduledDirectives = new HashMap<>(activityDirectives);  // was null before this
    if (engine.oldEngine != null) {
      engine.directivesDiff = engine.oldEngine.diffDirectives(activityDirectives);
      if (debug) System.out.println("SimulationDriver: engine.directivesDiff = " + engine.directivesDiff);
      engine.oldEngine.scheduledDirectives = null;  // only keep the full schedule for the current engine to save space
      directives = new HashMap<>(engine.directivesDiff.get("added"));
      directives.putAll(engine.directivesDiff.get("modified"));
      engine.directivesDiff.get("modified").forEach((k, v) -> engine.removeTaskHistory(engine.oldEngine.getTaskIdForDirectiveId(k), Duration.MIN_VALUE));
      //engine.directivesDiff.get("removed").forEach((k, v) -> engine.removeTaskHistory(engine.oldEngine.getTaskIdForDirectiveId(k)));
      engine.directivesDiff.get("removed").forEach((k, v) -> engine.removeActivity(engine.oldEngine.getTaskIdForDirectiveId(k)));
    }
    if (directives.isEmpty()) {
      this.simulateUntil(this.planDuration);
    } else {
      this.reallySimulateSchedule(directives); //, simulationStartTime, simulationDuration, planStartTime, planDuration, doComputeResults, simulationExtentConsumer);
    }
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

      final TaskFactory<?> task;
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
      ), null);
      plannedDirectiveToTask.put(directiveId,taskId);
    }
  }

  private static <Model, Output> TaskFactory<Unit> makeTaskFactory(
      final ActivityDirectiveId directiveId,
      final TaskFactory<Output> task,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final Topic<ActivityDirectiveId> activityTopic
  )
  {
    // Emit the current activity (defined by directiveId)
    return executor -> scheduler0 -> TaskStatus.calling((TaskFactory<Output>) (executor1 -> scheduler1 -> {
      scheduler1.emit(directiveId, activityTopic);
      return task.create(executor1).step(scheduler1);
    }), scheduler2 -> {
      // When the current activity finishes, get the list of the activities that needed this activity to finish to know their start time
      final List<Pair<ActivityDirectiveId, Duration>> dependents = resolved.get(directiveId) == null ? List.of() : resolved.get(directiveId);
      // Iterate over the dependents
      for (final var dependent : dependents) {
        scheduler2.spawn(executor2 -> scheduler3 ->
            // Delay until the dependent starts
            TaskStatus.delayed(dependent.getRight(), scheduler4 -> {
              final var dependentDirectiveId = dependent.getLeft();
              final var serializedDependentDirective = schedule.get(dependentDirectiveId).serializedActivity();

              // Initialize the Task for the dependent
              final TaskFactory<?> dependantTask;
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
