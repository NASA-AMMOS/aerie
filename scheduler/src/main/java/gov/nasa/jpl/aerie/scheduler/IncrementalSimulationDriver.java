package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IncrementalSimulationDriver {
  private record SimulatedActivity(Duration start, SerializedActivity activity, String name) {}

  private final MissionModel<?> missionModel;

  // The collection of activities scheduled so far.
  private final List<SimulatedActivity> activitiesInserted = new ArrayList<>();

  // The top-level simulation timeline.
  private TemporalEventSource timeline;
  private LiveCells cells;
  private SimulationEngine engine;
  // The current real time.
  private Duration curTime;

  // The task associated with each scheduled activity.
  private final Map<String, TaskId> activityToTask = new HashMap<>();
  // The scheduled activity that caused a task (if any).
  private final Map<TaskId, String> taskToActivity = new HashMap<>();

  // Cached simulation results covering the period [Duration.ZERO, lastSimResultsEnd],
  //   or `null` if no results are cached.
  private SimulationResults lastSimResults;
  private Duration lastSimResultsEnd;

  public IncrementalSimulationDriver(final MissionModel<?> missionModel){
    this.missionModel = missionModel;
    resetSimulation();
  }

  private void resetSimulation() {
    this.timeline = new TemporalEventSource();
    this.cells = new LiveCells(this.timeline, this.missionModel.getInitialCells());
    this.engine = new SimulationEngine();
    this.curTime = Duration.ZERO;

    this.activityToTask.clear();
    this.taskToActivity.clear();

    this.lastSimResults = null;
    this.lastSimResultsEnd = Duration.ZERO;

    // Begin tracking all resources.
    for (final var entry : this.missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      this.engine.trackResource(name, resource, this.curTime);
    }

    // Start daemon task(s) immediately, before anything else happens.
    {
      final var daemon = this.engine.initiateTaskFromSource(this.missionModel::getDaemon);
      final var commit = this.engine.performJobs(
          Set.of(SimulationEngine.JobId.forTask(daemon)),
          this.cells,
          this.curTime,
          Duration.MAX_VALUE,
          this.missionModel);

      this.timeline.add(commit);
    }
  }

  public void simulateActivity(SerializedActivity activity, Duration startTime, String nameAct){
    // Add this activity to the schedule.
    final var scheduledActivity = new SimulatedActivity(startTime, activity, nameAct);
    this.activitiesInserted.add(scheduledActivity);

    // Is this an incremental update?
    final var isIncremental = scheduledActivity.start().longerThan(this.curTime);

    // If not, start the simulation over from the beginning.
    if (!isIncremental) resetSimulation();

    // Spawn the activities that need to be simulated.
    // If this is an incremental update, we only need to simulate the new activity.
    // Otherwise, we have to simulate all the scheduled activities.
    final var remainingTasks = (isIncremental)
        ? spawnActivityTasks(List.of(scheduledActivity))
        : spawnActivityTasks(this.activitiesInserted);

    // Simulate forward until all scheduled activities have terminated.
    for (final var task : remainingTasks) {
      while (!this.engine.isTaskComplete(task)) {
        final var batch = this.engine.extractNextJobs(Duration.MAX_VALUE);

        // Increment real time.
        final var delta = batch.offsetFromStart().minus(this.curTime);
        this.curTime = batch.offsetFromStart();
        this.timeline.add(delta);

        // Run the jobs in this batch.
        final var commit = this.engine.performJobs(batch.jobs(), this.cells, this.curTime, Duration.MAX_VALUE, this.missionModel);
        this.timeline.add(commit);
      }
    }
  }

  private List<TaskId> spawnActivityTasks(final List<SimulatedActivity> schedule){
    final var taskIds = new ArrayList<TaskId>(schedule.size());

    for (final var scheduledActivity : schedule) {
      final var taskId = this.engine.initiateTaskFromInput(this.missionModel, scheduledActivity.activity());
      this.activityToTask.put(scheduledActivity.name(), taskId);
      this.taskToActivity.put(taskId, scheduledActivity.name());

      this.engine.scheduleTask(taskId, scheduledActivity.start());
      taskIds.add(taskId);
    }

    return taskIds;
  }

  /**
   * Returns the duration of a terminated simulated activity
   * @param actName the activity name
   * @return its duration if the activity has been simulated and has finished simulating, an IllegalArgumentException otherwise
   */
  public Duration getTerminatedActivityDuration(final String actName) {
    return this.engine.getTaskDuration(this.activityToTask.get(actName));
  }

  /**
   * Get the simulation results from the Duration.ZERO to the current simulation time point
   * @return the simulation results
   */
  public SimulationResults getSimulationResults() {
    return getSimulationResultsUntil(this.curTime);
  }

  /**
   * Get the simulation results from the Duration.ZERO to a specified end time point.
   * The provided simulation results might cover more than the required time period.
   * @return the simulation results
   */
  public SimulationResults getSimulationResultsUntil(final Duration endTime) {
    //if previous results cover a bigger period, we return do not regenerate
    if (this.lastSimResults == null || endTime.longerThan(this.lastSimResultsEnd)) {
      this.lastSimResults = this.engine.computeResults(
          this.engine,
          Instant.now(),
          endTime,
          new HashMap<>(),  /* TODO: Provide the actual mapping between activities and tasks. */
          this.timeline,
          this.missionModel);
      this.lastSimResultsEnd = endTime;
    }

    return this.lastSimResults;
  }
}
