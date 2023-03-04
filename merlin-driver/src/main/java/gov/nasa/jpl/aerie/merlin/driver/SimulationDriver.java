package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SimulationDriver {
  public static <Model>
  SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant startTime,
      final Duration planDuration,
      final Duration simulationDuration
  ) {
    final var USE_RESOURCE_TRACKER = true;

    /* The top-level simulation timeline. */
    final var timeline = new TemporalEventSource();
    try (final var engine = new SimulationEngine(timeline, missionModel.getInitialCells())) {
      if (!USE_RESOURCE_TRACKER) {
        // Begin tracking all resources.
        for (final var entry : missionModel.getResources().entrySet()) {
          final var name = entry.getKey();
          final var resource = entry.getValue();

          engine.trackResource(name, resource, Duration.ZERO);
        }
      }

      // Start daemon task(s) immediately, before anything else happens.
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
      engine.step();

      // Specify a topic on which tasks can log the activity they're associated with.
      final var activityTopic = new Topic<ActivityDirectiveId>();

      // Get all activities as close as possible to absolute time
      // Schedule all activities.
      // Using HashMap explicitly because it allows `null` as a key.
      // `null` key means that an activity is not waiting on another activity to finish to know its start time
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(planDuration, schedule).compute();

      scheduleActivities(
          schedule,
          resolved,
          missionModel,
          engine,
          activityTopic
      );

      // The sole purpose of this task is to make sure the simulation has "stuff to do" until the simulationDuration.
      engine.scheduleTask(Duration.ZERO, executor -> $ -> TaskStatus.completed(Unit.UNIT));

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (engine.hasJobsScheduledThrough(simulationDuration)) {
        engine.step();
      }

      if (USE_RESOURCE_TRACKER) {
        // Replay the timeline to collect resource profiles
        final var resourceTracker = new ResourceTracker(timeline, missionModel.getInitialCells());
        for (final var entry : missionModel.getResources().entrySet()) {
          final var name = entry.getKey();
          final var resource = entry.getValue();
          resourceTracker.track(name, resource);
        }
        while (!resourceTracker.isEmpty()) {
          resourceTracker.updateResources();
        }

        return SimulationEngine.computeResults(
            engine,
            startTime,
            simulationDuration,
            activityTopic,
            timeline,
            missionModel.getTopics(),
            resourceTracker.resourceProfiles());
      } else {
        return SimulationEngine.computeResults(
            engine,
            startTime,
            simulationDuration,
            activityTopic,
            timeline,
            missionModel.getTopics());
      }
    }
  }

  public static <Model, Return>
  void simulateTask(final MissionModel<Model> missionModel, final TaskFactory<Return> task) {
    /* The top-level simulation timeline. */
    final var timeline = new TemporalEventSource();
    try (final var engine = new SimulationEngine(timeline, missionModel.getInitialCells())) {
      // Start daemon task(s) immediately, before anything else happens.
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
      engine.step();

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      final var taskId = engine.scheduleTask(Duration.ZERO, task);
      while (!engine.isTaskComplete(taskId)) {
        engine.step();
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

      engine.scheduleTask(startOffset, makeTaskFactory(
          directiveId,
          task,
          schedule,
          resolved,
          missionModel,
          activityTopic
      ));
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
}
