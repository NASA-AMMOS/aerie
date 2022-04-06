package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine.JobId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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

      // Schedule all activities.
      final var taskToPlannedDirective = new HashMap<String, ActivityInstanceId>();
      for (final var entry : schedule.entrySet()) {
        final var directiveId = entry.getKey();
        final var startOffset = entry.getValue().getLeft();
        final var directive = entry.getValue().getRight();

        final var taskId = engine.initiateTaskFromInput(missionModel, directive);
        engine.scheduleTask(taskId, startOffset);

        taskToPlannedDirective.put(taskId.id(), directiveId);
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

      return engine.computeResults(engine, startTime, elapsedTime, taskToPlannedDirective, timeline, missionModel);
    }
  }

  public static <Model, Return>
  void simulateTask(final MissionModel<Model> missionModel, final Task<Return> task) {
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

      // Schedule all activities.
      final var taskId = engine.initiateTask(elapsedTime, task);
      engine.scheduleTask(taskId, elapsedTime);

      // Start daemon task(s) immediately, before anything else happens.
      {
        final var daemon = engine.initiateTaskFromSource(missionModel::getDaemon);
        final var commit = engine.performJobs(Set.of(JobId.forTask(daemon)), cells, elapsedTime, Duration.MAX_VALUE, missionModel);
        timeline.add(commit);
      }

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (!engine.isTaskComplete(taskId)) {
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
}
