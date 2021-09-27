package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.newengine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.newengine.SimulationEngine.JobId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.timeline.SimulationTimeline;
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
  public static <$Schema> SimulationResults simulate(
      final Adaptation<$Schema, ?> adaptation,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration)
  {
    return simulate(adaptation, SimulationTimeline.create(adaptation.getSchema()), schedule, startTime, simulationDuration);
  }

  private static <$Timeline, Model>
  SimulationResults simulate(
      final Adaptation<? super $Timeline, Model> missionModel,
      final SimulationTimeline<$Timeline> events,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration
  ) {
    try (final var engine = new SimulationEngine<$Timeline>()) {
      /* The current causal time. */
      var now = events.origin();
      /* The current real time. */
      var elapsedTime = Duration.ZERO;
      /* The current set of jobs remaining to perform. */

      // Begin tracking all resources.
      for (final var family : missionModel.getResourceFamilies()) {
        trackResourceFamily(engine, elapsedTime, family);
      }

      // Schedule the control task.
      final var controlTask = new ControlTask<$Timeline>(schedule);
      {
        final var control = engine.initiateTask(elapsedTime, controlTask);
        engine.scheduleTask(control, elapsedTime);
      }

      // Start daemon task(s) immediately, before anything else happens.
      {
        final var daemon = engine.initiateTaskFromSource(missionModel::getDaemon);
        now = engine.performJobs(Set.of(JobId.forTask(daemon)), now, elapsedTime, simulationDuration, missionModel);
      }

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (true) {
        final var batch = engine.extractNextJobs(simulationDuration);

        // Increment real time, if necessary.
        if (batch.offsetFromStart().longerThan(elapsedTime)) {
          now = now.wait(batch.offsetFromStart().minus(elapsedTime));
          elapsedTime = batch.offsetFromStart();
        }
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        if (batch.jobs().isEmpty() && batch.offsetFromStart().isEqualTo(simulationDuration)) {
          break;
        }

        // Run the jobs in this batch.
        now = engine.performJobs(batch.jobs(), now, elapsedTime, simulationDuration, missionModel);
      }

      return engine.computeResults(engine, startTime, elapsedTime, controlTask.extractTaskToPlannedDirective());
    }
  }

  public static <$Schema, $Timeline extends $Schema, Model>
  void simulateTask(
      final Adaptation<? super $Timeline, Model> missionModel,
      final SimulationTimeline<$Timeline> events,
      final Task<$Timeline> task
  ) {
    try (final var engine = new SimulationEngine<$Timeline>()) {
      /* The current causal time. */
      var now = events.origin();
      /* The current real time. */
      var elapsedTime = Duration.ZERO;
      /* The current set of jobs remaining to perform. */

      // Begin tracking all resources.
      for (final var family : missionModel.getResourceFamilies()) {
        trackResourceFamily(engine, elapsedTime, family);
      }

      // Schedule the control task.
      final var control = engine.initiateTask(elapsedTime, task);
      engine.scheduleTask(control, elapsedTime);

      // Start daemon task(s) immediately, before anything else happens.
      {
        final var daemon = engine.initiateTaskFromSource(missionModel::getDaemon);
        now = engine.performJobs(Set.of(JobId.forTask(daemon)), now, elapsedTime, Duration.MAX_VALUE, missionModel);
      }

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (!engine.isTaskComplete(control)) {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);

        // Increment real time, if necessary.
        if (batch.offsetFromStart().longerThan(elapsedTime)) {
          now = now.wait(batch.offsetFromStart().minus(elapsedTime));
          elapsedTime = batch.offsetFromStart();
        }
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        // Run the jobs in this batch.
        now = engine.performJobs(batch.jobs(), now, elapsedTime, Duration.MAX_VALUE, missionModel);
      }
    }
  }

  private static <$Timeline, ResourceType>
  void trackResourceFamily(
      final SimulationEngine<$Timeline> engine,
      final Duration currentTime,
      final ResourceFamily<? super $Timeline, ResourceType> family
  ) {
    final ResourceSolver<? super $Timeline, ResourceType, ?> solver = family.getSolver();

    for (final var entry : family.getResources().entrySet()) {
      final var name = entry.getKey();
      final var getter = entry.getValue();

      engine.trackResource(name, solver, getter, currentTime);
    }
  }

  private static final class ControlTask<$Timeline> implements Task<$Timeline> {
    private final Map<String, Pair<Duration, SerializedActivity>> schedule;

    /* The directive that caused a task (if any). */
    // Non-final because we replace it with an empty map when extracted by a client.
    private Map<String, String> taskToPlannedDirective = new HashMap<>();

    private final PriorityQueue<Triple<Duration, String, SerializedActivity>> scheduledTasks
        = new PriorityQueue<>(Comparator.comparing(Triple::getLeft));

    private Duration currentTime = Duration.ZERO;

    public ControlTask(final Map<String, Pair<Duration, SerializedActivity>> schedule) {
      this.schedule = Objects.requireNonNull(schedule);
      this.reset();
    }

    public Map<String, String> extractTaskToPlannedDirective() {
      final var taskToPlannedDirective = this.taskToPlannedDirective;
      this.taskToPlannedDirective = new HashMap<>();
      return taskToPlannedDirective;
    }

    @Override
    public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
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
