package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SimulationDriver {
  public static <Model>
  SimulationResultsInterface simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration
  ) {
    try (final var engine = new SimulationEngine(planStartTime, missionModel, null)) {
      /* The top-level simulation timeline. */
      //var cells = new LiveCells(engine.timeline, missionModel.getInitialCells());
      /* The current real time. */
      engine.setCurTime(Duration.ZERO);
      var elapsedTime = engine.curTime();

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Specify a topic to track queries
      final var queryTopic = new Topic<Topic<?>>();

      try {
        // Start daemon task(s) immediately, before anything else happens.
        engine.scheduleTask(Duration.ZERO, missionModel.getDaemon(), null);
        {
          final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
          final var commit = engine.performJobs(batch.jobs(), elapsedTime, Duration.MAX_VALUE, queryTopic);
          engine.timeline.add(commit, elapsedTime);
        }

        // Specify a topic on which tasks can log the activity they're associated with.
        //final var activityTopic = new Topic<ActivityDirectiveId>();

        // Get all activities as close as possible to absolute time
        // Schedule all activities.
        // Using HashMap explicitly because it allows `null` as a key.
        // `null` key means that an activity is not waiting on another activity to finish to know its start time
        HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(planDuration, schedule).compute();
        if(resolved.size() != 0) {
          resolved.put(
              null,
              StartOffsetReducer.adjustStartOffset(
                  resolved.get(null),
                  Duration.of(
                      planStartTime.until(simulationStartTime, ChronoUnit.MICROS),
                      Duration.MICROSECONDS)));
        }
        // Filter out activities that are before simulationStartTime
        resolved = StartOffsetReducer.filterOutNegativeStartOffset(resolved);

        scheduleActivities(
            schedule,
            resolved,
            missionModel,
            engine,
            engine.defaultActivityTopic
        );

        // Drive the engine until we're out of time.
        // TERMINATION: Actually, we might never break if real time never progresses forward.
        while (true) {
          final var batch = engine.extractNextJobs(simulationDuration);

          // Increment real time, if necessary.
          final var delta = batch.offsetFromStart().minus(elapsedTime);
          engine.setCurTime(batch.offsetFromStart());
          elapsedTime = engine.curTime();
          // TODO: Since we moved timeline from SimulationDriver to SimulationEngine, maybe some of this should be encapsulated in the engine.
          engine.timeline.add(delta);
          // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
          //   even if they occur at the same real time.

          if (batch.jobs().isEmpty() && batch.offsetFromStart().isEqualTo(simulationDuration)) {
            break;
          }

          // Run the jobs in this batch.
          final var commit = engine.performJobs(batch.jobs(), elapsedTime, simulationDuration, queryTopic);
          engine.timeline.add(commit, elapsedTime);
        }
      } catch (Throwable ex) {
        throw new SimulationException(elapsedTime, simulationStartTime, ex);
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

      return engine.computeResults(simulationStartTime, elapsedTime, SimulationEngine.defaultActivityTopic);
    }
  }

  public static <Model, Return>
  void simulateTask(final Instant startTime, final MissionModel<Model> missionModel, final TaskFactory<Return> task) {
    // TODO: Need to update this to be like IncrementalSimulationDriver
    try (final var engine = new SimulationEngine(startTime, missionModel, null)) {
      /* The top-level simulation timeline. */
      //var timeline = new TemporalEventSource();
      //var cells = new LiveCells(engine.timeline, missionModel.getInitialCells());
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
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon(), null);
      {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
        final var commit = engine.performJobs(batch.jobs(), elapsedTime, Duration.MAX_VALUE, queryTopic);
        engine.timeline.add(commit, elapsedTime);
      }

      // Schedule all activities.
      final var taskId = engine.scheduleTask(elapsedTime, task, null);

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (!engine.isTaskComplete(taskId)) {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);

        // Increment real time, if necessary.
        final var delta = batch.offsetFromStart().minus(elapsedTime);
        elapsedTime = batch.offsetFromStart();
        engine.setCurTime(elapsedTime);
        engine.timeline.add(delta);
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), elapsedTime, Duration.MAX_VALUE, queryTopic);
        engine.timeline.add(commit, elapsedTime);
      }
    }
  }


  private static <Model> void scheduleActivities(
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final SimulationEngine engine,
      final Topic<ActivityDirectiveId> activityTopic
  )
  {
    if(resolved.get(null) == null) { return; } // Nothing to simulate

    for (final Pair<ActivityDirectiveId, Duration> directivePair : resolved.get(null)) {
      final var directiveId = directivePair.getLeft();
      final var startOffset = directivePair.getRight();
      final var serializedDirective = schedule.get(directiveId).serializedActivity();

      final TaskFactory<?> task;
      try {
        task = missionModel.getTaskFactory(serializedDirective);
      } catch (final InstantiationException ex) {
        // All activity instantiations are assumed to be validated by this point
        throw new Error("Unexpected state: activity instantiation %s failed with: %s"
                            .formatted(serializedDirective.getTypeName(), ex.toString()));
      }

      engine.scheduleTask(startOffset,
                          makeTaskFactory(directiveId,
                                          task,
                                          schedule,
                                          resolved,
                                          missionModel,
                                          activityTopic),
                          null);
    }
  }

  private static <Model, Output> TaskFactory<Unit> makeTaskFactory(
      final ActivityDirectiveId directiveId,
      final TaskFactory<Output> task,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final Topic<ActivityDirectiveId> activityTopic
  )
  {
    // Emit the current activity (defined by directiveId)
    return executor -> scheduler0 -> TaskStatus.calling((TaskFactory<Output>) (executor1 -> scheduler1 -> {
      scheduler1.emit(directiveId, activityTopic);
      return task.create(executor1).step(scheduler1);
    }), scheduler2 -> {
      // When the current activity finishes, get the list of the activities that needed this activity to finish to know their start time
      final List<Pair<ActivityDirectiveId, Duration>> dependents = resolved.get(directiveId) == null ? List.of() : resolved.get(directiveId);
      // Iterate over the dependents
      for (final var dependent : dependents) {
        scheduler2.spawn(executor2 -> scheduler3 ->
            // Delay until the dependent starts
            TaskStatus.delayed(dependent.getRight(), scheduler4 -> {
              final var dependentDirectiveId = dependent.getLeft();
              final var serializedDependentDirective = schedule.get(dependentDirectiveId).serializedActivity();

              // Initialize the Task for the dependent
              final TaskFactory<?> dependantTask;
              try {
                dependantTask = missionModel.getTaskFactory(serializedDependentDirective);
              } catch (final InstantiationException ex) {
                // All activity instantiations are assumed to be validated by this point
                throw new Error("Unexpected state: activity instantiation %s failed with: %s"
                                    .formatted(serializedDependentDirective.getTypeName(), ex.toString()));
              }

              // Schedule the dependent
              // When it finishes, it will schedule the activities depending on it to know their start time
              scheduler4.spawn(makeTaskFactory(
                  dependentDirectiveId,
                  dependantTask,
                  schedule,
                  resolved,
                  missionModel,
                  activityTopic
              ));
              return TaskStatus.completed(Unit.UNIT);
            }));
      }
      return TaskStatus.completed(Unit.UNIT);
    });
  }
//  public Duration curTime() {
//    if (engine == null) {
//      return Duration.ZERO;
//    }
//    return engine.curTime();
//  }
//
//  public void setCurTime(Duration time) {
//    this.engine.setCurTime(time);
//  }

}
