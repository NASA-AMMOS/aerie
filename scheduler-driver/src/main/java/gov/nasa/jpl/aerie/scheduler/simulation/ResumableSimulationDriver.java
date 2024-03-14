package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
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
import gov.nasa.jpl.aerie.scheduler.NotNull;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ResumableSimulationDriver<Model> implements AutoCloseable {
  private final Supplier<Boolean> canceledListener;

  public long durationSinceRestart = 0;

  private static final Logger logger = LoggerFactory.getLogger(ResumableSimulationDriver.class);
  /* The current real time. All the tasks before and at this time have been performed.
 Simulation has not started so it is set to MIN_VALUE. */
  private Duration curTime = Duration.MIN_VALUE;
  private SimulationEngine engine = new SimulationEngine();
  private LiveCells cells;
  private TemporalEventSource timeline = new TemporalEventSource();
  private final MissionModel<Model> missionModel;
  private final Duration planDuration;
  private JobSchedule.Batch<SimulationEngine.JobId> batch;

  private final Topic<ActivityDirectiveId> activityTopic = new Topic<>();

  //mapping each activity name to its task id (in String form) in the simulation engine
  private final Map<ActivityDirectiveId, TaskId> plannedDirectiveToTask;

  //subset of plannedDirectiveToTask to check for scheduling dependent tasks
  private final Map<ActivityDirectiveId, TaskId> toCheckForDependencyScheduling;

  //simulation results so far
  private SimulationResults lastSimResults;
  //cached simulation results cover the period [Duration.ZERO, lastSimResultsEnd]
  private Duration lastSimResultsEnd = Duration.ZERO;

  //List of activities simulated since the last reset
  private final Map<ActivityDirectiveId, ActivityDirective> activitiesInserted = new HashMap<>();

  //counts the number of simulation restarts, used as performance metric in the scheduler
  //effectively counting the number of calls to initSimulation()
  private int countSimulationRestarts;

  public ResumableSimulationDriver(
      MissionModel<Model> missionModel,
      Duration planDuration,
      Supplier<Boolean> canceledListener
  ){
    this.missionModel = missionModel;
    plannedDirectiveToTask = new HashMap<>();
    toCheckForDependencyScheduling = new HashMap<>();
    this.planDuration = planDuration;
    countSimulationRestarts = 0;
    this.canceledListener = canceledListener;
    initSimulation();
  }


  private void printTimeSpent(){
    final var dur = durationSinceRestart/1_000_000_000.;
    final var average = curTime.shorterThan(Duration.of(1, Duration.SECONDS)) ? 0 : dur/curTime.in(Duration.SECONDS);
    if(dur != 0) {
      logger.info("Time spent in the last sim " + dur + "s, average per simulation second " + average + "s. Simulated until " + curTime);
    }
  }

  // This method is currently only used in one test.
  /*package-private*/ void clearActivitiesInserted() {activitiesInserted.clear();}

  /*package-private*/ void initSimulation(){
    logger.info("Reinitialization of the scheduling simulation");
    printTimeSpent();
    durationSinceRestart = 0;
    plannedDirectiveToTask.clear();
    toCheckForDependencyScheduling.clear();
    lastSimResults = null;
    lastSimResultsEnd = Duration.ZERO;
    long before = System.nanoTime();
    if (this.engine != null) this.engine.close();
    this.engine = new SimulationEngine();
    batch = null;
    /* The top-level simulation timeline. */
    this.timeline = new TemporalEventSource();
    this.cells = new LiveCells(timeline, missionModel.getInitialCells());
    curTime = Duration.MIN_VALUE;

    // Begin tracking all resources.
    for (final var entry : missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      engine.trackResource(name, resource, Duration.ZERO);
    }

    // Start daemon task(s) immediately, before anything else happens.
    {
      if(missionModel.hasDaemons()) {
        engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
        batch = engine.extractNextJobs(Duration.MAX_VALUE);
      }
    }
    this.durationSinceRestart += System.nanoTime() - before;
    countSimulationRestarts++;
  }

  /**
   * Return the number of simulation restarts
   * @return the number of simulation restarts
   */
  public int getCountSimulationRestarts(){
    return countSimulationRestarts;
  }

  @Override
  public void close() {
    logger.debug("Closing sim");
    printTimeSpent();
    this.engine.close();
  }

  private void simulateUntil(Duration endTime) throws SchedulingInterruptedException{
    long before = System.nanoTime();
    logger.info("Simulating until "+endTime);
    assert(endTime.noShorterThan(curTime));
      if(batch == null){
        batch = engine.extractNextJobs(Duration.MAX_VALUE);
      }
      // Increment real time, if necessary.
      while(!batch.offsetFromStart().longerThan(endTime) && !endTime.isEqualTo(Duration.MAX_VALUE)) {
        if(canceledListener.get()) throw new SchedulingInterruptedException("simulating");
        //by default, curTime is negative to signal we have not started simulation yet. We set it to 0 when we start.
        final var delta = batch.offsetFromStart().minus(curTime.isNegative() ? Duration.ZERO : curTime);
        curTime = batch.offsetFromStart();
        timeline.add(delta);
        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE);
        timeline.add(commit);

        batch = engine.extractNextJobs(Duration.MAX_VALUE);
      }
      lastSimResults = null;
      this.durationSinceRestart += (System.nanoTime() - before);
  }


  /**
   * Simulate an activity directive.
   * @param activity the serialized type and arguments of the activity directive to be simulated
   * @param startOffset the start offset from the activity's anchor
   * @param anchorId the activity id of the anchor (or null if the activity is anchored to the plan)
   * @param anchoredToStart toggle for if the activity is anchored to the start or end of its anchor
   * @param activityId the activity id for the activity to simulate
   */
  public void simulateActivity(
      final Duration startOffset,
      final SerializedActivity activity,
      final ActivityDirectiveId anchorId,
      final boolean anchoredToStart,
      final ActivityDirectiveId activityId
  ) throws SchedulingInterruptedException {
    simulateActivity(new ActivityDirective(startOffset, activity, anchorId, anchoredToStart), activityId);
  }

  /**
   * Simulate an activity directive.
   * @param activityToSimulate the activity directive to simulate
   * @param activityId the ActivityDirectiveId for the activity to simulate
   */
  public void simulateActivity(ActivityDirective activityToSimulate, ActivityDirectiveId activityId)
  throws SchedulingInterruptedException {
    simulateActivities(Map.of(activityId, activityToSimulate));
  }

  public void simulateActivities(@NotNull Map<ActivityDirectiveId, ActivityDirective> activitiesToSimulate)
  throws SchedulingInterruptedException {
    if(activitiesToSimulate.isEmpty()) return;

    activitiesInserted.putAll(activitiesToSimulate);

    final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(planDuration, activitiesToSimulate).compute();
    resolved.get(null).sort(Comparator.comparing(Pair::getRight));
    final var earliestStartOffset = resolved.get(null).get(0);

    if(earliestStartOffset.getRight().noLongerThan(curTime)){
      logger.info("Restarting simulation because earliest start of activity to simulate " + earliestStartOffset.getRight() + " is before current sim time " + curTime);
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
  public SimulationResults getSimulationResults(Instant startTimestamp) throws SchedulingInterruptedException {
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
  public SimulationResults getSimulationResultsUpTo(Instant startTimestamp, Duration endTime)
  throws SchedulingInterruptedException {
    //if previous results cover a bigger period, we return do not regenerate
    if(endTime.longerThan(curTime)){
      logger.info("Simulating from " + curTime + " to " + endTime + " to get simulation results");
      simulateUntil(endTime);
    } else{
      logger.info("Not simulating because asked endTime "+endTime+" is before current sim time " + curTime);
    }
    final var before = System.nanoTime();
    if(lastSimResults == null || endTime.longerThan(lastSimResultsEnd) || startTimestamp.compareTo(lastSimResults.startTime) != 0) {
      if(canceledListener.get()) throw new SchedulingInterruptedException("computing simulation results");
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
    this.durationSinceRestart += System.nanoTime() - before;

    return lastSimResults;
  }

  private void simulateSchedule(final Map<ActivityDirectiveId, ActivityDirective> schedule)
  throws SchedulingInterruptedException {
    final var before = System.nanoTime();
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
    final var toSchedule = new HashSet<ActivityDirectiveId>();
    toSchedule.add(null);
    scheduleActivities(
        toSchedule,
        schedule,
        resolved,
        missionModel,
        engine
    );

    var allTaskFinished = false;

    if (batch == null) {
      batch = engine.extractNextJobs(Duration.MAX_VALUE);
    }
    //by default, curTime is negative to signal we have not started simulation yet. We set it to 0 when we start.
    Duration delta = batch.offsetFromStart().minus(curTime.isNegative() ? Duration.ZERO : curTime);

    //once all tasks are finished, we need to wait for events triggered at the same time
    while (!allTaskFinished || delta.isZero()) {
      if(canceledListener.get()) throw new SchedulingInterruptedException("simulating");

      curTime = batch.offsetFromStart();
      timeline.add(delta);
      // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
      //   even if they occur at the same real time.

      // Run the jobs in this batch.
      final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE);
      timeline.add(commit);

      scheduleActivities(getSuccessorsToSchedule(engine), schedule, resolved, missionModel, engine);

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
      if(batch.offsetFromStart().longerThan(planDuration)){
        break;
      }
    }
    lastSimResults = null;
    this.durationSinceRestart+= System.nanoTime() - before;
  }

  /**
   * Returns the duration of a terminated simulated activity
   * @param activityDirectiveId the activity id
   * @return its duration if the activity has been simulated and has finished simulating, an IllegalArgumentException otherwise
   */
  public Optional<Duration> getActivityDuration(ActivityDirectiveId activityDirectiveId){
    //potential cause of non presence: (1) activity is outside plan bounds (2) activity has not been simulated yet
    if(!plannedDirectiveToTask.containsKey(activityDirectiveId)) return Optional.empty();
    return engine.getTaskDuration(plannedDirectiveToTask.get(activityDirectiveId));
  }

  private Set<ActivityDirectiveId> getSuccessorsToSchedule(final SimulationEngine engine) {
    final var toSchedule = new HashSet<ActivityDirectiveId>();
    final var iterator = toCheckForDependencyScheduling.entrySet().iterator();
    while(iterator.hasNext()){
      final var taskToCheck = iterator.next();
      if(engine.isTaskComplete(taskToCheck.getValue())){
        toSchedule.add(taskToCheck.getKey());
        iterator.remove();
      }
    }
    return toSchedule;
  }

  private void scheduleActivities(
      final Set<ActivityDirectiveId> toScheduleNow,
      final Map<ActivityDirectiveId, ActivityDirective> completeSchedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final SimulationEngine engine){
    for(final var predecessor: toScheduleNow) {
      for (final var directivePair : resolved.get(predecessor)) {
        final var offset = directivePair.getRight();
        final var directiveIdToSchedule = directivePair.getLeft();
        final var serializedDirective = completeSchedule.get(directiveIdToSchedule).serializedActivity();
        final TaskFactory<?> task;
        try {
          task = missionModel.getTaskFactory(serializedDirective);
        } catch (final InstantiationException ex) {
          // All activity instantiations are assumed to be validated by this point
          throw new Error("Unexpected state: activity instantiation %s failed with: %s"
                              .formatted(serializedDirective.getTypeName(), ex.toString()));
        }
        Duration computedStartTime = offset;
        if (predecessor != null) {
          computedStartTime = (curTime.isEqualTo(Duration.MIN_VALUE) ? Duration.ZERO : curTime).plus(offset);
        }
        final var taskId = engine.scheduleTask(
            computedStartTime,
            makeTaskFactory(directiveIdToSchedule, task, activityTopic));
        plannedDirectiveToTask.put(directiveIdToSchedule, taskId);
        if (resolved.containsKey(directiveIdToSchedule)) {
          toCheckForDependencyScheduling.put(directiveIdToSchedule, taskId);
        }
      }
    }
  }

  private static <Output> TaskFactory<Output> makeTaskFactory(
      final ActivityDirectiveId directiveId,
      final TaskFactory<Output> task,
      final Topic<ActivityDirectiveId> activityTopic) {
    return executor -> SimulationDriver.oneShotTask(
        scheduler -> {
          scheduler.emit(directiveId, activityTopic);
          return task.create(executor).step(scheduler);
        });
  }
}
