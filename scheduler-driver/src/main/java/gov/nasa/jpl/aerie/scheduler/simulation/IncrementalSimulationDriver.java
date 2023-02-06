package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IncrementalSimulationDriver<Model> {

  private Duration curTime = Duration.ZERO;
  private SimulationEngine engine = new SimulationEngine();
  private LiveCells cells;
  private TemporalEventSource timeline = new TemporalEventSource();
  private final MissionModel<Model> missionModel;

  private final Topic<ActivityInstanceId> activityTopic = new Topic<>();

  //mapping each activity name to its task id (in String form) in the simulation engine
  private final Map<ActivityInstanceId, TaskId> plannedDirectiveToTask;

  //simulation results so far
  private SimulationResults lastSimResults;
  //cached simulation results cover the period [Duration.ZERO, lastSimResultsEnd]
  private Duration lastSimResultsEnd = Duration.ZERO;

  //List of activities simulated since the last reset
  private final List<SimulatedActivity> activitiesInserted = new ArrayList<>();
  private Schedule previousSchedule = Schedule.empty();

  private Map<ActivityInstanceId, ActivityStatus> activityStatus = new HashMap<>();

  record SimulatedActivity(Duration start, SerializedActivity activity, ActivityInstanceId id) {}

  public IncrementalSimulationDriver(MissionModel<Model> missionModel){
    this.missionModel = missionModel;
    plannedDirectiveToTask = new HashMap<>();
    initSimulation();
  }

  /*package-private*/ void initSimulation(){
    plannedDirectiveToTask.clear();
    lastSimResults = null;
    lastSimResultsEnd = Duration.ZERO;
    if (this.engine != null) this.engine.close();
    this.engine = new SimulationEngine();
    activitiesInserted.clear();
    activityStatus.clear();

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

      final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
      final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE);
      timeline.add(commit);
    }
  }

  /**
   * Simulate an activity
   * @param activity the activity to simulate
   * @param startTime the start time of the activity
   * @param activityId the activity id for the activity to simulate
   * @throws InstantiationException
   */
  public void simulateActivity(Schedule newSchedule, SerializedActivity activity, Duration startTime, ActivityInstanceId activityId)
  throws InstantiationException
  {
    if (!newSchedule.contains(new StartTime.OffsetFromPlanStart(startTime), activity)) {
      throw new AssertionError("Nope! don't do that. " + activity + " " + newSchedule);
    }
    var firstDifference = Schedule.firstDifference(previousSchedule, newSchedule);
    previousSchedule = newSchedule;
    if (firstDifference.isEmpty()) {
      if (activityStatus.containsKey(activityId) && activityStatus.get(activityId) == ActivityStatus.FINISHED) {
        // TODO: Check if the given activity has finished, or if we should roll the simulation forward
        return;
      } else {
        firstDifference = Optional.of(startTime);
      }
    }

    final var activityToSimulate = new SimulatedActivity(startTime, activity, activityId);
    if (firstDifference.get().noLongerThan(curTime)){
      final var toBeInserted = new ArrayList<>(activitiesInserted);
      toBeInserted.add(activityToSimulate);
      initSimulation();
      final var schedule = toBeInserted
          .stream()
          .collect(Collectors.toMap( e -> e.id, e->Pair.of(e.start, e.activity)));
      simulateNewSchedule(newSchedule, schedule);
      activitiesInserted.addAll(toBeInserted);
    } else {
      final var schedule = Map.of(activityToSimulate.id,
                                  Pair.of(activityToSimulate.start, activityToSimulate.activity));
      extendExistingSchedule(newSchedule, schedule);
      activitiesInserted.add(activityToSimulate);
    }
  }

  private void extendExistingSchedule(
      final Schedule newSchedule,
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule) throws InstantiationException
  {
    simulateSchedule(newSchedule, schedule, new StopCondition.ActivityCompletion(schedule.keySet()));
  }

  private void simulateNewSchedule(
      final Schedule newSchedule,
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule) throws InstantiationException
  {
    simulateSchedule(newSchedule, schedule, new StopCondition.ActivityCompletion(schedule.keySet()));
  }

  public void simulateActivities(final Schedule schedule, final Set<ActivityInstanceId> activities) {
    for (final var activityId : activities) {
      if (!schedule.activitiesById().containsKey(activityId)) {
        throw new IllegalArgumentException("Cannot simulate activity id: " + activityId + " because it is not present in schedule: " + schedule);
      }
    }
    final var firstDifference = Schedule.firstDifference(previousSchedule, schedule);
    previousSchedule = schedule;

    if (firstDifference.get().noLongerThan(curTime)) {
      initSimulation();

    }
  }


  /**
   * Get the simulation results from the Duration.ZERO to the current simulation time point
   * @param startTimestamp the timestamp for the start of the planning horizon. Used as epoch for computing SimulationResults.
   * @return the simulation results
   */
  public SimulationResults getSimulationResults(Schedule schedule, Instant startTimestamp){
    return getSimulationResultsUpTo(schedule, startTimestamp, curTime);
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
  public SimulationResults getSimulationResultsUpTo(final Schedule schedule, Instant startTimestamp, Duration endTime){
    //if previous results cover a bigger period, we return do not regenerate
    if(endTime.longerThan(curTime)){
      try {
        simulateSchedule(
            schedule,
            schedule
                .activitiesById()
                .entrySet()
                .stream()
                .map($ -> Pair.of($.getKey(), Pair.of(((StartTime.OffsetFromPlanStart) $.getValue().startTime()).offset(), $.getValue().serializedActivity())))
                .collect(Collectors.toMap(Pair::getKey, Pair::getRight)), new StopCondition.ElapsedTime(endTime));
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      }
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

    for (final var id : lastSimResults.simulatedActivities.keySet()) {
      if (!schedule.activitiesById().containsKey(id)) {
        throw new IllegalStateException("Results contain " + id + " but schedule does not " + schedule);
      }
    }
    return lastSimResults;
  }

  private void simulateSchedule(final Schedule wholeSchedule, final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule, final StopCondition stopCondition)
  throws InstantiationException
  {
    if (stopCondition instanceof final StopCondition.ElapsedTime s) {
      if (!s.endTime().noShorterThan(curTime)) throw new AssertionError("endTime <= curTime: "
                                                                        + s.endTime()
                                                                        + " <= "
                                                                        + curTime);
    }

    var allTaskFinished = false;
    var nextTaskStart = schedule.values().stream().map(Pair::getLeft).min(Comparator.comparing($ -> $)).orElse(Duration.MAX_VALUE);
    while (true) {
      final var nextBatchStart = engine.peekNextBatch(Duration.MAX_VALUE);

      //once all tasks are finished, we need to wait for events triggered at the same time
      if (stopCondition instanceof StopCondition.ActivityCompletion s) {
        if (allTaskFinished
            && !nextBatchStart.minus(curTime).isZero()) { // TODO: Does this drop the batch on the floor? If we try to resume the simulation, will those jobs be lost? Answer: Yes, but that batch is always empty.
          break;
        }
      } else if (stopCondition instanceof StopCondition.ElapsedTime s) {
        if (engine.peekNextBatch(Duration.MAX_VALUE).longerThan(s.endTime()) || s.endTime().isEqualTo(Duration.MAX_VALUE)) {
          break;
        }
      } else {
        throw new Error("Unhandled variant of StopCondition: " + stopCondition);
      }

      if (nextTaskStart.shorterThan(Duration.MAX_VALUE) && nextBatchStart.noShorterThan(nextTaskStart)) {
        nextTaskStart = Duration.MAX_VALUE;
        for (final var entry : schedule.entrySet()) {
          final var startOffset = entry.getValue().getLeft();
          final var directiveId = entry.getKey();

          activityStatus.putIfAbsent(directiveId, ActivityStatus.NOT_STARTED);
          final var status = activityStatus.get(directiveId);

          if (status != ActivityStatus.NOT_STARTED) continue;
          if (startOffset.longerThan(nextBatchStart)) {
            nextTaskStart = Duration.min(nextTaskStart, startOffset);
            continue;
          }

          final var serializedDirective = entry.getValue().getRight();

          final var task = missionModel.getTaskFactory(serializedDirective);
          final var taskId = engine.scheduleTask(startOffset, emitAndThen(directiveId, this.activityTopic, task));
          activityStatus.put(directiveId, ActivityStatus.STARTED);

          plannedDirectiveToTask.put(directiveId,taskId);
        }
      }

      final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
      final var delta = batch.offsetFromStart().minus(curTime);
      curTime = batch.offsetFromStart();
      timeline.add(delta);
      // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
      //   even if they occur at the same real time.

      // Run the jobs in this batch.
      final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE);
      timeline.add(commit);

      for (final var activityId : schedule.keySet()) {
        if (plannedDirectiveToTask.containsKey(activityId)) {
          if (engine.isTaskComplete(plannedDirectiveToTask.get(activityId))) {
            activityStatus.put(activityId, ActivityStatus.FINISHED);
          }
        }
      }

      // all tasks are complete : do not exit yet, there might be event triggered at the same time
      if (stopCondition instanceof StopCondition.ActivityCompletion s && s.activities().stream().allMatch($ -> {
        activityStatus.putIfAbsent($, ActivityStatus.NOT_STARTED);
        return activityStatus.get($) == ActivityStatus.FINISHED;
      })) {
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
    final var taskId = plannedDirectiveToTask.get(activityInstanceId);
    if (taskId == null) return Optional.empty();
    return engine.getTaskDuration(taskId);
  }

  private static <E, T>
  TaskFactory<T> emitAndThen(final E event, final Topic<E> topic, final TaskFactory<T> continuation) {
    return executor -> scheduler -> {
      scheduler.emit(event, topic);
      return continuation.create(executor).step(scheduler);
    };
  }

  enum ActivityStatus {
    NOT_STARTED,
    STARTED,
    FINISHED
  }

  sealed interface StopCondition {
    record ActivityCompletion(Set<ActivityInstanceId> activities) implements StopCondition {}
    record ElapsedTime(Duration endTime) implements StopCondition {}
  }
}
