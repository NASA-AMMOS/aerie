package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.EngineCellId;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SpanException;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.resources.InMemorySimulationResourceManager;
import gov.nasa.jpl.aerie.merlin.driver.resources.SimulationResourceManager;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskEntryPoint;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Query;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.types.ActivityDirective;
import gov.nasa.jpl.aerie.types.ActivityDirectiveId;
import gov.nasa.jpl.aerie.types.SerializedActivity;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
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

    InitialConditions initialConditions;
    if (incons.isPresent()) {
      initialConditions = hydrateInitialConditions(missionModel, timeline, executor, incons.get());
    } else {
      initialConditions = new InitialConditions(new LiveCells(timeline, missionModel.getInitialCells()), List.of());
    }

    try (final var engine = new SimulationEngine(initialConditions.cells(), executor)) {
      for (final var task : initialConditions.tasks()) {
        engine.scheduleTask(ZERO, task.getLeft(), task.getRight()); // TODO figure out how to propagate entrypoints for previous tasks, especially child directives
      }

      /* The current real time. */
      simulationExtentConsumer.accept(ZERO);

      // Specify a topic on which tasks can log the activity they're associated with.
      final var activityTopic = new Topic<ActivityDirectiveId>();

      try {
        engine.init(missionModel.getResources(), missionModel.getDaemon());

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

      var missingTasks = setDifference(tasksNeeded, tasksSaved);

      while (!missingTasks.isEmpty()) {
        for (final var task : missingTasks) {
          tasksSaved.add(task);
          final var spanId = engine.taskSpan.get(task);
          final String entrypointId = engine.entrypoints.get(task) != null ? engine.entrypoints.get(task).id() : TaskEntryPoint.freshId();
          final Optional<TaskEntryPoint.ParentReference> parent;
          if (engine.entrypoints.containsKey(task)) {
            final TaskEntryPoint entrypoint = engine.entrypoints.get(task);
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
            entrypoint = new TaskEntryPoint.Directive(entrypointId, spanInfo.input().get(spanId), parent);
          } else {
            entrypoint = engine.entrypoints.get(task);
            tasksNeeded.add(engine.taskParent.get(task));
            if (entrypoint == null) entrypoint = new TaskEntryPoint.SystemTask(TaskEntryPoint.freshId(), "SimulationDriver serializing tasks");
          }
          serializedTasks.add(serializeTask(engine, task, entrypoint));
        }
        missingTasks = setDifference(tasksNeeded, tasksSaved);
      }

      final SerializedValue fincons = SerializedValue.of(Map.of(
          "cells", SerializedValue.of(serializedCells),
          "tasks", SerializedValue.of(serializedTasks)
      ));

      final var topics = missionModel.getTopics();
      final var results = engine.computeResults(simulationStartTime, activityTopic, topics, resourceManager);

      final var format =
          new DateTimeFormatterBuilder()
              .appendPattern("uuuu-DDD'T'HH:mm:ss")
              .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
              .toFormatter();

      results.simulatedActivities.put(
          new ActivityInstanceId(1000L),
          new ActivityInstance("parent", Map.of(), LocalDateTime.parse("2024-252T10:10:10.169", format).atZone(ZoneOffset.UTC).toInstant(), Duration.HOUR.times(24 * 84), null, List.of(), Optional.empty(), SerializedValue.of(Map.of())));
      return Pair.of(results, fincons);
    }
  }

  private static <T> Set<T> setDifference(Set<T> a, Set<T> b) {
    final var aCopy = new LinkedHashSet<>(a);
    aCopy.removeAll(b);
    return aCopy;
  }

  private static SerializedValue serializeTask(SimulationEngine engine, TaskId task, TaskEntryPoint entrypoint) {
    final var lastStepTime = engine.lastStepTime.get(task);
    final List<SerializedValue> readLog = engine.readLog.containsKey(task) ? engine.readLog.get(task) : List.of();
    final var numSteps = engine.taskSteps.get(task);
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
              SerializedValue.of(e.directive().getArguments())))
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
        "lastStepTime", lastStepTime == null ? SerializedValue.NULL : SerializedValue.of(lastStepTime.minus(engine.getElapsedTime()).in(MICROSECONDS)),  // expected to be negative
        "finished", SerializedValue.of(finished)
    ));
  }

  private record InitialConditions(LiveCells cells, List<Pair<TaskFactory<?>, TaskEntryPoint>> tasks) {}

  record MyTask<T>(MutableObject<TaskFactory<T>> task, long steps, List<SerializedValue> readLog, Duration lastStepTime, TaskEntryPoint entrypoint, List<MyTask<?>> childrenToRestart) {
    <F> void setTaskFactory(TaskFactory<F> taskFactory) {
      this.task.setValue((TaskFactory<T>) taskFactory);
    }
  }

  private static <Model> InitialConditions hydrateInitialConditions(
      final MissionModel<Model> missionModel,
      final TemporalEventSource timeline,
      final ExecutorService executor,
      final SerializedValue incons)
  {
    final LiveCells cells;

    cells = new LiveCells(timeline);
    final var serializedCells = incons.asMap().get().get("cells").asList().get();
    final var queries = missionModel.getInitialCells().queries();

    for (int i = 0; i < serializedCells.size(); i++) {
      final var serializedCell = serializedCells.get(i);
      final var query = queries.get(i);

      putCell(cells, query, serializedCell, missionModel.getInitialCells());
    }

    final var tasks = new LinkedHashMap<String, MyTask<?>>();
    for (final var serializedTask : incons.asMap().get().get("tasks").asList().get()) {
      final var entrypoint = serializedTask.asMap().get().get("entrypoint").asMap().get();

      final var id = entrypoint.get("id").asString().get();

      switch (entrypoint.get("type").asString().get()) {
        case "directive" -> {
          final var directive = entrypoint.get("directive").asMap().get();
          final var type = directive.get("type").asString().get();
          final var args = directive.get("args").asMap().get();
          final var taskFactory = deserializeActivity(missionModel, new SerializedActivity(type, args));
          final var steps = serializedTask.asMap().get().get("steps").asInt().get();
          final Optional<TaskEntryPoint.ParentReference> parentReference;
          if (!entrypoint.get("parentId").isNull()) {
            parentReference = Optional.of(new TaskEntryPoint.ParentReference(entrypoint.get("parentId").asString().get(), entrypoint.get("childNumber").asInt().get()));
          } else {
            parentReference = Optional.empty();
          }
          final List<SerializedValue> readLog = serializedTask.asMap().get().get("reads").asList().get();
          final Duration lastStepTime = Duration.of(
              serializedTask.asMap().get().get("lastStepTime").asInt().get(),
              MICROSECONDS);
          tasks.put(id, new MyTask<>(new MutableObject<>(taskFactory), steps, readLog, lastStepTime, new TaskEntryPoint.Directive(id, new SerializedActivity(type, args), parentReference), new ArrayList<>()));
        }

        case "subtask" -> {
          final var parentId = entrypoint.get("parentId").asString().get();
          final var childNumber = entrypoint.get("childNumber").asInt().get();
          final var parentReference = new TaskEntryPoint.ParentReference(parentId, childNumber);
          final var steps = serializedTask.asMap().get().get("steps").asInt().get();

          final List<SerializedValue> readLog = serializedTask.asMap().get().get("reads").asList().get();
          final Duration lastStepTime = Duration.of(
              serializedTask.asMap().get().get("lastStepTime").asInt().get(),
              MICROSECONDS);
          tasks.put(id, new MyTask<>(new MutableObject<>(null), steps, readLog, lastStepTime, new TaskEntryPoint.Subtask(id, parentReference), new ArrayList<>()));
        }

        case "system" -> {
        }

        case "daemon" -> {
          // TODO
        }
      }
    }

    var rootIds = new LinkedHashSet<>(tasks.keySet());
    for (final var entry : tasks.entrySet()) {
      final var task = entry.getValue();
      if (task.entrypoint.parent().isPresent()) {
        final var parent = tasks.get(task.entrypoint.parent().get().id());
        if (parent != null) {
          parent.childrenToRestart.add(task);
          rootIds.remove(entry.getKey());
        }
      }
    }

    final var roots = new ArrayList<MyTask<?>>();
    for (final var taskId : rootIds) {
      roots.add(tasks.get(taskId));
    }

    final InitialConditions result = new InitialConditions(cells, new ArrayList<>());
    for (final var task : roots) {
      result.tasks().add(Pair.of(instantiateTask(task, cells, executor), task.entrypoint()));
    }
    return result;
  }

  private static <T> TaskFactory<T> instantiateTask(MyTask<T> readyTask, final LiveCells cells, ExecutorService executor) {
    final var readIterator = readyTask.readLog().iterator();
    final var task = readyTask.task().getValue().create(executor);

    final var childCounter = new MutableLong(0);

    final var childrenToSpawn = new ArrayList<TaskFactory<?>>();

    final var toEmit = new ArrayList<Pair<Object, Topic>>();

    final var scheduler = new Scheduler() {
      @Override
      public <State> State get(final CellId<State> cellId) {
        final var query = ((EngineCellId<?, State>) cellId);
        // TODO: Cache the return value (until the next emit or until the task yields) to avoid unnecessary copies
        //  if the same state is requested multiple times in a row.
        return cells.getCell(query.query()).get().deserialize(readIterator.next()).getState();
      }

      @Override
      public <Event> void emit(final Event event, final Topic<Event> topic) {
      }

      @Override
      public void spawn(final InSpan taskSpan, final TaskFactory<?> task) {
        for (final var waitingChild : readyTask.childrenToRestart) {
          if (childCounter.getValue().equals(waitingChild.entrypoint().parent().get().childNumber())) {
            waitingChild.setTaskFactory(task);
            childrenToSpawn.add(instantiateTask(waitingChild, cells, executor));
          }
        }
        childCounter.increment();
      }
    };
    TaskStatus<T> status = null;
    for (var i = 0; i < readyTask.steps(); i++) {
      status = task.step(scheduler);
      if (status instanceof TaskStatus.CallingTask<T> s) {
        for (final var waitingChild : readyTask.childrenToRestart) {
          if (childCounter.getValue().equals(waitingChild.entrypoint().parent().get().childNumber())) {
            waitingChild.setTaskFactory(s.child());
            status = TaskStatus.calling(s.childSpan(), instantiateTask(waitingChild, cells, executor), s.continuation());
          }
        }
        childCounter.increment();
      }
    }

    if (status instanceof TaskStatus.Delayed<T> s) {
      status = TaskStatus.delayed(s.delay().plus(readyTask.lastStepTime), s.continuation());
    }

    final var finalStatus = status;
    return $ -> Task.of(s -> {
      for (final var child : childrenToSpawn) {
        s.spawn(InSpan.Fresh, child);
      }
      return finalStatus;
    });
  }

  private static <State> void putCell(
      LiveCells liveCells,
      Query<State> query,
      SerializedValue serializedCell,
      LiveCells cells)
  {
    liveCells.put(query, cells.getCell(query).get().deserialize(serializedCell));
  }

  // This method is used as a helper method for executing unit tests
  public static <Model, Return>
  void simulateTask(final MissionModel<Model> missionModel, final TaskFactory<Return> task) {
    try (final var engine = new SimulationEngine(missionModel.getInitialCells())) {
      // Track resources and kick off daemon tasks
      try {
        engine.init(missionModel.getResources(), missionModel.getDaemon());
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

  private static <Model> TaskFactory<?> deserializeActivity(MissionModel<Model> missionModel, SerializedActivity serializedDirective) {
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
