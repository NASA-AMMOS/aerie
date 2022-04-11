package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IncrementalSimulationDriver {

  private Duration curTime = Duration.ZERO;
  private SimulationEngine engine = new SimulationEngine();
  private LiveCells cells;
  private TemporalEventSource timeline = new TemporalEventSource();
  private final MissionModel<?> missionModel;

  //mapping each activity name to its task id (in String form) in the simulation engine
  private final Map<ActivityInstanceId, TaskId> plannedDirectiveToTask;
  //and the opposite
  private final Map<TaskId, ActivityInstanceId> taskToPlannedDirective;

  //simulation results so far
  private SimulationResults lastSimResults;
  //cached simulation results cover the period [Duration.ZERO, lastSimResultsEnd]
  private Duration lastSimResultsEnd = Duration.ZERO;

  //List of activities simulated since the last reset
  private final List<SimulatedActivity> activitiesInserted = new ArrayList<>();

  record SimulatedActivity(Duration start, SerializedActivity activity, ActivityInstanceId id) {}

  public IncrementalSimulationDriver(MissionModel<?> missionModel){
    this.missionModel = missionModel;
    plannedDirectiveToTask = new HashMap<>();
    taskToPlannedDirective = new HashMap<>();
    initSimulation();
    simulateUntil(Duration.ZERO);
  }

  private void initSimulation(){
    plannedDirectiveToTask.clear();
    taskToPlannedDirective.clear();
    lastSimResults = null;
    lastSimResultsEnd = Duration.ZERO;
    this.engine = new SimulationEngine();
    activitiesInserted.clear();

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
      final var daemon = engine.initiateTaskFromSource(missionModel::getDaemon);
      final var commit = engine.performJobs(Set.of(SimulationEngine.JobId.forTask(daemon)),
                                            cells, curTime, Duration.MAX_VALUE, missionModel);
      timeline.add(commit);
    }
  }

  //
  private void simulateUntil(Duration endTime){
    assert(endTime.noShorterThan(curTime));
    while (true) {
      final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
      // Increment real time, if necessary.
      if(batch.offsetFromStart().longerThan(endTime) || endTime.isEqualTo(Duration.MAX_VALUE)){
        break;
      }
      curTime = batch.offsetFromStart();
      final var delta = batch.offsetFromStart().minus(curTime);
      timeline.add(delta);
      // Run the jobs in this batch.
      final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE, missionModel);
      timeline.add(commit);

    }
    lastSimResults = null;
  }


  public void simulateActivity(SerializedActivity activity, Duration startTime, ActivityInstanceId activityId)
  throws TaskSpecType.UnconstructableTaskSpecException
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
   * @return the simulation results
   */
  public SimulationResults getSimulationResults(){
    return getSimulationResultsUpTo(curTime);
  }

  /**
   * Get the simulation results from the Duration.ZERO to a specified end time point.
   * The provided simulation results might cover more than the required time period.
   * @return the simulation results
   */
  public SimulationResults getSimulationResultsUpTo(Duration endTime){
    //if previous results cover a bigger period, we return do not regenerate
    if(endTime.longerThan(curTime)){
      simulateUntil(endTime);
    }

    if(lastSimResults == null || endTime.longerThan(lastSimResultsEnd)) {
      final Map<String, ActivityInstanceId> convertedTaskToPlannedDir = new HashMap<>();
      taskToPlannedDirective.forEach((taskId, plannedDirective)->
                                         convertedTaskToPlannedDir.put(taskId.id(), plannedDirective));
      lastSimResults = engine.computeResults(
          engine,
          Instant.now(),
          endTime,
          convertedTaskToPlannedDir,
          timeline,
          missionModel);
      lastSimResultsEnd = endTime;
      //while sim results may not be up to date with curTime, a regeneration has taken place after the last insertion
    }
    return lastSimResults;
  }

  private void simulateSchedule(final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule)
  throws TaskSpecType.UnconstructableTaskSpecException
  {

    if(schedule.isEmpty()){
      throw new IllegalArgumentException("simulateSchedule() called with empty schedule, use simulateUntil() instead");
    }

    for (final var entry : schedule.entrySet()) {
      final var directiveId = entry.getKey();
      final var startOffset = entry.getValue().getLeft();
      final var directive = entry.getValue().getRight();

      final var taskId = engine.initiateTaskFromInputOrFail(missionModel, directive);
      engine.scheduleTask(taskId, startOffset);
      plannedDirectiveToTask.put(directiveId,taskId);
      taskToPlannedDirective.put(taskId, directiveId);
    }
    var allTaskFinished = false;
    while (true) {
      final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
      // Increment real time, if necessary.
      final var delta = batch.offsetFromStart().minus(curTime);
      //once all tasks are finished, we need to wait for events triggered at the same time
      if(allTaskFinished && !delta.isZero()){
        break;
      }
      curTime = batch.offsetFromStart();
      timeline.add(delta);
      // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
      //   even if they occur at the same real time.

      // Run the jobs in this batch.
      final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE, missionModel);
      timeline.add(commit);

      // all tasks are complete : do not exit yet, there might be event triggered at the same time
      if ((taskToPlannedDirective.size() > 0 && taskToPlannedDirective.keySet().stream().allMatch(taskId -> engine.isTaskComplete(taskId)))) {
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

}
