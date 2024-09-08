package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SpanException;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.resources.InMemorySimulationResourceManager;
import gov.nasa.jpl.aerie.merlin.driver.resources.SimulationResourceManager;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskEntryPoint;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventSource;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Query;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.types.ActivityDirective;
import gov.nasa.jpl.aerie.types.ActivityDirectiveId;
import gov.nasa.jpl.aerie.types.SerializedActivity;
import gov.nasa.jpl.aerie.types.Timestamp;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public final class SimulationDriver {
  public static <Model> SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final Supplier<Boolean> simulationCanceled
  ) {
    return simulate(
        missionModel,
        schedule,
        simulationStartTime,
        simulationDuration,
        planStartTime,
        planDuration,
        simulationCanceled,
        $ -> {},
        new InMemorySimulationResourceManager(), Optional.empty()).getLeft();
  }

  public static <Model> Pair<SimulationResults, SerializedValue> simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final Supplier<Boolean> simulationCanceled,
      final Consumer<Duration> simulationExtentConsumer,
      final SimulationResourceManager resourceManager,
      final Optional<SerializedValue> incons
  ) {
    var timeline = new TemporalEventSource();
    final var executor = Executors.newVirtualThreadPerTaskExecutor();

    final LiveCells cells = initCells(missionModel, incons, timeline);
    try (final var engine = new SimulationEngine(cells, executor)) {
      if (incons.isPresent()) {
        engine.hydrateInitialConditions(missionModel, executor, incons.get(), cells, simulationStartTime);
      }

      /* The current real time. */
      simulationExtentConsumer.accept(ZERO);

      // Specify a topic on which tasks can log the activity they're associated with.
      final var activityTopic = new Topic<ActivityDirectiveId>();

      try {
        if (incons.isEmpty()) {
          engine.init(missionModel.getResources(), Optional.of(missionModel.getDaemon()));
        } else {
          engine.init(missionModel.getResources(), Optional.empty());
        }

        // Get all activities as close as possible to absolute time
        // Schedule all activities.
        // Using HashMap explicitly because it allows `null` as a key.
        // `null` key means that an activity is not waiting on another activity to finish to know its start time
        HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(planDuration, schedule).compute();
        if (!resolved.isEmpty()) {
          resolved.put(
              null,
              StartOffsetReducer.adjustStartOffset(
                  resolved.get(null),
                  Duration.of(
                      planStartTime.until(simulationStartTime, ChronoUnit.MICROS),
                      MICROSECONDS)));
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

        // Drive the engine until we're out of time or until simulation is canceled.
        // TERMINATION: Actually, we might never break if real time never progresses forward.
        engineLoop:
        while (!simulationCanceled.get()) {
          if(simulationCanceled.get()) break;
          final var status = engine.step(simulationDuration);
          switch (status) {
            case SimulationEngine.Status.CompleteNoJobs s: break engineLoop;
            case SimulationEngine.Status.CompleteAtDuration s: break engineLoop;
            case SimulationEngine.Status.InProgress s:
              resourceManager.acceptUpdates(s.elapsedTime(), s.realResourceUpdates(), s.dynamicResourceUpdates());
              break;
          }
          simulationExtentConsumer.accept(engine.getElapsedTime());
        }

      } catch (SpanException ex) {
        // Swallowing the spanException as the internal `spanId` is not user meaningful info.
        final var topics = missionModel.getTopics();
        final var directiveDetail = engine.getDirectiveDetailsFromSpan(activityTopic, topics, ex.spanId);
        if(directiveDetail.directiveId().isPresent()) {
          throw new SimulationException(
              engine.getElapsedTime(),
              simulationStartTime,
              directiveDetail.directiveId().get(),
              directiveDetail.activityStackTrace(),
              ex.cause);
        }
        throw new SimulationException(engine.getElapsedTime(), simulationStartTime, ex.cause);
      } catch (Throwable ex) {
        throw new SimulationException(engine.getElapsedTime(), simulationStartTime, ex);
      }

      System.out.println("-----------------------------------------");
      final var serializedCells = new ArrayList<SerializedValue>();
      for (final var query : missionModel.getInitialCells().queries()) {
        final var cell = engine.cells.getCell(query);
        serializedCells.add(cell.get().serialize());
      }
      final var serializedTasks = new ArrayList<SerializedValue>();
      final var spanInfo = engine.computeSpanInfo(activityTopic, missionModel.getTopics(), engine.timeline);

      final var tasksSaved = new LinkedHashSet<TaskId>();
      final var tasksNeeded = new LinkedHashSet<>(engine.tasks.keySet());

      Set<TaskId> missingTasks = new LinkedHashSet<>(tasksNeeded);

      while (!missingTasks.isEmpty()) {
        for (final var task : missingTasks) {
          tasksSaved.add(task);
          final var spanId = engine.fincons.taskSpan().get(task);
          final String entrypointId = engine.fincons.entrypoints().get(task) != null ? engine.fincons.entrypoints().get(task).id() : TaskEntryPoint.freshId();
          final Optional<TaskEntryPoint.ParentReference> parent;
          if (engine.fincons.entrypoints().containsKey(task)) {
            final TaskEntryPoint entrypoint = engine.fincons.entrypoints().get(task);
            if (entrypoint instanceof TaskEntryPoint.Subtask e) {
              parent = Optional.of(e.parent$());
            } else {
              parent = Optional.empty();
            }
          } else {
            parent = Optional.empty();
          }
          TaskEntryPoint entrypoint;
          if (spanInfo.isActivity(spanId) && spanInfo.isActivity(task)) {
            entrypoint = new TaskEntryPoint.Directive(entrypointId, simulationStartTime.plus(engine.spans.get(spanId).startOffset().in(MICROSECONDS), ChronoUnit.MICROS), spanInfo.input().get(spanId), parent);
          } else {
            entrypoint = engine.fincons.entrypoints().get(task);
            if (engine.fincons.taskParent().get(task) != null) tasksNeeded.add(engine.fincons.taskParent().get(task));
            if (entrypoint == null) entrypoint = new TaskEntryPoint.SystemTask(TaskEntryPoint.freshId(), "SimulationDriver serializing tasks");
          }

          if (!(entrypoint instanceof TaskEntryPoint.SystemTask)) {
            serializedTasks.add(serializeTask(engine, task, entrypoint));
          }
        }
        missingTasks = setDifference(tasksNeeded, tasksSaved);
      }

      final SerializedValue fincons = SerializedValue.of(Map.of(
          "cells", SerializedValue.of(serializedCells),
          "tasks", SerializedValue.of(serializedTasks)
      ));

      final var topics = missionModel.getTopics();
      final var results = engine.computeResults(simulationStartTime, activityTopic, topics, resourceManager);

      return Pair.of(results, fincons);
    }
  }

  private static <Model> LiveCells initCells(MissionModel<Model> missionModel, Optional<SerializedValue> incons$, final EventSource timeline) {
    if (incons$.isEmpty()) return new LiveCells(timeline, missionModel.getInitialCells());

    final var incons = incons$.get();

    final var cells = new LiveCells(timeline);
    final var serializedCells = incons.asMap().get().get("cells").asList().get();
    final var queries = missionModel.getInitialCells().queries();

    for (int i = 0; i < serializedCells.size(); i++) {
      final var serializedCell = serializedCells.get(i);
      final var query = queries.get(i);

      putCell(cells, query, serializedCell, missionModel.getInitialCells());
    }
    return cells;
  }

  private static <State> void putCell(
      LiveCells liveCells,
      Query<State> query,
      SerializedValue serializedCell,
      LiveCells cells)
  {
    liveCells.put(query, cells.getCell(query).get().deserialize(serializedCell));
  }

  private static <T> Set<T> setDifference(Set<T> a, Set<T> b) {
    final var aCopy = new LinkedHashSet<>(a);
    aCopy.removeAll(b);
    return aCopy;
  }

  private static SerializedValue serializeTask(SimulationEngine engine, TaskId task, TaskEntryPoint entrypoint) {
    final var lastStepTime = engine.fincons.lastStepTime().get(task);
    final List<SerializedValue> readLog = engine.fincons.readLog().containsKey(task) ? engine.fincons.readLog().get(task) : List.of();
    final var numSteps = engine.fincons.taskSteps().get(task);
    final var numChildren = engine.fincons.childCount().get(task);
    final var finished = engine.tasks.get(task) == null;
    final var serializedEntryPoint = switch (entrypoint) {
      case TaskEntryPoint.Directive e -> SerializedValue.of(Map.of(
          "id",
          SerializedValue.of(e.id()),
          "type",
          SerializedValue.of("directive"),
          "parentId",
          e.parent().map($ -> SerializedValue.of($.id())).orElse(SerializedValue.NULL),
          "childNumber",
          e.parent().map($ -> SerializedValue.of($.childNumber())).orElse(SerializedValue.NULL),
          "directive",
          SerializedValue.of(Map.of(
              "type",
              SerializedValue.of(e.directive().getTypeName()),
              "args",
              SerializedValue.of(e.directive().getArguments()))),
          "startTime",
          SerializedValue.of(new Timestamp(e.startTime()).toString())
      ));
      case TaskEntryPoint.Daemon e -> SerializedValue.of(Map.of(
          "id",
          SerializedValue.of(e.id()),
          "type",
          SerializedValue.of("daemon")
      ));
      case TaskEntryPoint.Subtask e -> SerializedValue.of(Map.of(
          "id",
          SerializedValue.of(e.id()),
          "type",
          SerializedValue.of("subtask"),
          "parentId",
          SerializedValue.of(e.parent$().id()),
          "childNumber",
          SerializedValue.of(e.parent$().childNumber())
      ));
      case TaskEntryPoint.SystemTask e -> SerializedValue.of(Map.of(
          "id",
          SerializedValue.of(e.id()),
          "type",
          SerializedValue.of("system"),
          "comment",
          SerializedValue.of(e.comment())
      ));
    };

    return SerializedValue.of(Map.of(
        "entrypoint", serializedEntryPoint,
        "reads", SerializedValue.of(readLog),
        "steps", numSteps == null ? SerializedValue.NULL : SerializedValue.of(numSteps),
        "children", numSteps == null ? SerializedValue.NULL : SerializedValue.of(numChildren),
        "lastStepTime", lastStepTime == null ? SerializedValue.NULL : SerializedValue.of(lastStepTime.minus(engine.getElapsedTime()).in(MICROSECONDS)),  // expected to be negative
        "finished", SerializedValue.of(finished)
    ));
  }

  // This method is used as a helper method for executing unit tests
  public static <Model, Return>
  void simulateTask(final MissionModel<Model> missionModel, final TaskFactory<Return> task) {
    try (final var engine = new SimulationEngine(missionModel.getInitialCells())) {
      // Track resources and kick off daemon tasks
      try {
        engine.init(missionModel.getResources(), Optional.of(missionModel.getDaemon()));
      } catch (Throwable t) {
        throw new RuntimeException("Exception thrown while starting daemon tasks", t);
      }

      // Schedule the task.
      final var spanId = engine.scheduleTask(ZERO, task, new TaskEntryPoint.Daemon(TaskEntryPoint.freshId()));

      // Drive the engine until the scheduled task completes.
      while (!engine.getSpan(spanId).isComplete()) {
        try {
          engine.step(Duration.MAX_VALUE);
        } catch (Throwable t) {
          throw new RuntimeException("Exception thrown while simulating tasks", t);
        }
      }
    }
  }

  private static <Model> void scheduleActivities(
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final SimulationEngine engine,
      final Topic<ActivityDirectiveId> activityTopic
  ) {
    if (resolved.get(null) == null) {
      // Nothing to simulate
      return;
    }
    for (final Pair<ActivityDirectiveId, Duration> directivePair : resolved.get(null)) {
      final var directiveId = directivePair.getLeft();
      final var startOffset = directivePair.getRight();
      final var serializedDirective = schedule.get(directiveId).serializedActivity();

      final TaskFactory<?> task = deserializeActivity(missionModel, serializedDirective);

      engine.scheduleTask(startOffset, makeTaskFactory(
          directiveId,
          task,
          schedule,
          resolved,
          missionModel,
          activityTopic
      ), new TaskEntryPoint.SystemTask(TaskEntryPoint.freshId(), "SimulationDriver::scheduleActivities"));
    }
  }

  private static <Model, Output> TaskFactory<Unit> makeTaskFactory(
      final ActivityDirectiveId directiveId,
      final TaskFactory<Output> taskFactory,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final Topic<ActivityDirectiveId> activityTopic
  ) {
    record Dependent(Duration offset, TaskFactory<?> task) {}

    final List<Dependent> dependents = new ArrayList<>();
    for (final var pair : resolved.getOrDefault(directiveId, List.of())) {
      dependents.add(new Dependent(
          pair.getRight(),
          makeTaskFactory(
              pair.getLeft(),
              deserializeActivity(missionModel, schedule.get(pair.getLeft()).serializedActivity()),
              schedule,
              resolved,
              missionModel,
              activityTopic)));
    }

    return executor -> {
      final var task = taskFactory.create(executor);
      return Task
          .callingWithSpan(
              Task.emitting(directiveId, activityTopic)
                  .andThen(task))
          .andThen(
              Task.spawning(
                  dependents
                      .stream()
                      .map(
                          dependent ->
                              TaskFactory.delaying(dependent.offset())
                                         .andThen(dependent.task()))
                      .toList()));
    };
  }

  public static <Model> TaskFactory<?> deserializeActivity(
      MissionModel<Model> missionModel,
      SerializedActivity serializedDirective) {
    final TaskFactory<?> task;
    try {
      task = missionModel.getTaskFactory(serializedDirective);
    } catch (final InstantiationException ex) {
      // All activity instantiations are assumed to be validated by this point
      throw new Error("Unexpected state: activity instantiation %s failed with: %s"
                          .formatted(serializedDirective.getTypeName(), ex.toString()));
    }
    return task;
  }
}
