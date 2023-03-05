package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class IncrementalSimulationDriver<Model> {

  private Duration curTime = Duration.ZERO;
  private SimulationEngine engine;
  private LiveCells cells;
  private LiveCells oldCells;
  //private TemporalEventSource timeline = new TemporalEventSource();
  private final MissionModel<Model> missionModel;

  private Instant startTime;

  private final Topic<ActivityInstanceId> activityTopic = new Topic<>();

  //mapping each activity name to its task id (in String form) in the simulation engine
  private final Map<ActivityInstanceId, TaskId> plannedDirectiveToTask;

  //simulation results so far
  private SimulationResults lastSimResults;
  //cached simulation results cover the period [Duration.ZERO, lastSimResultsEnd]
  private Duration lastSimResultsEnd = Duration.ZERO;

  //List of activities simulated since the last reset
  private final List<SimulatedActivity> activitiesInserted = new ArrayList<>();
  private Topic<Topic<?>> queryTopic = new Topic<>();

  // Whether we're rerunning the simulation, in which case we can be lazy about starting up stuff, like daemons
  private boolean rerunning = false;

  record SimulatedActivity(Duration start, SerializedActivity activity, ActivityInstanceId id) {}

  public IncrementalSimulationDriver(Instant startTime, MissionModel<Model> missionModel){
    this.startTime = startTime;
    this.missionModel = missionModel;
    plannedDirectiveToTask = new HashMap<>();
    initSimulation();
  }

  /*package-private*/ void initSimulation(){
    plannedDirectiveToTask.clear();
    lastSimResults = null;
    lastSimResultsEnd = Duration.ZERO;
    // If rerunning the simulation, reuse the existing SimulationEngine to avoid redundant computation
    this.rerunning = this.engine != null && this.cells.size() > 0;
    if (this.engine != null) this.engine.close();
    SimulationEngine oldEngine = rerunning ? this.engine : null;
    this.engine = new SimulationEngine(startTime, missionModel, oldEngine);
    activitiesInserted.clear();

    /* The top-level simulation timeline. */
    // this.timeline = new TemporalEventSource();
    this.cells = new LiveCells(engine.timeline, missionModel.getInitialCells());
    this.oldCells = oldEngine == null ? null : new LiveCells(oldEngine.timeline, oldEngine.getMissionModel().getInitialCells());

    /* The current real time. */
    curTime = Duration.ZERO;

    // Begin tracking any resources that have not already been simulated.
    for (final var entry : missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      if (!rerunning || !oldEngine.hasSimulatedResource(name)) {
        engine.trackResource(name, resource, curTime);
      }
    }

    // Start daemon task(s) immediately, before anything else happens.
    if (!rerunning) {
      startDaemons(curTime);
    }
  }

  private void startDaemons(Duration time) {
    engine.scheduleTask(time, missionModel.getDaemon());

    final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
    final var commit = engine.performJobs(batch.jobs(), cells, time, Duration.MAX_VALUE, queryTopic);
    engine.timeline.add(commit, time);
  }

  //
  private void simulateUntil(Duration endTime){
    assert(endTime.noShorterThan(curTime));
    while (true) {
      var timeOfNextJobs = engine.timeOfNextJobs();
      var nextTime = Duration.min(timeOfNextJobs, endTime.plus(Duration.EPSILON));

      var earliestStaleTopics = engine.earliestStaleTopics(nextTime);  // might want to not limit by nextTime and cache for future iterations
      var staleTopicTime = earliestStaleTopics.getRight();
      nextTime = Duration.min(nextTime, staleTopicTime);

      var earliestStaleReads = engine.earliestStaleReads(curTime, nextTime);  // might want to not limit by nextTime and cache for future iterations
      var staleReadTime = earliestStaleReads.getLeft();
      nextTime = Duration.min(nextTime, staleReadTime);

      // Increment real time, if necessary.
      final var delta = nextTime.minus(curTime);
      if(nextTime.longerThan(endTime) || endTime.isEqualTo(Duration.MAX_VALUE)){  // should this be nextTime.isEqualTo(Duration.MAX_VALUE)?
        break;
      }
      curTime = nextTime;
      engine.timeline.add(delta);

      if (staleTopicTime.isEqualTo(nextTime)) {
        // TODO: HERE!!
      }

      if (staleReadTime.isEqualTo(nextTime)) {
        // TODO: HERE!!
      }

      if (timeOfNextJobs.isEqualTo(nextTime)) {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE, queryTopic);
        engine.timeline.add(commit, curTime);
      }

    }
    lastSimResults = null;
  }


  /**
   * Simulate an activity
   * @param activity the activity to simulate
   * @param startTime the start time of the activity
   * @param activityId the activity id for the activity to simulate
   * @throws InstantiationException
   */
  public void simulateActivity(SerializedActivity activity, Duration startTime, ActivityInstanceId activityId)
  throws InstantiationException
  {
    final var activityToSimulate = new SimulatedActivity(startTime, activity, activityId);
    if(startTime.noLongerThan(curTime)){
      final var toBeInserted = new ArrayList<>(activitiesInserted);
      toBeInserted.add(activityToSimulate);
      initSimulation();
      final var schedule = toBeInserted
          .stream()
          .collect(Collectors.toMap( e -> e.id, e->Pair.of(e.start, e.activity)));
      simulateSchedule(schedule);
      activitiesInserted.addAll(toBeInserted);
    } else {
      final var schedule = Map.of(activityToSimulate.id,
                                  Pair.of(activityToSimulate.start, activityToSimulate.activity));
      simulateSchedule(schedule);
      activitiesInserted.add(activityToSimulate);
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
      lastSimResults = engine.computeResults(
          startTimestamp,
          endTime,
          activityTopic);
      lastSimResultsEnd = endTime;
      //while sim results may not be up to date with curTime, a regeneration has taken place after the last insertion
    }
    return lastSimResults;
  }

  private void simulateSchedule(final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule)
  throws InstantiationException
  {

    if(schedule.isEmpty()){
      throw new IllegalArgumentException("simulateSchedule() called with empty schedule, use simulateUntil() instead");
    }

    for (final var entry : schedule.entrySet()) {
      final var directiveId = entry.getKey();
      final var startOffset = entry.getValue().getLeft();
      final var serializedDirective = entry.getValue().getRight();

      final var task = missionModel.getTaskFactory(serializedDirective);
      final var taskId = engine.scheduleTask(startOffset, emitAndThen(directiveId, this.activityTopic, task));

      plannedDirectiveToTask.put(directiveId,taskId);
    }
    var allTaskFinished = false;
    while (true) {
      var timeOfNextJobs = engine.timeOfNextJobs();
      var nextTime = timeOfNextJobs;

      var earliestStaleTopics = engine.earliestStaleTopics(nextTime);  // might want to not limit by nextTime and cache for future iterations
      var staleTopicTime = earliestStaleTopics.getRight();
      nextTime = Duration.min(nextTime, staleTopicTime);

      var earliestStaleReads = engine.earliestStaleReads(curTime, nextTime);  // might want to not limit by nextTime and cache for future iterations
      var staleReadTime = earliestStaleReads.getLeft();
      nextTime = Duration.min(nextTime, staleReadTime);

      final var delta = nextTime.minus(curTime);
      //once all tasks are finished, we need to wait for events triggered at the same time
      if(allTaskFinished && !delta.isZero()){
        break;
      }
      // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
      //   even if they occur at the same real time.

      curTime = nextTime;
      engine.timeline.add(delta);

      if (staleTopicTime.isEqualTo(nextTime)) {
        // TODO: HERE!!
      }

      if (staleReadTime.isEqualTo(nextTime)) {
        // TODO: HERE!!
      }

      if (timeOfNextJobs.isEqualTo(nextTime)) {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE, queryTopic);
        engine.timeline.add(commit, curTime);
      }

      // all tasks are complete : do not exit yet, there might be event triggered at the same time
      if (!plannedDirectiveToTask.isEmpty() && plannedDirectiveToTask.values().stream().allMatch(engine::isTaskComplete)) {
        allTaskFinished = true;
      }

    }
    lastSimResults = null;
  }

  /**
   * Returns the duration of a terminated simulated activity
   * @param activityInstanceId the activity id
   * @return its duration if the activity has been simulated and has finished simulating, an IllegalArgumentException otherwise
   */
  public Optional<Duration> getActivityDuration(ActivityInstanceId activityInstanceId){
    return engine.getTaskDuration(plannedDirectiveToTask.get(activityInstanceId));
  }

  private static <E, T>
  TaskFactory<T> emitAndThen(final E event, final Topic<E> topic, final TaskFactory<T> continuation) {
    return executor -> scheduler -> {
      scheduler.emit(event, topic);
      return continuation.create(executor).step(scheduler);
    };
  }
}
