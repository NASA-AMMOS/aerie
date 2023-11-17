package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.JobSchedule;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SimulationDriver<Model> {

  private static boolean debug = false;

  public static final boolean defaultUseResourceTracker = false;

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
  private ResourceTracker resourceTracker = null;
  private final boolean useResourceTracker;
  private final MissionModel<Model> missionModel;
  private Instant startTime;
  private final Duration planDuration;
  private JobSchedule.Batch<SimulationEngine.JobId> batch;

  private static final Topic<ActivityDirectiveId> activityTopic = SimulationEngine.defaultActivityTopic;

  private Topic<Topic<?>> queryTopic = new Topic<>();

  /** Whether we're rerunning the simulation, in which case we reuse past results and have an old SimulationEngine */
  private boolean rerunning = false;

  public SimulationDriver(MissionModel<Model> missionModel, Duration planDuration, final boolean useResourceTracker) {
    this(missionModel, Instant.now(), planDuration, useResourceTracker);
  }

  public SimulationDriver(
      MissionModel<Model> missionModel, Instant startTime, Duration planDuration,
      boolean useResourceTracker)
  {
    this.missionModel = missionModel;
    this.startTime = startTime;
    this.planDuration = planDuration;
    this.useResourceTracker = useResourceTracker;
    initSimulation(planDuration);
    batch = null;
  }


  public void initSimulation(final Duration simDuration) {
    if (debug) System.out.println("SimulationDriver.initSimulation()");
    // If rerunning the simulation, reuse the existing SimulationEngine to avoid redundant computation
    this.rerunning = this.engine != null && this.engine.timeline.commitsByTime.size() > 1;
    if (this.engine != null) this.engine.close();
    SimulationEngine oldEngine = rerunning ? this.engine : null;

    this.engine = new SimulationEngine(startTime, missionModel, oldEngine, resourceTracker);
    if (useResourceTracker) {
      this.resourceTracker = new ResourceTracker(engine, missionModel.getInitialCells());
      engine.resourceTracker = this.resourceTracker; // yes, this looks strange following the lines above
    }

    //assert useResourceTracker;

    /* The current real time. */
    //setCurTime(Duration.ZERO);

    // Begin tracking any resources that have not already been simulated.
    trackResources();

    // Start daemon task(s) immediately, before anything else happens.
    startDaemons(curTime());

    // The sole purpose of this task is to make sure the simulation has "stuff to do" until the simulationDuration.
    engine.scheduleTask(
        simDuration,
        executor -> $ -> TaskStatus.completed(Unit.UNIT),
        null); // TODO: skip this if rerunning? and end time is same?
  }


  public static <Model> SimulationResultsInterface simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration)
  {
    return simulate(missionModel, schedule, simulationStartTime, simulationDuration, planStartTime, planDuration,
                    defaultUseResourceTracker, $ -> {});
  }

  public static <Model> SimulationResultsInterface simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final boolean useResourceTracker,
      final Consumer<Duration> simulationExtentConsumer
  )
  {
    var driver = new SimulationDriver<>(missionModel, simulationStartTime, simulationDuration, useResourceTracker);
    return driver.simulate(schedule, simulationStartTime, simulationDuration, planStartTime, planDuration, simulationExtentConsumer);
  }

  public SimulationResultsInterface simulate(
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration
  )
  {
    return simulate(schedule, simulationStartTime, simulationDuration, planStartTime, planDuration, true, $ -> {});
  }

  public SimulationResultsInterface simulate(
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final Consumer<Duration> simulationExtentConsumer
  )
  {
    return simulate(schedule, simulationStartTime, simulationDuration, planStartTime, planDuration, true, simulationExtentConsumer);
  }

  public SimulationResultsInterface simulate(
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final boolean doComputeResults,
      final Consumer<Duration> simulationExtentConsumer
  )
  {
    try {
      if (debug) System.out.println("SimulationDriver.simulate(" + schedule + ")");

      if (engine.scheduledDirectives == null) {
        engine.scheduledDirectives = new HashMap<>(schedule);
      }

      simulationExtentConsumer.accept(curTime());

      // Get all activities as close as possible to absolute time
      // Schedule all activities.
      // Using HashMap explicitly because it allows `null` as a key.
      // `null` key means that an activity is not waiting on another activity to finish to know its start time
      HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(
          planDuration,
          schedule).compute();
      if (resolved.size() != 0) {
        resolved.put(
            null,
            StartOffsetReducer.adjustStartOffset(
                resolved.get(null),
                Duration.of(
                    planStartTime.until(simulationStartTime, ChronoUnit.MICROS),
                    Duration.MICROSECONDS)));
      }
      // Filter out activities that are before simulationStartTime
      resolved = StartOffsetReducer.filterOutNegativeStartOffset(resolved);

      scheduleActivities(
          schedule,
          resolved,
          missionModel,
          engine,
          engine.defaultActivityTopic
      );

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (engine.hasJobsScheduledThrough(simulationDuration)) {
        engine.step(simulationDuration, queryTopic, simulationExtentConsumer);
      }
    } catch (Throwable ex) {
      throw new SimulationException(curTime(), simulationStartTime, ex);
    }

    // A query depends on an event if
    // - that event has the same topic as the query
    // - that event occurs causally before the query

    // Let A be an event or query issued by task X, and B be either an event or query issued by task Y
    // A flows to B if B is causally after A and
    // - X = Y
    // - X spawned Y causally after A
    // - Y called X, and emitted B after X terminated
    // - Transitively: if A flows to C and C flows to B, A flows to B
    // tstill not enough...?

    if (doComputeResults) {
      return engine.computeResults(startTime, simulationDuration, activityTopic);
    }
    return null;
  }

  private void startDaemons(Duration time) {
    if (!this.rerunning) {
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon(), null);
      engine.step(Duration.MAX_VALUE, queryTopic, $ -> {});
    }
  }

  private void trackResources() {
    // Begin tracking any resources that have not already been simulated.
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

  public SimulationResultsInterface diffAndSimulate(
      Map<ActivityDirectiveId, ActivityDirective> activityDirectives,
      Instant simulationStartTime,
      Duration simulationDuration,
      Instant planStartTime,
      Duration planDuration) {
    return diffAndSimulate(activityDirectives, simulationStartTime,simulationDuration, planStartTime, planDuration,
                           true, $ -> {});
  }

  public SimulationResultsInterface diffAndSimulate(
      Map<ActivityDirectiveId, ActivityDirective> activityDirectives,
      Instant simulationStartTime,
      Duration simulationDuration,
      Instant planStartTime,
      Duration planDuration,
      boolean doComputeResults,
      final Consumer<Duration> simulationExtentConsumer) {
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
    return this.simulate(directives, simulationStartTime, simulationDuration, planStartTime, planDuration, doComputeResults, simulationExtentConsumer);
  }

  public <Return> //static <Model, Return>
  void simulateTask(final TaskFactory<Return> task) {
    if (debug) System.out.println("SimulationDriver.simulateTask(" + task + ")");

    // Schedule all activities.
    final var taskId = engine.scheduleTask(curTime(), task, null);

    // Drive the engine until we're out of time.
    // TERMINATION: Actually, we might never break if real time never progresses forward.
    while (!engine.isTaskComplete(taskId)) {
      engine.step(Duration.MAX_VALUE, queryTopic, $ -> {});
    }
    if (useResourceTracker) {
      engine.generateResourceProfiles(curTime());  // REVIEW: Is this necessary?
                                                   // Okay to keep here since work is not lost for resourceTracker.
    }
  }

  private static <Model> void scheduleActivities(
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

      engine.scheduleTask(startOffset,
                          makeTaskFactory(directiveId,
                                          task,
                                          schedule,
                                          resolved,
                                          missionModel,
                                          activityTopic),
                          null);
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

  public SimulationResultsInterface computeResults(Instant startTime, Duration simDuration) {
    return engine.computeResults(startTime, simDuration, SimulationEngine.defaultActivityTopic);
  }

  public SimulationEngine getEngine() {
    return engine;
  }

  public MissionModel<Model> getMissionModel() {
    return missionModel;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Duration getPlanDuration() {
    return planDuration;
  }
}
