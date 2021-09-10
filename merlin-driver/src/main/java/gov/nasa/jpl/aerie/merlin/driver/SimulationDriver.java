package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.TaskFactory;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskFrame;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskInfo;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskQueue;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Checkpoint;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.RealApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.SimulationTimeline;
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
      final Adaptation<$Schema, ?> adaptation,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration)
  throws TaskSpecInstantiationException
  {
    return simulate(adaptation, SimulationTimeline.create(adaptation.getSchema()), schedule, startTime, simulationDuration);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <$Schema, $Timeline extends $Schema, Model> SimulationResults simulate(
      final Adaptation<$Schema, Model> adaptation,
      final SimulationTimeline<$Timeline> database,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration)
  throws TaskSpecInstantiationException
  {
    final var taskIdToActivityId = new HashMap<String, String>();
    final var taskFactory = new TaskFactory<$Timeline, Model>(adaptation.getTaskSpecificationTypes());

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
        info = taskFactory.createTask(adaptation.getModel(), activity.getTypeName(), activity.getParameters(), Optional.empty());
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
    for (final var profile : profiles.values()) profile.updateAt(adaptation, now);

    // Step the stimulus program forward until we reach the end of the simulation.
    final var checkpoints = new HashMap<Checkpoint<$Timeline>, History<$Timeline>>();
    final var changedTables = new boolean[database.getTableCount()];
    now = queue.consumeUpTo(simulationDuration, now, (delta, frame) -> {
      Arrays.fill(changedTables, false);

      final var yieldTime = TaskFrame.runToCompletion(frame, (taskId, builder) -> {
        final var info = taskFactory.get(taskId);

        final var status = info.step(queue.getElapsedTime(), new Scheduler<>() {
          @Override
          public Checkpoint<$Timeline> now() {
            final var checkpoint = new Checkpoint<$Timeline>() {};
            checkpoints.put(checkpoint, builder.now());

            return checkpoint;
          }

          @Override
          public <State> State getStateAt(
              final Checkpoint<$Timeline> checkpoint,
              final Query<? super $Timeline, ?, State> token)
          {
            final var query = adaptation
                .getQuery(token.specialize())
                .orElseThrow(() -> new IllegalArgumentException("forged token"));

            final var time = Optional
                .ofNullable(checkpoints.get(checkpoint))
                .orElseThrow(() -> new IllegalArgumentException("forged token"));

            return time.ask(query);
          }

          @Override
          public <Event, State> void emit(
              final Event event,
              final Query<? super $Timeline, ? super Event, State> token)
          {
            final var query = adaptation
                .getQuery(token.specialize())
                .orElseThrow(() -> new IllegalArgumentException("forged token"));

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
            final var childInfo = taskFactory.createTask(adaptation.getModel(), type, arguments, Optional.of(info.id));
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
            final var childInfo = taskFactory.createTask(adaptation.getModel(), type, arguments, Optional.of(info.id));
            info.children.add(childInfo.id);

            queue.deferTo(queue.getElapsedTime().plus(delay), childInfo.id);
            return childInfo.id;
          }
        });

        if (status instanceof TaskStatus.Completed) {
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
        } else if (status instanceof TaskStatus.Delayed<?> s) {
          queue.deferTo(queue.getElapsedTime().plus(s.delay()), info.id);
        } else if (status instanceof TaskStatus.AwaitingTask<?> s) {
          if (completedTasks.contains(s.target())) {
            queue.deferTo(queue.getElapsedTime(), taskId);
          } else {
            waitingTasks.computeIfAbsent(s.target(), k -> new HashSet<>()).add(taskId);
          }
        } else if (status instanceof TaskStatus.AwaitingCondition<$Timeline> s) {
          conditionedTasks.put(info.id, s.condition());
        } else {
          throw new IllegalStateException("Unknown subclass of %s: %s".formatted(TaskStatus.class, status));
        }
      });

      for (final var profile : profiles.values()) {
        profile.extendBy(delta);

        // Only fetch a new dynamics if it could possibly have been changed by the tasks we just ran.
        for (final var dependency : profile.lastDependencies) {
          if (changedTables[dependency.getTableIndex()]) {
            profile.updateAt(adaptation, yieldTime);
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

          final var querier = new Querier<$Timeline>() {
            @Override
            public <State> State getState(final Query<? super $Timeline, ?, State> token) {
              final var query = adaptation
                  .getQuery(token.specialize())
                  .orElseThrow(() -> new IllegalArgumentException("forged token"));

              return yieldTime.ask(query);
            }
          };

          final var triggerTime$ = condition
              .nextSatisfied(querier, maxBound.minus(queue.getElapsedTime()))
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

    // Flush the job queue, terminating any tasks that are incomplete.
    waitingTasks.keySet().forEach((taskId) -> taskFactory.get(taskId).abort());
    conditionedTasks.keySet().forEach((taskId) -> taskFactory.get(taskId).abort());
    now = queue.consumeUpTo(Duration.MAX_VALUE, now, (delta, frame) -> {
      return TaskFrame.runToCompletion(frame, (taskId, builder) -> {
        taskFactory.get(taskId).abort();
      });
    });

    final var realProfiles = new HashMap<String, List<Pair<Duration, RealDynamics>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>();
    profiles.forEach(new BiConsumer<String, ProfileBuilder<?, ?, ?>>() {
      @Override
      public void accept(final String name, final ProfileBuilder<?, ?, ?> profileBuilder) {
        acceptHelper(name, profileBuilder);
      }

      <Dynamics> void acceptHelper(final String name, final ProfileBuilder<?, ?, Dynamics> profile) {
        final var solver = profile.solver;
        solver.approximate(new ResourceSolver.ApproximatorVisitor<Dynamics, Void>() {
          @Override
          public Void real(final RealApproximator<Dynamics> approximator) {
            final var realProfile = new ArrayList<Pair<Duration, RealDynamics>>();

            var dynamicsStart = Duration.ZERO;
            final var dynamicsIter = profile.build().iterator();
            while (dynamicsIter.hasNext()) {
              final var entry = dynamicsIter.next();

              final var dynamicsDuration = entry.getLeft();
              final var dynamics = entry.getRight();
              final var dynamicsOwnsEndpoint = !dynamicsIter.hasNext();

              final var approximation = approximator.approximate(dynamics).iterator();

              var partStart = Duration.ZERO;
              do {
                final var part = approximation.next();
                final var partExtent = Duration.min(part.extent, dynamicsDuration);

                realProfile.add(Pair.of(partExtent, part.dynamics));
                partStart = partStart.plus(partExtent);
              } while (approximation.hasNext() && (partStart.shorterThan(dynamicsDuration) || dynamicsOwnsEndpoint));

              dynamicsStart = dynamicsStart.plus(dynamicsDuration);
            }

            realProfiles.put(name, realProfile);

            return null;
          }

          @Override
          public Void discrete(final DiscreteApproximator<Dynamics> approximator) {
            final var discreteProfile = new ArrayList<Pair<Duration, SerializedValue>>();

            var dynamicsStart = Duration.ZERO;
            final var dynamicsIter = profile.build().iterator();
            while (dynamicsIter.hasNext()) {
              final var entry = dynamicsIter.next();

              final var dynamicsDuration = entry.getLeft();
              final var dynamics = entry.getRight();
              final var dynamicsOwnsEndpoint = !dynamicsIter.hasNext();

              final var approximation = approximator.approximate(dynamics).iterator();

              var partStart = Duration.ZERO;
              do {
                final var part = approximation.next();
                final var partExtent = Duration.min(part.extent, dynamicsDuration);

                discreteProfile.add(Pair.of(partExtent, part.dynamics));
                partStart = partStart.plus(partExtent);
              } while (approximation.hasNext() && (partStart.shorterThan(dynamicsDuration) || dynamicsOwnsEndpoint));

              dynamicsStart = dynamicsStart.plus(dynamicsDuration);
            }

            discreteProfiles.put(name, Pair.of(approximator.getSchema(), discreteProfile));

            return null;
          }
        });
      }
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

    return SimulationResults.create(
        realProfiles,
        discreteProfiles,
        taskIdToActivityId,
        taskInfo,
        startTime);
  }

  public static <$Schema, $Timeline extends $Schema, Model> void simulateTask(
      final Adaptation<$Schema, Model> adaptation,
      final SimulationTimeline<$Timeline> database,
      final Task<$Timeline> task)
  throws TaskSpecInstantiationException
  {
    final var taskFactory = new TaskFactory<$Timeline, Model>(adaptation.getTaskSpecificationTypes());

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

    final var topTaskInfo = taskFactory.createAnonymousTask(task, Optional.empty());
    queue.deferTo(Duration.ZERO, topTaskInfo.id);

    var now = database.origin();

    // Step the stimulus program forward until the task ends.
    final var checkpoints = new HashMap<Checkpoint<$Timeline>, History<$Timeline>>();
    final var changedTables = new boolean[database.getTableCount()];
    while (!completedTasks.contains(topTaskInfo.id)) {
      final var frame$ = queue.popNextFrame(now, Duration.MAX_VALUE);
      if (frame$.isEmpty()) break;

      final var frame = frame$.get();

      final var yieldTime = TaskFrame.runToCompletion(frame, (taskId, builder) -> {
        final var info = taskFactory.get(taskId);

        final var status = info.step(queue.getElapsedTime(), new Scheduler<>() {
          @Override
          public Checkpoint<$Timeline> now() {
            final var checkpoint = new Checkpoint<$Timeline>() {};
            checkpoints.put(checkpoint, builder.now());

            return checkpoint;
          }

          @Override
          public <State> State getStateAt(
              final Checkpoint<$Timeline> checkpoint,
              final Query<? super $Timeline, ?, State> token)
          {
            final var query = adaptation
                .getQuery(token.specialize())
                .orElseThrow(() -> new IllegalArgumentException("forged token"));

            final var time = Optional
                .ofNullable(checkpoints.get(checkpoint))
                .orElseThrow(() -> new IllegalArgumentException("forged token"));

            return time.ask(query);
          }

          @Override
          public <Event, State> void emit(
              final Event event,
              final Query<? super $Timeline, ? super Event, State> token)
          {
            final var query = adaptation
                .getQuery(token.specialize())
                .orElseThrow(() -> new IllegalArgumentException("forged token"));

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
            final var childInfo = taskFactory.createTask(adaptation.getModel(), type, arguments, Optional.of(info.id));
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
            final var childInfo = taskFactory.createTask(adaptation.getModel(), type, arguments, Optional.of(info.id));
            info.children.add(childInfo.id);

            queue.deferTo(queue.getElapsedTime().plus(delay), childInfo.id);
            return childInfo.id;
          }
        });

        if (status instanceof TaskStatus.Completed) {
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
        } else if (status instanceof TaskStatus.Delayed<?> s) {
          queue.deferTo(queue.getElapsedTime().plus(s.delay()), info.id);
        } else if (status instanceof TaskStatus.AwaitingTask<?> s) {
          if (completedTasks.contains(s.target())) {
            queue.deferTo(queue.getElapsedTime(), taskId);
          } else {
            waitingTasks.computeIfAbsent(s.target(), k -> new HashSet<>()).add(taskId);
          }
        } else if (status instanceof TaskStatus.AwaitingCondition<$Timeline> s) {
          conditionedTasks.put(info.id, s.condition());
        } else {
          throw new IllegalStateException("Unknown subclass of %s: %s".formatted(TaskStatus.class, status));
        }
      });

      // Signal any tasks whose condition occurs soon.
      // TODO: Only check conditions which could have possibly been affected by the latest batch of tasks.
      //   This will require some rejigging, since we'll need to store alongside each condition
      //   its dependencies *and* its last nextSatisfied() result.
      {
        var maxBound = queue.getNextJobTime().orElse(Duration.MAX_VALUE);
        final var activatedTasks = new ArrayList<String>();

        for (final var entry : conditionedTasks.entrySet()) {
          final var taskId = entry.getKey();
          final var condition = entry.getValue();

          final var querier = new Querier<$Timeline>() {
            @Override
            public <State> State getState(final Query<? super $Timeline, ?, State> token) {
              final var query = adaptation
                  .getQuery(token.specialize())
                  .orElseThrow(() -> new IllegalArgumentException("forged token"));

              return yieldTime.ask(query);
            }
          };

          final var triggerTime$ = condition
              .nextSatisfied(querier, maxBound.minus(queue.getElapsedTime()))
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

      now = yieldTime;
    }

    // Flush the job queue, terminating any tasks that are incomplete.
    waitingTasks.keySet().forEach((taskId) -> taskFactory.get(taskId).abort());
    conditionedTasks.keySet().forEach((taskId) -> taskFactory.get(taskId).abort());
    now = queue.consumeUpTo(Duration.MAX_VALUE, now, (delta, frame) -> {
      return TaskFrame.runToCompletion(frame, (taskId, builder) -> {
        taskFactory.get(taskId).abort();
      });
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
