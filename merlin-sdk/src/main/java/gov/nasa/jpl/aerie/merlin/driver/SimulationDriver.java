package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.TaskFactory;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskFrame;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskInfo;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskQueue;
import gov.nasa.jpl.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.SimulationTimeline;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class SimulationDriver {
  public static <$Schema> SimulationResults simulate(
      final Adaptation<$Schema> adaptation,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration)
  throws TaskSpecInstantiationException
  {
    return simulate(adaptation, SimulationTimeline.create(adaptation.getSchema()), schedule, startTime, simulationDuration);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <$Schema, $Timeline extends $Schema> SimulationResults simulate(
      final Adaptation<$Schema> adaptation,
      final SimulationTimeline<$Timeline> database,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration)
  throws TaskSpecInstantiationException
  {
    final var taskIdToActivityId = new HashMap<String, String>();
    final var taskFactory = new TaskFactory<$Schema, $Timeline>(adaptation.getTaskSpecificationTypes());

    // A time-ordered queue of all scheduled future tasks.
    final var queue = new TaskQueue<String>();
    // The set of all tasks that have completed.
    final var completedTasks = new HashSet<String>();
    // For each task, a set of tasks awaiting its completion (if any).
    final var waitingTasks = new HashMap<String, Set<String>>();
    // For each task, the condition blocking its progress (if any).
    final var conditionedTasks = new HashMap<String, Condition<? super $Timeline>>();

    {
      final var daemon = adaptation.<$Timeline>getDaemon();
      final var activityId = UUID.randomUUID().toString();

      final var info = taskFactory.createAnonymousTask(daemon, Optional.empty());
      info.isDaemon = true;

      taskIdToActivityId.put(info.id, activityId);
      queue.deferTo(Duration.ZERO, info.id);
    }

    for (final var entry : schedule.entrySet()) {
      final var activityId = entry.getKey();
      final var startDelta = entry.getValue().getLeft();
      final var activity = entry.getValue().getRight();

      final TaskInfo<$Timeline> info;
      try {
        info = taskFactory.createTask(activity.getTypeName(), activity.getParameters(), Optional.empty());
      } catch (final InstantiationException ex) {
        throw new TaskSpecInstantiationException(activityId, ex);
      }

      taskIdToActivityId.put(info.id, activityId);
      queue.deferTo(startDelta, info.id);
    }

    // Collect profiles for all resources.
    final var profiles = new HashMap<String, ProfileBuilder<$Schema, ?, ?>>();
    for (final var family : adaptation.getResourceFamilies()) createProfilesForFamily(family, profiles::put);

    var now = database.origin();
    for (final var profile : profiles.values()) profile.updateAt(now);

    // Step the stimulus program forward until we reach the end of the simulation.
    final var changedTables = new boolean[database.getTableCount()];
    now = queue.consumeUpTo(simulationDuration, now, (delta, frame) -> {
      Arrays.fill(changedTables, false);

      final var yieldTime = TaskFrame.runToCompletion(frame, (taskId, builder) -> {
        final var info = taskFactory.get(taskId);

        final var status = info.step(queue.getElapsedTime(), new Scheduler<>() {
          @Override
          public Checkpoint<$Timeline> now() {
            return builder.now();
          }

          @Override
          public <Event> void emit(final Event event, final Query<? super $Timeline, Event, ?> query) {
            changedTables[query.getTableIndex()] = true;
            builder.emit(event, query);
          }

          @Override
          public String spawn(final Task<$Timeline> task) {
            final var childInfo = taskFactory.createAnonymousTask(task, Optional.of(info.id));
            if (info.isDaemon) childInfo.isDaemon = true;
            info.children.add(childInfo.id);

            builder.signal(childInfo.id);
            return childInfo.id;
          }

          @Override
          public String spawn(final String type, final Map<String, SerializedValue> arguments) {
            final var childInfo = taskFactory.createTask(type, arguments, Optional.of(info.id));
            info.children.add(childInfo.id);

            builder.signal(childInfo.id);
            return childInfo.id;
          }

          @Override
          public String defer(final Duration delay, final Task<$Timeline> task) {
            final var childInfo = taskFactory.createAnonymousTask(task, Optional.of(info.id));
            if (info.isDaemon) childInfo.isDaemon = true;
            info.children.add(childInfo.id);

            queue.deferTo(queue.getElapsedTime().plus(delay), childInfo.id);
            return childInfo.id;
          }

          @Override
          public String defer(final Duration delay, final String type, final Map<String, SerializedValue> arguments) {
            final var childInfo = taskFactory.createTask(type, arguments, Optional.of(info.id));
            info.children.add(childInfo.id);

            queue.deferTo(queue.getElapsedTime().plus(delay), childInfo.id);
            return childInfo.id;
          }
        });

        status.match(new TaskStatus.Visitor<$Timeline, Void>() {
          @Override
          public Void completed() {
            var ancestorId$ = Optional.of(info.id);

            completeAncestors: while (ancestorId$.isPresent()) {
              final var ancestorInfo = taskFactory.get(ancestorId$.get());

              // If this task is still ongoing, it's definitely not complete.
              if (!ancestorInfo.isDone()) break;

              // Check if this task's children are all complete.
              for (final var childId : ancestorInfo.children) {
                if (!completedTasks.contains(taskFactory.get(childId).id)) {
                  // It's not yet "truly" complete.
                  break completeAncestors;
                }
              }

              // Mark this task as complete, and signal anybody waiting on it.
              completedTasks.add(ancestorInfo.id);

              final var conditionedActivities = waitingTasks.remove(ancestorInfo.id);
              if (conditionedActivities != null) {
                for (final var conditionedTask : conditionedActivities) {
                  queue.deferTo(queue.getElapsedTime(), conditionedTask);
                }
              }

              ancestorId$ = ancestorInfo.parent;
            }

            return null;
          }

          @Override
          public Void delayed(final Duration delay) {
            queue.deferTo(queue.getElapsedTime().plus(delay), info.id);
            return null;
          }

          @Override
          public Void awaiting(final String target) {
            if (completedTasks.contains(target)) {
              queue.deferTo(queue.getElapsedTime(), taskId);
            } else {
              waitingTasks.computeIfAbsent(target, k -> new HashSet<>()).add(taskId);
            }

            return null;
          }

          @Override
          public Void awaiting(final Condition<? super $Timeline> condition) {
            conditionedTasks.put(info.id, condition);
            return null;
          }
        });
      });

      for (final var profile : profiles.values()) {
        profile.extendBy(delta);

        // Only fetch a new dynamics if it could possibly have been changed by the tasks we just ran.
        for (final var dependency : profile.lastDependencies) {
          if (changedTables[dependency.getTableIndex()]) {
            profile.updateAt(yieldTime);
            break;
          }
        }
      }

      // Signal any tasks whose condition occurs soon.
      // TODO: Only check conditions which could have possibly been affected by the latest batch of tasks.
      //   This will require some rejigging, since we'll need to store alongside each condition
      //   its dependencies *and* its last nextSatisfied() result.
      {
        var maxBound = queue.getNextJobTime().orElse(simulationDuration);
        final var activatedTasks = new ArrayList<String>();

        for (final var entry : conditionedTasks.entrySet()) {
          final var taskId = entry.getKey();
          final var condition = entry.getValue();

          final var triggerTime$ = condition
              .nextSatisfied(
                  yieldTime::ask,
                  Window.between(Duration.ZERO, maxBound.minus(queue.getElapsedTime())))
              .map(queue.getElapsedTime()::plus);

          if (triggerTime$.isEmpty()) continue;
          final var triggerTime = triggerTime$.get();

          if (triggerTime.shorterThan(maxBound)) activatedTasks.clear();
          if (triggerTime.noLongerThan(maxBound)) activatedTasks.add(taskId);
          maxBound = triggerTime;
        }

        for (final var taskId : activatedTasks) {
          queue.deferTo(maxBound, taskId);
          conditionedTasks.remove(taskId);
        }
      }

      return yieldTime;
    });

    // Identify all sample times.
    final var timestamps = new ArrayList<Duration>();
    {
      timestamps.add(Duration.ZERO);
      timestamps.add(simulationDuration);
    }

    // Collect samples for all resources.
    final var resourceSamples = new HashMap<String, List<Pair<Duration, SerializedValue>>>();
    profiles.forEach((name, profile) -> {
      resourceSamples.put(name, SampleTaker.sample(profile));
    });

    // Use the map of task id to activity id to replace task ids with the corresponding
    // activity id for use by the front end.
    final var taskInfo = new HashMap<String, TaskInfo<?>>();
    taskFactory.forEach(entry -> {
      final var taskId = entry.getKey();
      final var info = entry.getValue();

      taskIdToActivityId.computeIfAbsent(taskId, $ -> UUID.randomUUID().toString());
      taskInfo.put(taskId, info);
    });

    return new SimulationResults(
        resourceSamples,
        taskIdToActivityId,
        taskInfo,
        startTime);
  }

  public static <$Schema> void simulateTask(
      final Adaptation<$Schema> adaptation,
      final Duration simulationDuration)
  throws TaskSpecInstantiationException
  {
    simulateTask(adaptation, SimulationTimeline.create(adaptation.getSchema()), simulationDuration);
  }

  private static <$Schema, $Timeline extends $Schema> void simulateTask(
      final Adaptation<$Schema> adaptation,
      final SimulationTimeline<$Timeline> database,
      final Duration simulationDuration)
  throws TaskSpecInstantiationException
  {
    final var taskFactory = new TaskFactory<$Schema, $Timeline>(adaptation.getTaskSpecificationTypes());

    // A time-ordered queue of all scheduled future tasks.
    final var queue = new TaskQueue<String>();
    // The set of all tasks that have completed.
    final var completedTasks = new HashSet<String>();
    // For each task, a set of tasks awaiting its completion (if any).
    final var waitingTasks = new HashMap<String, Set<String>>();
    // For each task, the condition blocking its progress (if any).
    final var conditionedTasks = new HashMap<String, Condition<? super $Timeline>>();

    {
      final var daemon = adaptation.<$Timeline>getDaemon();
      final var info = taskFactory.createAnonymousTask(daemon, Optional.empty());
      queue.deferTo(Duration.ZERO, info.id);
    }

    var now = database.origin();

    // Step the stimulus program forward until we reach the end of the simulation.
    final var changedTables = new boolean[database.getTableCount()];
    now = queue.consumeUpTo(simulationDuration, now, (delta, frame) -> {
      final var yieldTime = TaskFrame.runToCompletion(frame, (taskId, builder) -> {
        final var info = taskFactory.get(taskId);

        final var status = info.step(queue.getElapsedTime(), new Scheduler<>() {
          @Override
          public Checkpoint<$Timeline> now() {
            return builder.now();
          }

          @Override
          public <Event> void emit(final Event event, final Query<? super $Timeline, Event, ?> query) {
            changedTables[query.getTableIndex()] = true;
            builder.emit(event, query);
          }

          @Override
          public String spawn(final Task<$Timeline> task) {
            final var childInfo = taskFactory.createAnonymousTask(task, Optional.of(info.id));
            if (info.isDaemon) childInfo.isDaemon = true;
            info.children.add(childInfo.id);

            builder.signal(childInfo.id);
            return childInfo.id;
          }

          @Override
          public String spawn(final String type, final Map<String, SerializedValue> arguments) {
            final var childInfo = taskFactory.createTask(type, arguments, Optional.of(info.id));
            info.children.add(childInfo.id);

            builder.signal(childInfo.id);
            return childInfo.id;
          }

          @Override
          public String defer(final Duration delay, final Task<$Timeline> task) {
            final var childInfo = taskFactory.createAnonymousTask(task, Optional.of(info.id));
            if (info.isDaemon) childInfo.isDaemon = true;
            info.children.add(childInfo.id);

            queue.deferTo(queue.getElapsedTime().plus(delay), childInfo.id);
            return childInfo.id;
          }

          @Override
          public String defer(final Duration delay, final String type, final Map<String, SerializedValue> arguments) {
            final var childInfo = taskFactory.createTask(type, arguments, Optional.of(info.id));
            info.children.add(childInfo.id);

            queue.deferTo(queue.getElapsedTime().plus(delay), childInfo.id);
            return childInfo.id;
          }
        });

        status.match(new TaskStatus.Visitor<$Timeline, Void>() {
          @Override
          public Void completed() {
            var ancestorId$ = Optional.of(info.id);

            completeAncestors: while (ancestorId$.isPresent()) {
              final var ancestorInfo = taskFactory.get(ancestorId$.get());

              // If this task is still ongoing, it's definitely not complete.
              if (!ancestorInfo.isDone()) break;

              // Check if this task's children are all complete.
              for (final var childId : ancestorInfo.children) {
                if (!completedTasks.contains(taskFactory.get(childId).id)) {
                  // It's not yet "truly" complete.
                  break completeAncestors;
                }
              }

              // Mark this task as complete, and signal anybody waiting on it.
              completedTasks.add(ancestorInfo.id);

              final var conditionedActivities = waitingTasks.remove(ancestorInfo.id);
              if (conditionedActivities != null) {
                for (final var conditionedTask : conditionedActivities) {
                  queue.deferTo(queue.getElapsedTime(), conditionedTask);
                }
              }

              ancestorId$ = ancestorInfo.parent;
            }

            return null;
          }

          @Override
          public Void delayed(final Duration delay) {
            queue.deferTo(queue.getElapsedTime().plus(delay), info.id);
            return null;
          }

          @Override
          public Void awaiting(final String target) {
            if (completedTasks.contains(target)) {
              queue.deferTo(queue.getElapsedTime(), taskId);
            } else {
              waitingTasks.computeIfAbsent(target, k -> new HashSet<>()).add(taskId);
            }

            return null;
          }

          @Override
          public Void awaiting(final Condition<? super $Timeline> condition) {
            conditionedTasks.put(info.id, condition);
            return null;
          }
        });
      });

      // Signal any tasks whose condition occurs soon.
      // TODO: Only check conditions which could have possibly been affected by the latest batch of tasks.
      //   This will require some rejigging, since we'll need to store alongside each condition
      //   its dependencies *and* its last nextSatisfied() result.
      {
        var maxBound = queue.getNextJobTime().orElse(simulationDuration);
        final var activatedTasks = new ArrayList<String>();

        for (final var entry : conditionedTasks.entrySet()) {
          final var taskId = entry.getKey();
          final var condition = entry.getValue();

          final var triggerTime$ = condition
              .nextSatisfied(
                  yieldTime::ask,
                  Window.between(Duration.ZERO, maxBound.minus(queue.getElapsedTime())))
              .map(queue.getElapsedTime()::plus);

          if (triggerTime$.isEmpty()) continue;
          final var triggerTime = triggerTime$.get();

          if (triggerTime.shorterThan(maxBound)) activatedTasks.clear();
          if (triggerTime.noLongerThan(maxBound)) activatedTasks.add(taskId);
          maxBound = triggerTime;
        }

        for (final var taskId : activatedTasks) {
          queue.deferTo(maxBound, taskId);
          conditionedTasks.remove(taskId);
        }
      }

      return yieldTime;
    });
  }

  private static <$Schema, Resource>
  void
  createProfilesForFamily(
      final ResourceFamily<$Schema, Resource> family,
      final BiConsumer<String, ProfileBuilder<$Schema, ?, ?>> handler)
  {
    final var solver = family.getSolver();

    for (final var entry : family.getResources().entrySet()) {
      handler.accept(entry.getKey(), new ProfileBuilder<>(solver, entry.getValue()));
    }
  }

  public static class TaskSpecInstantiationException extends Exception {
    public final String id;

    public TaskSpecInstantiationException(final String id, final Throwable cause) {
      super(cause);
      this.id = id;
    }
  }

  public static class InstantiationException extends RuntimeException {
    public final String typeName;
    public final Map<String, SerializedValue> arguments;

    public InstantiationException(final String typeName, final Map<String, SerializedValue> arguments, final Throwable cause) {
      super(
          String.format("Could not instantiate task of type %s with arguments %s", typeName, arguments),
          cause);

      this.typeName = Objects.requireNonNull(typeName);
      this.arguments = Objects.requireNonNull(arguments);
    }
  }
}
