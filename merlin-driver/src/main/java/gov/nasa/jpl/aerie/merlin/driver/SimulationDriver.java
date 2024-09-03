package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.EngineCellId;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SpanException;
import gov.nasa.jpl.aerie.merlin.driver.resources.InMemorySimulationResourceManager;
import gov.nasa.jpl.aerie.merlin.driver.resources.SimulationResourceManager;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskEntryPoint;
import gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser;
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
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import java.util.ArrayList;

import javax.json.Json;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Unit.UNIT;

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
        new InMemorySimulationResourceManager());
  }

  public static <Model> SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final Supplier<Boolean> simulationCanceled,
      final Consumer<Duration> simulationExtentConsumer,
      final SimulationResourceManager resourceManager
  ) {
    var timeline = new TemporalEventSource();

    final LiveCells cells;

    record ScheduleEntry<T>(Duration startTime, TaskFactory<T> state, TaskEntryPoint entrypoint) {}
    final List<ScheduleEntry<?>> tasksToSchedule = new ArrayList<>();
    final var executor = Executors.newVirtualThreadPerTaskExecutor();

    final String inconsFile = "fincons.json";
    if (new File(inconsFile).exists()) {
      try {
        cells = new LiveCells(timeline);
        final var incons = new SerializedValueJsonParser().parse(
            Json
                .createReader(new StringReader(Files.readString(Path.of(inconsFile))))
                .readValue()).getSuccessOrThrow();
        final var serializedCells = incons.asMap().get().get("cells").asList().get();
        final var queries = missionModel.getInitialCells().queries();

        for (int i = 0; i < serializedCells.size(); i++) {
          final var serializedCell = serializedCells.get(i);
          final var query = queries.get(i);

          putCell(cells, query, serializedCell, missionModel.getInitialCells());
        }

        // First pass: build map by id, include refcount
        final var references = new LinkedHashMap<String, MutableInt>();

        record TaskToRestart(String id, List<Integer> childrenToRestart) {}
        record ReadyTask(TaskFactory<?> taskFactory, int steps, List<SerializedValue> readLog, Duration lastStepTime, String type, Map<String, SerializedValue> args) {}
        record WaitingTask(int childNumber, int steps, Duration lastStepTime) {}

        final var waitingTasks = new LinkedHashMap<String, List<WaitingTask>>();
        final var readyToRestart = new LinkedList<ReadyTask>();
        for (final var serializedTask : incons.asMap().get().get("tasks").asList().get()) {
          final var entrypoint = serializedTask.asMap().get().get("entrypoint").asMap().get();

          final var id = entrypoint.get("id").asString().get();
          references.computeIfAbsent(id, $ -> new MutableInt());

          switch (entrypoint.get("type").asString().get()) {
            case "directive" -> {
              final var directive = entrypoint.get("directive").asMap().get();
              final var type = directive.get("type").asString().get();
              final var args = directive.get("args").asMap().get();
              final var taskFactory = deserializeActivity(missionModel, new SerializedActivity(type, args));
              final var steps = serializedTask.asMap().get().get("steps").asInt().get();
              readyToRestart.add(new ReadyTask(
                  taskFactory,
                  (int) (long) steps,
                  serializedTask.asMap().get().get("reads").asList().get(),
                  Duration.of(serializedTask.asMap().get().get("lastStepTime").asInt().get(), MICROSECONDS),
                  type,
                  args));
            }

            case "subtask" -> {
              final var parentId = entrypoint.get("parentId").asString().get();
              final var childNumber = entrypoint.get("childNumber").asInt().get();
              final var steps = serializedTask.asMap().get().get("steps").asInt().get();
              waitingTasks.computeIfAbsent(parentId, $ -> new ArrayList<>()).add(
                  new WaitingTask((int) (long) childNumber,
                                  (int) (long) steps,
                                  Duration.of(serializedTask.asMap().get().get("lastStepTime").asInt().get(), MICROSECONDS)));
            }

            case "system" -> {
//              readyToRestart.add(new ReadyTask(
//                  taskFactory,
//                  (int) (long) steps,
//                  serializedTask.asMap().get().get("reads").asList().get(),
//                  Duration.of(serializedTask.asMap().get().get("lastStepTime").asInt().get(), MICROSECONDS),
//                  type,
//                  args));
            }

            case "daemon" -> {}
          }
        }

        while (!readyToRestart.isEmpty()) {
          final var readyTask = readyToRestart.pop();
          final var readIterator = readyTask.readLog().iterator();
          final var task = readyTask.taskFactory().create(executor);
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

            }
          };
          TaskStatus<?> status = null;
          for (var i = 0; i < readyTask.steps(); i++) {
            status = task.step(scheduler);
          }
          final TaskStatus<?> $status =
              switch (status) {
                case TaskStatus.AwaitingCondition<?> s -> s;
                case TaskStatus.CallingTask<?> s -> s;
                case TaskStatus.Completed<?> s -> s;
                case TaskStatus.Delayed<?> s -> {
                  yield TaskStatus.delayed(s.delay().plus(readyTask.lastStepTime()), s.continuation());
                }
                case null -> null;
              };
          if ($status == null) {
            tasksToSchedule.add(new ScheduleEntry<>(ZERO, $ -> task, new TaskEntryPoint.Directive(TaskEntryPoint.freshId(), new SerializedActivity(readyTask.type, readyTask.args))));
          } else {
            tasksToSchedule.add(new ScheduleEntry<>(ZERO, $ -> Task.of($$ -> $status).andThen(task), new TaskEntryPoint.Directive(TaskEntryPoint.freshId(), new SerializedActivity(readyTask.type, readyTask.args))));
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      cells = new LiveCells(timeline, missionModel.getInitialCells());
    }

    try (final var engine = new SimulationEngine(cells, executor)) {
      for (final var entry : tasksToSchedule) {
        engine.scheduleTask(entry.startTime(), entry.state(), entry.entrypoint());
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
            case SimulationEngine.Status.NoJobs noJobs: break engineLoop;
            case SimulationEngine.Status.AtDuration atDuration: break engineLoop;
            case SimulationEngine.Status.Nominal nominal:
              resourceManager.acceptUpdates(nominal.elapsedTime(), nominal.realResourceUpdates(), nominal.dynamicResourceUpdates());
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

      // Task needs:
      // - entry point
      // - reads
      // The entry point is either:
      //   - An activity directive
      //   - An activity directive with a path to a particular anonymous subtask. Enumerate calls and spawns. E.g:
      //           Parent()->2->1 is the first child of the second child of Parent
      // The list of reads is linear and contains parent and child reads combined???
      //    Can we dedupe? Later...
      // Number of steps

      try {
        // get unfinished tasks
        // for each unfinished task, get its entrypoint

        final SimulationEngine.SpanInfo spanInfo = engine.computeSpanInfo(activityTopic, missionModel.getTopics(), engine.combineTimeline());

//        spanInfo.isActivity()

        for (final var task : engine.tasks.keySet()) {
          final var entrypoint = engine.entrypoints.get(task);

          // TODO look up the activity args event and replace the entrypoint if you can


          final var lastStepTime = engine.lastStepTime.get(task);
          final List<SerializedValue> readLog = engine.readLog.containsKey(task) ? engine.readLog.get(task) : List.of();
          final var numSteps = engine.taskSteps.get(task);
          final var serializedEntryPoint = switch (entrypoint) {
            case TaskEntryPoint.Directive e -> SerializedValue.of(Map.of(
                "id",
                SerializedValue.of(e.id()),
                "type",
                SerializedValue.of("directive"),
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
                SerializedValue.of(e.parentId()),
                "childNumber",
                SerializedValue.of(e.childNumber())
            ));
            case TaskEntryPoint.SystemTask e -> SerializedValue.of(Map.of(
                "id",
                SerializedValue.of(e.id()),
                "type",
                SerializedValue.of("system")
            ));
          };
          serializedTasks.add(SerializedValue.of(Map.of(
              "entrypoint", serializedEntryPoint,
              "reads", SerializedValue.of(readLog),
              "steps", SerializedValue.of(numSteps),
              "lastStepTime",
              SerializedValue.of(lastStepTime.minus(engine.getElapsedTime()).in(MICROSECONDS))))); // expected to be negative
        }

        Files.write(Path.of(inconsFile), List.of(new SerializedValueJsonParser().unparse(SerializedValue.of(Map.of(
            "cells", SerializedValue.of(serializedCells),
            "tasks", SerializedValue.of(serializedTasks)
        ))).toString()), StandardOpenOption.CREATE);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      final var topics = missionModel.getTopics();
      return engine.computeResults(simulationStartTime, activityTopic, topics, resourceManager);
    }
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
      ), new TaskEntryPoint.SystemTask(TaskEntryPoint.freshId()));
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
