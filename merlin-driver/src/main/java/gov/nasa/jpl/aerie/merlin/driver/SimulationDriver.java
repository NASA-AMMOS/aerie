package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SlabList;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.min;

public final class SimulationDriver {
  public static List<CachedSimulationEngine> cachedEngines = new ArrayList<>(); // TODO cache relevant prefix of plan, sim config, model id
  // TODO correctly handle relatively scheduled activities

  public static <Model>
  SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration
  )
  {
    return simulate(missionModel, schedule, simulationStartTime, simulationDuration, planStartTime, planDuration, $ -> {});
  }

  public record CachedSimulationEngine(
      Duration startOffset,
      List<ActivityDirective> activityDirectives,
      SimulationEngine simulationEngine,
      LiveCells cells,
      SlabList<TemporalEventSource.TimePoint> timePoints,
      Topic<ActivityDirectiveId> activityTopic
  ) {
    public CachedSimulationEngine {
      cells.freeze();
      timePoints.freeze();
      simulationEngine.close();
    }

    public static CachedSimulationEngine empty() {
      return new CachedSimulationEngine(
          Duration.ZERO,
          List.of(),
          new SimulationEngine(),
          new LiveCells(new TemporalEventSource()),
          new SlabList<>(),
          new Topic<>()
      );
    }
  }

  public static <Model>
  SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final Consumer<Duration> simulationExtentConsumer
  ) {
    try (final var engine = new SimulationEngine()) {
      /* The top-level simulation timeline. */
      var timeline = new TemporalEventSource();
      var cells = new LiveCells(timeline, missionModel.getInitialCells());
      /* The current real time. */
      var elapsedTime = Duration.ZERO;

      simulationExtentConsumer.accept(elapsedTime);

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Specify a topic on which tasks can log the activity they're associated with.
      final var activityTopic = new Topic<ActivityDirectiveId>();

      try {
        // Start daemon task(s) immediately, before anything else happens.
        engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
        {
          final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
          final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
          timeline.add(commit);
        }

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
            activityTopic
        );

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

          simulationExtentConsumer.accept(elapsedTime);

          if (batch.jobs().isEmpty() && batch.offsetFromStart().isEqualTo(simulationDuration)) {
            break;
          }

          // Run the jobs in this batch.
          final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, simulationDuration);
          timeline.add(commit);
        }
      } catch (Throwable ex) {
        throw new SimulationException(elapsedTime, simulationStartTime, ex);
      }

      final var topics = missionModel.getTopics();
      return SimulationEngine.computeResults(engine, simulationStartTime, elapsedTime, activityTopic, timeline, topics);
    }
  }

  public static Optional<CachedSimulationEngine> bestCachedEngine(final Map<ActivityDirectiveId, ActivityDirective> schedule, final List<CachedSimulationEngine> cachedEngines) {
    Optional<CachedSimulationEngine> bestCandidate = Optional.empty();
    for (final var cachedEngine : cachedEngines) {
      if (bestCandidate.isPresent() && cachedEngine.startOffset().noLongerThan(bestCandidate.get().startOffset())) continue;

      final List<ActivityDirective> activityDirectives = new ArrayList<>(cachedEngine.activityDirectives());
      // Find the invalidation time
      var invalidationTime = Duration.MAX_VALUE;
      final var scheduledActivities = new ArrayList<>(schedule.values());
      for (final var activity : scheduledActivities) {
        if (activityDirectives.contains(activity)) {
          activityDirectives.remove(activity);
        } else {
          invalidationTime = min(invalidationTime, activity.startOffset());
        }
      }
      for (final var activity : activityDirectives) {
        invalidationTime = min(invalidationTime, activity.startOffset());
      }
      if (cachedEngine.startOffset().shorterThan(invalidationTime)) {
        bestCandidate = Optional.of(cachedEngine);
      }
    }

    bestCandidate.ifPresent(cachedSimulationEngine -> System.out.println("Re-using simulation engine at " + cachedSimulationEngine.startOffset()));

    return bestCandidate;
  }

  private static TemporalEventSource makeCombinedTimeline(List<TemporalEventSource> timelines, TemporalEventSource timeline) {
    final TemporalEventSource combinedTimeline = new TemporalEventSource();
    for (final var entry : timelines) {
      for (final var timePoint : entry.points()) {
        if (timePoint instanceof TemporalEventSource.TimePoint.Delta t) {
          combinedTimeline.add(t.delta());
        } else if (timePoint instanceof TemporalEventSource.TimePoint.Commit t) {
          combinedTimeline.add(t.events());
        }
      }
    }

    for (final var timePoint : timeline) {
      if (timePoint instanceof TemporalEventSource.TimePoint.Delta t) {
        combinedTimeline.add(t.delta());
      } else if (timePoint instanceof TemporalEventSource.TimePoint.Commit t) {
        combinedTimeline.add(t.events());
      }
    }
    return combinedTimeline;
  }

  public static BiFunction<Duration, Duration, Boolean> desiredCheckpoints(final List<Duration> desiredCheckpoints) {
    return (elapsedTime, nextTime) -> {
      for (final var desiredCheckpoint : desiredCheckpoints) {
        if (elapsedTime.noLongerThan(desiredCheckpoint) && nextTime.longerThan(desiredCheckpoint)) {
          return true;
        }
      }
      return false;
    };
  }

  public static BiFunction<Duration, Duration, Boolean> wallClockCheckpoints(final long thresholdSeconds) {
    MutableLong lastCheckpointRealTime = new MutableLong(System.nanoTime());
    MutableObject<Duration> lastCheckpointSimTime = new MutableObject<>(Duration.ZERO);
    return (elapsedTime, nextTime) -> {
      if (nextTime.longerThan(elapsedTime) && System.nanoTime() - lastCheckpointRealTime.getValue() > (thresholdSeconds * 1000 * 1000 * 1000)) {
        lastCheckpointRealTime.setValue(System.nanoTime());
        lastCheckpointSimTime.setValue(elapsedTime);
        return true;
      } else {
        return false;
      }
    };
  }

  public record SimulationResultsWithCheckpoints(SimulationResults results, List<CachedSimulationEngine> checkpoints) {}
  public static <Model> SimulationResultsWithCheckpoints simulateWithCheckpoints(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final Consumer<Duration> simulationExtentConsumer,
      final CachedSimulationEngine cachedEngine,
      final BiFunction<Duration, Duration, Boolean> shouldTakeCheckpoint
  ) {
    final var activityTopic = cachedEngine.activityTopic();
    final var timelines = new ArrayList<TemporalEventSource>();
    timelines.add(new TemporalEventSource(cachedEngine.timePoints));
    final var checkpoints = new ArrayList<CachedSimulationEngine>();
    var engine = cachedEngine.simulationEngine.duplicate();
    engine.unscheduleAfter(cachedEngine.startOffset);
    try (var ignored = cachedEngine.simulationEngine) {
      var timeline = new TemporalEventSource();
      var cells = new LiveCells(timeline, cachedEngine.cells());
      /* The current real time. */
      var elapsedTime = cachedEngine.startOffset();

      simulationExtentConsumer.accept(elapsedTime);

      // Specify a topic on which tasks can log the activity they're associated with.

      try {
        final var filteredSchedule = new HashMap<ActivityDirectiveId, ActivityDirective>();
        for (final var entry : schedule.entrySet()) {
          if (entry.getValue().startOffset().longerThan(cachedEngine.startOffset())) {
            filteredSchedule.put(entry.getKey(), entry.getValue());
          }
        }

        // Get all activities as close as possible to absolute time
        // Schedule all activities.
        // Using HashMap explicitly because it allows `null` as a key.
        // `null` key means that an activity is not waiting on another activity to finish to know its start time
        HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(planDuration, filteredSchedule).compute();
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
            filteredSchedule,
            resolved,
            missionModel,
            engine,
            activityTopic
        );

        // Drive the engine until we're out of time.
        // TERMINATION: Actually, we might never break if real time never progresses forward.
        while (true) {
          final var nextTime = engine.peekNextTime().orElse(Duration.MAX_VALUE);
          if (shouldTakeCheckpoint.apply(elapsedTime, nextTime)) {
            System.out.println("Taking checkpoint at " + elapsedTime);
            cells.freeze();
            checkpoints.add(new CachedSimulationEngine(
                elapsedTime,
                schedule.values().stream().toList(),
                engine,
                cells,
                makeCombinedTimeline(timelines, timeline).points(),
                activityTopic
            ));
            timelines.add(timeline);
            engine = engine.duplicate();
            timeline = new TemporalEventSource();
            cells = new LiveCells(timeline, cells);
            System.out.println("Finished taking checkpoint at " + elapsedTime);
          }

          final var batch = engine.extractNextJobs(simulationDuration);
          // Increment real time, if necessary.
          final var delta = batch.offsetFromStart().minus(elapsedTime);
          elapsedTime = batch.offsetFromStart();
          timeline.add(delta);
          // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
          //   even if they occur at the same real time.

          simulationExtentConsumer.accept(elapsedTime);

          if (batch.jobs().isEmpty() && batch.offsetFromStart().isEqualTo(simulationDuration)) {
            break;
          }

          // Run the jobs in this batch.
          final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, simulationDuration);
          timeline.add(commit);
        }
      } catch (Throwable ex) {
        throw new SimulationException(elapsedTime, simulationStartTime, ex);
      }

      return new SimulationResultsWithCheckpoints(SimulationEngine.computeResults(
          engine,
          simulationStartTime,
          elapsedTime,
          activityTopic,
          makeCombinedTimeline(timelines, timeline),
          missionModel.getTopics()), checkpoints);
    }
  }

  public static <Model, Return>
  void simulateTask(final MissionModel<Model> missionModel, final TaskFactory<Return> task) {
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

      // Start daemon task(s) immediately, before anything else happens.
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
      {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
        timeline.add(commit);
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
        timeline.add(delta);
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
        timeline.add(commit);
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
    return executor -> oneShotTask(scheduler0 -> TaskStatus.calling((TaskFactory<Output>) (executor1 -> oneShotTask(scheduler1 -> {
      scheduler1.emit(directiveId, activityTopic);
      return task.create(executor1).step(scheduler1);
    })), oneShotTask(scheduler2 -> {
      // When the current activity finishes, get the list of the activities that needed this activity to finish to know their start time
      final List<Pair<ActivityDirectiveId, Duration>> dependents = resolved.get(directiveId) == null ? List.of() : resolved.get(directiveId);
      // Iterate over the dependents
      for (final var dependent : dependents) {
        scheduler2.spawn(executor2 -> oneShotTask(scheduler3 ->
            // Delay until the dependent starts
            TaskStatus.delayed(dependent.getRight(), oneShotTask(scheduler4 -> {
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
            })))
        );
      }
      return TaskStatus.completed(Unit.UNIT);
    })));
  }

  public static <T> Task<T> oneShotTask(Function<Scheduler, TaskStatus<T>> f) {
    return new Task<>() {
      @Override
      public TaskStatus<T> step(final Scheduler scheduler) {
        return f.apply(scheduler);
      }

      @Override
      public Task<T> duplicate(Executor executor) {
        return this;
      }
    };
  }
}
