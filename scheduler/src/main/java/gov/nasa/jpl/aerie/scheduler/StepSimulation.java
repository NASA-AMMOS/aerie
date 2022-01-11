package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public class StepSimulation {

  private Duration curTime;
  private SimulationEngine engine;
  private LiveCells cells;
  private TemporalEventSource timeline;
  private MissionModel<?> missionModel;

  //used for looking up durations
  private Map<SerializedActivity, TaskId> actToId;

  //boolean stating whether the simulation results should be regenerated
  private boolean areLastSimResultsDirty;

  //simulation results so far
  private SimulationResults lastSimResults;

  //should the simulation be resetted everytime an activity is simulated
  private boolean resetEveryTime;

  //map from activity start time to serialized activity and its name
  private LinkedHashMap<Duration, Pair<SerializedActivity, String>> activitiesInserted;

  /*when we really want to reset the whole simulation*/
  public void clearActInserted(){
    activitiesInserted.clear();
  }

  public StepSimulation(MissionModel<?> missionModel){
    this.missionModel = missionModel;
    resetEveryTime = false;
    activitiesInserted = new LinkedHashMap<>();
  }

  public void resetEveryTime(){
    resetEveryTime= true;
  }

  public void initSimulation(){
    actToId = new HashMap<>();
    lastSimResults = null;
    this.engine = new SimulationEngine();
    activitiesInserted.clear();

      /* The top-level simulation timeline. */
      this.timeline = new TemporalEventSource();
      areLastSimResultsDirty = false;
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

  public void simulateActivity(SerializedActivity activity, Duration startTime, String name){
    if(startTime.shorterThan(curTime) || resetEveryTime){
      var toBeReinserted = new LinkedHashMap<Duration, Pair<SerializedActivity, String>>();
      toBeReinserted.putAll(activitiesInserted);
      initSimulation();
      insertAllActsBefore(toBeReinserted, startTime);
      simulateAct(activity, startTime,name);
      insertAllActsAfter(toBeReinserted, startTime);
    }
    simulateAct(activity, startTime,name);
  }

  private void insertAllActsBefore(LinkedHashMap<Duration, Pair<SerializedActivity, String>> acts, Duration startTime){
    for(var act : acts.entrySet()){
      var start = act.getKey();
      var activity = act.getValue().getLeft();
      var name = act.getValue().getRight();
      if(start.shorterThan(startTime)) {
        this.simulateAct(activity, startTime, name);
      }
    }
  }

  private void insertAllActsAfter(LinkedHashMap<Duration, Pair<SerializedActivity, String>> acts, Duration startTime){
    for(var act : acts.entrySet()){
      var start = act.getKey();
      var activity = act.getValue().getLeft();
      var name = act.getValue().getRight();
      if(start.longerThan(startTime) || start.isEqualTo(startTime)) {
        this.simulateAct(activity, startTime, name);
      }
    }
  }

  //WARNING computes the results for the whole horizon until current time them from beginning
  public SimulationResults getSimulationResults(){
    if(areLastSimResultsDirty || lastSimResults == null) {
      lastSimResults = engine.computeResults(
          engine,
          Instant.now(),
          curTime,
          new HashMap<String, ActivityInstanceId>(),
          timeline,
          missionModel);
    }
    return lastSimResults;
  }

  //WARNING computes the results for the whole horizon until current time them from beginning
  public SimulationResults getSimulationResultsUntil(Duration endTime){
    if(areLastSimResultsDirty || lastSimResults == null) {
      lastSimResults = engine.computeResults(
          engine,
          Instant.now(),
          endTime,
          new HashMap<String, ActivityInstanceId>(),
          timeline,
          missionModel);
    }
    return lastSimResults;
  }


  private void simulateAct(SerializedActivity activity, Duration startTime, String name){

    final var schedule = Map.of(name, Pair.of(startTime, activity));

    final var controlTask = new ControlTask(schedule, curTime);
    // Schedule the control task.
    final var control = engine.initiateTask(curTime, controlTask);
    actToId.put(activity, control);

    engine.scheduleTask(control, curTime);

    while (!engine.isTaskComplete(control)) {
      final var batch = engine.extractNextJobs(Duration.MAX_VALUE);

      // Increment real time, if necessary.
      final var delta = batch.offsetFromStart().minus(curTime);
      curTime = batch.offsetFromStart();
      timeline.add(delta);
      // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
      //   even if they occur at the same real time.

      // Run the jobs in this batch.
      final var commit = engine.performJobs(batch.jobs(), cells, curTime, Duration.MAX_VALUE, missionModel);
      timeline.add(commit);
    }
    areLastSimResultsDirty = true;
    activitiesInserted.put(startTime, Pair.of(activity, name));
  }

  public Duration getActivityDuration(SerializedActivity act){
    //peek into engine
    return engine.getTaskDuration(actToId.get(act));
  }

  public boolean activityHasFinished(SerializedActivity act){
    return engine.isTaskComplete(actToId.get(act));
  }

  private final class ControlTask implements Task {
    private final Map<String, Pair<Duration, SerializedActivity>> schedule;

    /* The directive that caused a task (if any). */
    // Non-final because we replace it with an empty map when extracted by a client.
    private Map<String, String> taskToPlannedDirective = new HashMap<>();

    private final PriorityQueue<Triple<Duration, String, SerializedActivity>> scheduledTasks
        = new PriorityQueue<>(Comparator.comparing(Triple::getLeft));

    private Duration currentTime = Duration.ZERO;

    public ControlTask(final Map<String, Pair<Duration, SerializedActivity>> schedule, Duration curTime) {
      this.schedule = Objects.requireNonNull(schedule);
      this.currentTime = curTime;
      this.reset();
    }

    public Map<String, String> extractTaskToPlannedDirective() {
      final var taskToPlannedDirective = this.taskToPlannedDirective;
      this.taskToPlannedDirective = new HashMap<>();
      return taskToPlannedDirective;
    }

    @Override
    public TaskStatus step(final Scheduler scheduler) {
      while (true) {
        var nextTask = this.scheduledTasks.peek();
        if (nextTask == null) break;

        final var startTime = nextTask.getLeft();
        if (startTime.longerThan(this.currentTime)) {
          final var delta = nextTask.getLeft().minus(this.currentTime);
          this.currentTime = nextTask.getLeft();
          return TaskStatus.delayed(delta);
        }

        this.scheduledTasks.remove();

        final var directiveId = nextTask.getMiddle();
        final var specification = nextTask.getRight();

        final var id = scheduler.spawn(specification.getTypeName(), specification.getParameters());
        this.taskToPlannedDirective.put(id, directiveId);
      }

      return TaskStatus.completed();
    }

    @Override
    public void reset() {
      this.scheduledTasks.clear();
      for (final var entry : this.schedule.entrySet()) {
        this.scheduledTasks.add(Triple.of(
            entry.getValue().getLeft(),
            entry.getKey(),
            entry.getValue().getRight()));
      }
    }
  }
}
