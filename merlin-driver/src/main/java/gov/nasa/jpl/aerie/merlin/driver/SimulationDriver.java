package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine.JobId;
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
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public final class SimulationDriver {
  public static <Model>
  SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration
  ) {
    try (final var engine = new SimulationEngine()) {
      /* The top-level simulation timeline. */
      var timeline = new TemporalEventSource();
      var cells = new LiveCells(timeline, missionModel.getInitialCells());
      /* The current real time. */
      var elapsedTime = Duration.ZERO;

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Schedule the control task.
      final var controlTask = new ControlTask(schedule);
      {
        final var control = engine.initiateTask(elapsedTime, controlTask);
        engine.scheduleTask(control, elapsedTime);
      }

      // Start daemon task(s) immediately, before anything else happens.
      {
        final var daemon = engine.initiateTaskFromSource(missionModel::getDaemon);
        final var commit = engine.performJobs(Set.of(JobId.forTask(daemon)), cells, elapsedTime, simulationDuration, missionModel);
        timeline.add(commit);
      }

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (true) {
        final var batch = engine.extractNextJobs(simulationDuration);

        // Increment real time, if necessary.
        final var delta = batch.offsetFromStart().minus(elapsedTime);
        elapsedTime = batch.offsetFromStart();
        timeline.add(delta);
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        if (batch.jobs().isEmpty() && batch.offsetFromStart().isEqualTo(simulationDuration)) {
          break;
        }

        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, simulationDuration, missionModel);
        timeline.add(commit);
      }

      return engine.computeResults(engine, startTime, elapsedTime, controlTask.extractTaskToPlannedDirective(), timeline, missionModel);
    }
  }

  public static <Model>
  void simulateTask(final MissionModel<Model> missionModel, final Task task) {
    try (final var engine = new SimulationEngine()) {
      /* The top-level simulation timeline. */
      var timeline = new TemporalEventSource();
      var cells = new LiveCells(timeline, missionModel.getInitialCells());
      /* The current real time. */
      var elapsedTime = Duration.ZERO;

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Schedule the control task.
      final var control = engine.initiateTask(elapsedTime, task);
      engine.scheduleTask(control, elapsedTime);

      // Start daemon task(s) immediately, before anything else happens.
      {
        final var daemon = engine.initiateTaskFromSource(missionModel::getDaemon);
        final var commit = engine.performJobs(Set.of(JobId.forTask(daemon)), cells, elapsedTime, Duration.MAX_VALUE, missionModel);
        timeline.add(commit);
      }

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (!engine.isTaskComplete(control)) {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);

        // Increment real time, if necessary.
        final var delta = batch.offsetFromStart().minus(elapsedTime);
        elapsedTime = batch.offsetFromStart();
        timeline.add(delta);
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE, missionModel);
        timeline.add(commit);
      }
    }
  }

  public static ControlTask buildPlanTask(Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule){
    return new ControlTask(schedule);
  }

  private static final class ControlTask implements Task {
    private final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule;

    /* The directive that caused a task (if any). */
    // Non-final because we replace it with an empty map when extracted by a client.
    private Map<String, ActivityInstanceId> taskToPlannedDirective = new HashMap<>();

    private final PriorityQueue<Triple<Duration, ActivityInstanceId, SerializedActivity>> scheduledTasks
        = new PriorityQueue<>(Comparator.comparing(Triple::getLeft));

    private Duration currentTime = Duration.ZERO;

    public ControlTask(final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule) {
      this.schedule = Objects.requireNonNull(schedule);
      this.reset();
    }

    public Map<String, ActivityInstanceId> extractTaskToPlannedDirective() {
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
