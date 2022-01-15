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

public final class IncrementalSimulationDriver {
  private final MissionModel<?> missionModel;
  private CachedSimulation simulation;

  // The collection of activities scheduled so far.
  private final List<SimulatedActivity> schedule = new ArrayList<>();

  public IncrementalSimulationDriver(final MissionModel<?> missionModel){
    this.missionModel = missionModel;
    this.simulation = new CachedSimulation(missionModel);
  }

  public void simulateActivity(final SerializedActivity activity, final Duration startOffset, final String name) {
    // Add this activity to the schedule.
    this.schedule.add(new SimulatedActivity(startOffset, activity, name));

    // Is this an incremental update?
    final var isIncremental = startOffset.longerThan(this.simulation.getCurrentTime());

    // If not, start the simulation over from the beginning.
    if (!isIncremental) this.simulation = new CachedSimulation(this.missionModel);

    // Spawn all remaining activities, then simulate forward until they've all terminated.
    final var remaining = (isIncremental)
        ? this.schedule.subList(this.schedule.size() - 1, this.schedule.size())
        : this.schedule;

    for (final var entry : remaining) this.simulation.spawn(entry.name(), entry.start(), entry.activity());
    for (final var entry : remaining) this.simulation.simulateUntilTerminated(entry.name());
  }

  /**
   * Get the duration of an activity that has terminated.
   *
   * @param name The name of the activity to query.
   * @return The duration of the queried activity.
   * @throws IllegalArgumentException if the activity has not terminated
   */
  public Duration getTerminatedActivityDuration(final String name) {
    return this.simulation.getTerminatedActivityDuration(name);
  }

  /** Get simulation results up to the current simulation time point. */
  public SimulationResults getSimulationResults() {
    return this.simulation.getSimulationResultsUntil(this.simulation.getCurrentTime());
  }

  /** Get simulation results up to at least the provided end time. */
  public SimulationResults getSimulationResultsUntil(final Duration endTime) {
    return this.simulation.getSimulationResultsUntil(endTime);
  }

  private record SimulatedActivity(Duration start, SerializedActivity activity, String name) {}

  private static final class CachedSimulation {
    private final MissionModel<?> missionModel;

    // The top-level simulation timeline.
    private final TemporalEventSource timeline = new TemporalEventSource();
    private final LiveCells cells;
    private final SimulationEngine engine = new SimulationEngine();
    // The current real time.
    private Duration currentTime = Duration.ZERO;

    // The task associated with each scheduled activity.
    private final Map<String, TaskId> activityToTask = new HashMap<>();
    // The scheduled activity that caused a task (if any).
    private final Map<TaskId, String> taskToActivity = new HashMap<>();

    // Cached simulation results covering the period [Duration.ZERO, lastSimResultsEnd],
    //   or `null` if no results are cached.
    private SimulationResults lastSimResults = null;
    private Duration lastSimResultsEnd = Duration.ZERO;

    public CachedSimulation(final MissionModel<?> missionModel) {
      this.missionModel = missionModel;
      this.cells = new LiveCells(this.timeline, missionModel.getInitialCells());

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();
        this.engine.trackResource(name, resource, this.currentTime);
      }

      // Start daemon task(s) immediately, before anything else happens.
      {
        final var daemon = this.engine.initiateTaskFromSource(missionModel::getDaemon);
        final var commit = this.engine.performJobs(
            Set.of(SimulationEngine.JobId.forTask(daemon)),
            this.cells,
            this.currentTime,
            Duration.MAX_VALUE,
            missionModel);

        this.timeline.add(commit);
      }
    }

    public void spawn(final String name, final Duration startOffset, final SerializedActivity activity) {
      final var taskId = this.engine.initiateTaskFromInput(this.missionModel, activity);
      this.activityToTask.put(name, taskId);
      this.taskToActivity.put(taskId, name);

      this.engine.scheduleTask(taskId, startOffset);
    }

    public void simulateUntilTerminated(final String name) {
      final var task = this.activityToTask.get(name);

      while (!this.engine.isTaskComplete(task)) {
        final var batch = this.engine.extractNextJobs(Duration.MAX_VALUE);

        // Increment real time.
        final var delta = batch.offsetFromStart().minus(this.currentTime);
        this.currentTime = batch.offsetFromStart();
        this.timeline.add(delta);

        // Run the jobs in this batch.
        final var commit = this.engine.performJobs(
            batch.jobs(),
            this.cells,
            this.currentTime,
            Duration.MAX_VALUE,
            this.missionModel);
        this.timeline.add(commit);
      }
    }

    public Duration getCurrentTime() {
      return this.currentTime;
    }

    public SimulationResults getSimulationResultsUntil(final Duration endTime) {
      if (this.lastSimResults == null || endTime.longerThan(this.lastSimResultsEnd)) {
        this.lastSimResults = this.engine.computeResults(
            this.engine,
            Instant.now(),  /* TODO: Provide a meaningful start time. */
            endTime,
            Map.of(),  /* TODO: Provide the actual mapping between activities and tasks. */
            this.timeline,
            this.missionModel);
        this.lastSimResultsEnd = endTime;
      }

      return this.lastSimResults;
    }

    public Duration getTerminatedActivityDuration(final String name) {
      return this.engine.getTaskDuration(this.activityToTask.get(name));
    }
  }
}
