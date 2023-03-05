package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.Map;

public final class SimulationDriver {
  public static <Model>
  SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration
  ) {
    try (final var engine = new SimulationEngine(startTime, missionModel, null)) {
      /* The top-level simulation timeline. */
      var cells = new LiveCells(engine.timeline, missionModel.getInitialCells());
      /* The current real time. */
      var elapsedTime = Duration.ZERO;

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Specify a topic to track queries
      final var queryTopic = new Topic<Topic<?>>();

      // Start daemon task(s) immediately, before anything else happens.
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
      {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE, queryTopic);
        engine.timeline.add(commit, elapsedTime);
      }

      // Specify a topic on which tasks can log the activity they're associated with.
      //final var activityTopic = new Topic<ActivityInstanceId>();

      // Schedule all activities.
      for (final var entry : schedule.entrySet()) {
        final var directiveId = entry.getKey();
        final var startOffset = entry.getValue().getLeft();
        final var serializedDirective = entry.getValue().getRight();

        final TaskFactory<?> task;
        try {
          task = missionModel.getTaskFactory(serializedDirective);
        } catch (final InstantiationException ex) {
          // All activity instantiations are assumed to be validated by this point
          throw new Error("Unexpected state: activity instantiation %s failed with: %s"
              .formatted(serializedDirective.getTypeName(), ex.toString()));
        }

        engine.scheduleTask(startOffset, SimulationEngine.emitAndThen(directiveId, engine.defaultActivityTopic, task));
      }

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (true) {
        final var batch = engine.extractNextJobs(simulationDuration);

        // Increment real time, if necessary.
        final var delta = batch.offsetFromStart().minus(elapsedTime);
        elapsedTime = batch.offsetFromStart();
        // TODO: Since we moved timeline from SimulationDriver to SimulationEngine, maybe some of this should be encapsulated in the engine.
        engine.timeline.add(delta);
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        if (batch.jobs().isEmpty() && batch.offsetFromStart().isEqualTo(simulationDuration)) {
          break;
        }

        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, simulationDuration, queryTopic);
        engine.timeline.add(commit, elapsedTime);
      }

      // A query depends on an event if
      // - that event has the same topic as the query
      // - that event occurs causally before the query

      // Let A be an event or query issued by task X, and B be either an event or query issued by task Y
      // A flows to B if B is causally after A and
      // - X = Y
      // - X spawned Y causally after A
      // - Y called X, and emitted B after X terminated
      // - Transitively: if A flows to C and C flows to B, A flows to B
      // tstill not enough...?

      return engine.computeResults(startTime, elapsedTime, engine.defaultActivityTopic);
    }
  }

  public static <Model, Return>
  void simulateTask(final Instant startTime, final MissionModel<Model> missionModel, final TaskFactory<Return> task) {
    // TODO: Need to update this to be like IncrementalSimulationDriver
    try (final var engine = new SimulationEngine(startTime, missionModel, null)) {
      /* The top-level simulation timeline. */
      //var timeline = new TemporalEventSource();
      var cells = new LiveCells(engine.timeline, missionModel.getInitialCells());
      /* The current real time. */
      var elapsedTime = Duration.ZERO;

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Specify a topic to track queries
      final var queryTopic = new Topic<Topic<?>>();

      // Start daemon task(s) immediately, before anything else happens.
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
      {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE, queryTopic);
        engine.timeline.add(commit, elapsedTime);
      }

      // Schedule all activities.
      final var taskId = engine.scheduleTask(elapsedTime, task);

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (!engine.isTaskComplete(taskId)) {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);

        // Increment real time, if necessary.
        final var delta = batch.offsetFromStart().minus(elapsedTime);
        elapsedTime = batch.offsetFromStart();
        engine.timeline.add(delta);
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE, queryTopic);
        engine.timeline.add(commit, elapsedTime);
      }
    }
  }

}
