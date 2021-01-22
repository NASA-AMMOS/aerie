package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskRecord;
import gov.nasa.jpl.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.RealApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.SimulationTimeline;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SimulationDriver {
  public static <$Schema> SimulationResults simulate(
      final Adaptation<$Schema> adaptation,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration,
      final Duration samplingPeriod
  ) throws TaskSpecInstantiationException
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
      final Duration samplingPeriod
  ) throws TaskSpecInstantiationException
  {
    final var activityTypes = adaptation.getTaskSpecificationTypes();
    final var taskSpecs = new ArrayList<Triple<Duration, String, TaskSpec<$Schema, ?>>>();
    final var daemonSet = new HashSet<String>();

    for (final var daemon : adaptation.getDaemons()) {
      final var activityId = UUID.randomUUID().toString();
      try {
        taskSpecs.add(Triple.of(Duration.ZERO,
                                activityId,
                                TaskSpec.instantiate(activityTypes.get(daemon.getKey()), daemon.getValue())));
      } catch (final TaskSpecType.UnconstructableTaskSpecException e) {
        throw new TaskSpecInstantiationException(activityId, e);
      }
      daemonSet.add(activityId);
    }

    for (final var entry : schedule.entrySet()) {
      final var activityId = entry.getKey();
      final var startDelta = entry.getValue().getLeft();
      final var serializedInstance = entry.getValue().getRight();
      final var type = serializedInstance.getTypeName();
      final var arguments = serializedInstance.getParameters();

      try {
        taskSpecs.add(Triple.of(startDelta,
                                activityId,
                                TaskSpec.instantiate(activityTypes.get(type), arguments)));
      } catch (final TaskSpecType.UnconstructableTaskSpecException e) {
        throw new TaskSpecInstantiationException(activityId, e);
      }
    }

    final BiFunction<String, Map<String, SerializedValue>, Task<$Timeline>> createTask = (type, arguments) -> {
      final var taskSpecType = activityTypes.get(type);
      try {
        return TaskSpec.instantiate(taskSpecType, arguments).createTask();
      } catch (final TaskSpecType.UnconstructableTaskSpecException e) {
        throw new Error(String.format("Could not instantiate task of type %s with arguments %s",
                                      taskSpecType.getName(),
                                      arguments));
      }
    };

    final var simulator = new SimulationEngine<>(database.origin(), createTask);
    final var taskIdToActivityId = new HashMap<String, String>();
    for (final var entry : taskSpecs) {
      final var activityId = entry.getMiddle();
      final var startDelta = entry.getLeft();
      final var taskSpec = entry.getRight();

      final var taskId = taskSpec.enqueueTask(startDelta, simulator);
      taskIdToActivityId.put(taskId, activityId);
    }

    simulator.runFor(simulationDuration);

    // Collect profiles for all resources.
    final var profiles = new HashMap<String, Profile<?, ?>>();
    adaptation.getResourceFamilies().forEach(group -> {
      forEachProfile(database, simulator.getCurrentHistory(), group, profiles::put);
    });

    // Collect samples for all resources.
    final var timestamps = new ArrayList<Duration>();
    {
      var elapsedTime = Duration.ZERO;
      while (!elapsedTime.longerThan(simulationDuration)) {
        timestamps.add(elapsedTime);
        elapsedTime = elapsedTime.plus(samplingPeriod);
      }

      if (!simulationDuration.remainderOf(samplingPeriod).isZero()) {
        timestamps.add(simulationDuration);
      }
    }

    final var timelines = new HashMap<String, List<SerializedValue>>();
    profiles.forEach((name, profile) -> {
      timelines.put(name, SampleTaker.sample(profile, timestamps));
    });

    // Collect samples for all resources in which it can have different sampling time
    final var resourceSamplingPoints = new HashMap<String, TreeSet<Duration>>();
    profiles.forEach((name, profile) -> {
      var i = profile.iterator();
      var sampleTimes = new TreeSet<Duration>();
      while (i.hasNext()) {
        var win = i.next().getLeft();
        sampleTimes.add(win.start);
        sampleTimes.add(win.end);
      }
      sampleTimes.addAll(timestamps);
      resourceSamplingPoints.put(name, sampleTimes);
    });

    final var resourceSamples = new HashMap<String, List<Pair<Duration, SerializedValue>>>();
    profiles.forEach((name, profile) -> {
      final var sampleValues = new ArrayList<SerializedValue>();
      final var samplePoints = new ArrayList<Duration>(resourceSamplingPoints.get(name));
      sampleValues.addAll(SampleTaker.sample(profile, samplePoints));

      resourceSamples.put(name, constructPairList(samplePoints, sampleValues));
    });

    // Collect windows for all conditions.
    final var constraintViolations = new ArrayList<ConstraintViolation>();
    adaptation.getConstraints().forEach((id, condition) -> {
      final var windows = condition.interpret(
          new ConditionSolver<>(
              database,
              simulator.getCurrentHistory(),
              Window.between(Duration.ZERO, simulationDuration)));

      final var violableConstraint = new ViolableConstraint();
      violableConstraint.name = id;
      violableConstraint.id = id;
      violableConstraint.category = "None";
      violableConstraint.message = "None";
      constraintViolations.add(new ConstraintViolation(windows, violableConstraint));
    });

    // Use the map of task id to activity id to replace task ids with the corresponding
    // activity id for use by the front end.
    final var mappedTaskWindows = new HashMap<String, Window>();
    final var mappedTaskRecords = new HashMap<String, TaskRecord>();
    {
      final var taskRecords = simulator.getTaskRecords();
      final var taskWindows = simulator.getTaskWindows();

      // Generate activity ids for all tasks
      taskRecords.forEach((id, record) -> {
        taskIdToActivityId.computeIfAbsent(id, $ -> UUID.randomUUID().toString());
      });

      taskWindows.forEach((id, window) -> {
        mappedTaskWindows.put(taskIdToActivityId.get(id), window);
      });

      taskRecords.forEach((id, record) -> {
        final var activityId = taskIdToActivityId.get(id);
        final var mappedParentId = record.parentId.map(taskIdToActivityId::get);

        mappedTaskRecords.put(activityId, new TaskRecord(record.type, record.arguments, mappedParentId));
      });
    }

    final var results = new SimulationResults(
        timestamps,
        timelines,
        constraintViolations,
        mappedTaskRecords,
        mappedTaskWindows,
        startTime);

    if (!daemonSet.containsAll(results.unfinishedActivities.keySet())) {
      throw new Error("There should be no unfinished activities when simulating to completion.");
    }

    return results;
  }

  public static <$Timeline, Resource>
  void
  forEachProfile(
      final SimulationTimeline<$Timeline> database,
      final History<$Timeline> endTime,
      final ResourceFamily<? super $Timeline, Resource, ?> family,
      final BiConsumer<String, Profile<?, ?>> consumer)
  {
    final var solver = family.getSolver();
    final var resources = family.getResources();

    resources.forEach((name, resource) -> {
      consumer.accept(name, computeProfile(database, endTime, solver, resource));
    });
  }

  public static <S, T>
  List<Pair<S,T>>
  constructPairList(
      final List<S> a,
      final List<T> b)
  {
    final var pair = new ArrayList<Pair<S, T>>
        (Math.min(a.size(), b.size()));
    final var aIter = a.iterator();
    final var bIter = b.iterator();
    while (aIter.hasNext() && bIter.hasNext()) {
      pair.add(Pair.of(aIter.next(), bIter.next()));
    }
    return pair;
  }

  public static <$Timeline, Resource, Dynamics, Condition>
  Profile<Dynamics, Condition>
  computeProfile(
      final SimulationTimeline<$Timeline> database,
      final History<$Timeline> endTime,
      final ResourceSolver<? super $Timeline, Resource, Dynamics, Condition> solver,
      final Resource resource)
  {
    return database.accumulateUpTo(endTime, new Profile<>(solver), (profile, info) -> {
      final var history = info.getLeft();
      final var window = info.getRight();

      return profile.append(
          window.end.minus(window.start),
          solver.getDynamics(resource, history));
    });
  }

  private static final class TaskSpec<$Schema, Spec> {
    private final Spec spec;
    private final TaskSpecType<$Schema, Spec> specType;

    private TaskSpec(
        final Spec spec,
        final TaskSpecType<$Schema, Spec> specType)
    {
      this.spec = Objects.requireNonNull(spec);
      this.specType = Objects.requireNonNull(specType);
    }

    public static <$Schema, Spec>
    TaskSpec<$Schema, Spec> instantiate(
        final TaskSpecType<$Schema, Spec> specType,
        final Map<String, SerializedValue> arguments)
    throws TaskSpecType.UnconstructableTaskSpecException
    {
      return new TaskSpec<>(specType.instantiate(arguments), specType);
    }

    public String getTypeName() {
      return this.specType.getName();
    }

    public Map<String, SerializedValue> getArguments() {
      return this.specType.getArguments(this.spec);
    }

    public List<String> getValidationFailures() {
      return this.specType.getValidationFailures(this.spec);
    }

    public <$Timeline extends $Schema> Task<$Timeline> createTask() {
      return this.specType.createTask(this.spec);
    }

    public <$Timeline extends $Schema> String enqueueTask(
        final Duration delay,
        final SimulationEngine<$Timeline> engine)
    {
      return engine.defer(delay, this.spec, this.specType);
    }
  }

  public static class TaskSpecInstantiationException extends Exception {
    public final String id;

    public TaskSpecInstantiationException(final String id, final Throwable cause) {
      super(cause);
      this.id = id;
    }
  }
}
