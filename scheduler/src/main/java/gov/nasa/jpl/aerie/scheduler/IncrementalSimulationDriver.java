package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

public class IncrementalSimulationDriver {

  private Duration curTime;
  private SimulationEngine engine;
  private LiveCells cells;
  private TemporalEventSource timeline;
  private MissionModel<?> missionModel;

  //mapping each activity name to its task id (in String form) in the simulation engine
  private Map<String, String> actToId;

  //boolean stating whether the simulation results should be regenerated
  private boolean areLastSimResultsDirty;

  //simulation results so far
  private SimulationResults lastSimResults;
  //cached simulation results cover the period [Duration.ZERO, lastSimResultsEnd]
  private Duration lastSimResultsEnd = Duration.ZERO;

  //map from activity start time to serialized activity and its name
  private List<SimulatedActivity> activitiesInserted = new ArrayList<>();

  record SimulatedActivity(Duration start, SerializedActivity activity, String name) implements Comparable<SimulatedActivity> {
    @Override
    public int compareTo(@NotNull final SimulatedActivity o) {
      return start.compareTo(o.start);
    }
  }

  public IncrementalSimulationDriver(MissionModel<?> missionModel){
    this.missionModel = missionModel;
    initSimulation();
  }

  private void initSimulation(){
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

  public void simulateActivity(SerializedActivity activity, Duration startTime, String nameAct){
    var testAct = new SimulatedActivity(startTime, activity, nameAct);
    if(startTime.noLongerThan(curTime)){
      var toBeInserted = new ArrayList<>(activitiesInserted);
      toBeInserted.add(testAct);
      initSimulation();
      simulateManyActs(toBeInserted);
    } else {
      simulateAct(testAct);
    }
  }


  /**
   * Get the simulation results from the Duration.ZERO to the current simulation time point
   * @return the simulation results
   */
  public SimulationResults getSimulationResults(){
    return getSimulationResultsUntil(curTime);
  }

  /**
   * Get the simulation results from the Duration.ZERO to a specified end time point.
   * The provided simulation results might cover more than the required time period.
   * @return the simulation results
   */  public SimulationResults getSimulationResultsUntil(Duration endTime){
    //if previous results cover a bigger period, we return do not regenerate
    if(areLastSimResultsDirty || lastSimResults == null || endTime.longerThan(lastSimResultsEnd)) {
      lastSimResults = engine.computeResults(
          engine,
          Instant.now(),
          endTime,
          new HashMap<>(),
          timeline,
          missionModel);
      lastSimResultsEnd = endTime;
      //while sim results may not be up to date with curTime, a regeneration has taken place after the last insertion
      areLastSimResultsDirty = false;
    }
    return lastSimResults;
  }

  private ControlTask buildControlTask(ArrayList<SimulatedActivity> acts){
    final var schedule = acts.stream().collect(Collectors.toMap( e -> e.name, e->Pair.of(e.start, e.activity)));
    return new ControlTask(schedule, curTime);
  }

  private ControlTask buildControlTask(SimulatedActivity simAct){
    final var schedule = Map.of(simAct.name, Pair.of(simAct.start, simAct.activity));
    return new ControlTask(schedule, curTime);
  }

  private void simulateControlTask(ControlTask controlTask){
    // Schedule the control task.
    final var control = engine.initiateTask(curTime, controlTask);
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
  }

  private void simulateManyActs(ArrayList<SimulatedActivity> acts){
    final var controlTask = buildControlTask(acts);
    simulateControlTask(controlTask);
    activitiesInserted.addAll(acts);
  }

  private void simulateAct(SimulatedActivity simAct){
    final var controlTask = buildControlTask(simAct);
    simulateControlTask(controlTask);
    activitiesInserted.add(simAct);
  }

  /**
   * Returns the duration of a terminated simulated activity
   * @param actName the activity name
   * @return its duration if the activity has been simulated and has finished simulating, an IllegalArgumentException otherwise
   */
  public Duration getTerminatedActivityDuration(String actName){
    return engine.getTaskDuration(actToId.get(actName));
  }

  private final class ControlTask implements Task {
    private final Map<String, Pair<Duration, SerializedActivity>> schedule;

    /* The directive that caused a task (if any). */
    // Non-final because we replace it with an empty map when extracted by a client.
    private Map<String, String> taskToPlannedDirective = new HashMap<>();

    private final PriorityQueue<Triple<Duration, String, SerializedActivity>> scheduledTasks
        = new PriorityQueue<>(Comparator.comparing(Triple::getLeft));

    private Duration currentTime;

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
        actToId.put(nextTask.getMiddle(), id);
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
