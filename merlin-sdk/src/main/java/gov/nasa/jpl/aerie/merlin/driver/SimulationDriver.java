package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationKernel;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskFactory;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskFrame;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskInfo;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskRecord;
import gov.nasa.jpl.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.SimulationTimeline;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class SimulationDriver {
  public static <$Schema> SimulationResults simulate(
      final Adaptation<$Schema> adaptation,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration,
      final Duration samplingPeriod)
  throws TaskSpecInstantiationException
  {
    return simulate(adaptation, SimulationTimeline.create(adaptation.getSchema()), schedule, startTime, simulationDuration, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <$Schema, $Timeline extends $Schema> SimulationResults simulate(
      final Adaptation<$Schema> adaptation,
      final SimulationTimeline<$Timeline> database,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration,
      final Duration samplingPeriod)
  throws TaskSpecInstantiationException
  {
    final var taskIdToActivityId = new HashMap<String, String>();
    final var taskFactory = new TaskFactory<$Schema, $Timeline>(adaptation.getTaskSpecificationTypes());
    final var kernel = new SimulationKernel();

    for (final var daemon : adaptation.getDaemons()) {
      final var activityId = UUID.randomUUID().toString();

      final var info = taskFactory.createTask(daemon.getKey(), daemon.getValue(), Optional.empty());
      info.isDaemon = true;

      taskIdToActivityId.put(info.id, activityId);
      kernel.delay(info.id, Duration.ZERO);
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
      kernel.delay(info.id, startDelta);
    }

    // Collect profiles for all resources.
    final var profiles = new HashMap<String, ProfileBuilder<$Schema, ?, ?, ?>>();
    for (final var family : adaptation.getResourceFamilies()) createProfilesForFamily(family, profiles::put);

    var now = database.origin();
    for (final var profile : profiles.values()) profile.updateAt(now);

    // Step the stimulus program forward until we reach the end of the simulation.
    final var changedTables = new boolean[database.getTableCount()];
    now = kernel.consumeUpTo(simulationDuration, now, (delta, frame) -> {
      Arrays.fill(changedTables, false);

      final var yieldTime = TaskFrame.runToCompletion(frame, (taskId, builder) -> {
        final var info = taskFactory.get(taskId);

        final var status = info.step(kernel.getElapsedTime(), new Scheduler<>() {
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
          public String spawn(final String type, final Map<String, SerializedValue> arguments) {
            final var childInfo = taskFactory.createTask(type, arguments, Optional.of(info.id));
            builder.signal(childInfo.id);
            return childInfo.id;
          }

          @Override
          public String defer(final Duration delay, final String type, final Map<String, SerializedValue> arguments) {
            final var childInfo = taskFactory.createTask(type, arguments, Optional.of(info.id));
            kernel.delay(childInfo.id, delay);
            return childInfo.id;
          }
        });

        kernel.updateByStatus(info.id, status);
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

      // TODO: Check if any conditioned tasks should be signalled.

      return yieldTime;
    });

    // Identify all sample times.
    final var timestamps = new ArrayList<Duration>();
    {
      var elapsedTime = Duration.ZERO;
      while (elapsedTime.shorterThan(simulationDuration)) {
        timestamps.add(elapsedTime);
        elapsedTime = elapsedTime.plus(samplingPeriod);
      }
      timestamps.add(simulationDuration);
    }

    // Collect samples for all resources.
    final var resourceSamples = new HashMap<String, List<Pair<Duration, SerializedValue>>>();
    profiles.forEach((name, profile) -> {
      resourceSamples.put(name, SampleTaker.sample(profile, timestamps));
    });

    // Use the map of task id to activity id to replace task ids with the corresponding
    // activity id for use by the front end.
    final var mappedTaskWindows = new HashMap<String, Window>();
    final var mappedTaskRecords = new HashMap<String, TaskRecord>();
    taskFactory.forEach(entry -> {
      final var taskId = entry.getKey();
      taskIdToActivityId.computeIfAbsent(taskId, $ -> UUID.randomUUID().toString());
    });
    taskFactory.forEach(entry -> {
      final var taskId = entry.getKey();
      final var info = entry.getValue();

      final var activityId = taskIdToActivityId.get(taskId);
      final var mappedParentId = info.parent.map(taskIdToActivityId::get);

      info.getWindow().ifPresent($ -> mappedTaskWindows.put(activityId, $));
      mappedTaskRecords.put(activityId, new TaskRecord(info.typeName, info.arguments, mappedParentId, info.isDaemon));
    });

    final var results = new SimulationResults(
        resourceSamples,
        new ArrayList<>(),
        mappedTaskRecords,
        mappedTaskWindows,
        startTime);

    return results;
  }

  private static <$Schema, Resource, Condition>
  void
  createProfilesForFamily(
      final ResourceFamily<$Schema, Resource, Condition> family,
      final BiConsumer<String, ProfileBuilder<$Schema, ?, ?, ?>> handler)
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
