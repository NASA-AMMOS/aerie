package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskRecord;
import gov.nasa.jpl.aerie.merlin.protocol.Adaptation;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

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

    return simulate(adaptation, simulator, startTime, simulationDuration, samplingPeriod, taskIdToActivityId, daemonSet);
  }

  private static <$Schema, $Timeline extends $Schema> SimulationResults simulate(
      final Adaptation<$Schema> adaptation,
      final SimulationEngine<$Timeline> simulator,
      final Instant startTime,
      final Duration simulationDuration,
      final Duration samplingPeriod,
      final Map<String, String> taskIdToActivityId,
      final Set<String> daemonSet
  )
  {
    final var timestamps = new ArrayList<Duration>();
    final var timelines = new HashMap<String, List<SerializedValue>>();
    final var resourceTypes = adaptation.getResourceFamilies();

    resourceTypes.forEach(group -> {
      group.getResources().forEach((name, resource) -> timelines.put(name, new ArrayList<>()));
    });

    // Run simulation to completion, sampling states at periodic intervals.
    {
      // Get an initial sample.
      sampleResources(simulator, resourceTypes, timestamps, timelines);

      // Sample periodically until the sampling duration expires.
      final var remainder = simulationDuration.remainderOf(samplingPeriod);
      for (long i = 0; i < simulationDuration.dividedBy(samplingPeriod); ++i) {
        simulator.runFor(samplingPeriod);
        sampleResources(simulator, resourceTypes, timestamps, timelines);
      }

      // Take one last sample if the period doesn't evenly divide the duration.
      if (!remainder.isZero()) {
        simulator.runFor(simulationDuration.remainderOf(samplingPeriod));
        sampleResources(simulator, resourceTypes, timestamps, timelines);
      }
    }

    // TODO: implement constraint checking when we have a developed solution
    // for relating conditions, resources, and constraints in the driver. For
    // now we'll return an empty List.
    final var constraintViolations = Collections.<ConstraintViolation>emptyList();

    // Use the map of task id to activity id to replace task ids with the corresponding
    // activity id for use by the front end.
    final Map<String, Window> mappedTaskWindows;
    final Map<String, TaskRecord> mappedTaskRecords;
    {
      final var taskRecords = simulator.getTaskRecords();
      final var taskWindows = simulator.getTaskWindows();

      // Generate activity ids for all tasks
      taskRecords.forEach((id, record) -> taskIdToActivityId.computeIfAbsent(id, $ -> UUID.randomUUID().toString()));

      mappedTaskWindows = new HashMap<>();
      taskWindows.forEach((id, window) -> mappedTaskWindows.put(taskIdToActivityId.get(id), window));

      mappedTaskRecords = new HashMap<>();
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

    if (!results.unfinishedActivities.keySet().stream().allMatch(daemonSet::contains)) {
      throw new Error("There should be no unfinished activities when simulating to completion.");
    }

    return results;
  }

  private static <$Schema, $Timeline extends $Schema>
  void sampleResources(
      final SimulationEngine<$Timeline> simulator,
      final Iterable<ResourceFamily<$Schema, ?, ?>> resourceGroups,
      final ArrayList<Duration> timestamps,
      final HashMap<String, List<SerializedValue>> timelines)
  {
    timestamps.add(simulator.getElapsedTime());
    resourceGroups.forEach(group -> {
      sampleResourceType(simulator.getCurrentHistory(), timelines, group);
    });
  }

  private static <$Schema, Resource> void sampleResourceType(
      final History<? extends $Schema> now,
      final HashMap<String, List<SerializedValue>> timelines,
      final ResourceFamily<$Schema, Resource, ?> type)
  {
    final var solver = type.getSolver();

    type.getResources().forEach((name, resource) -> {
      timelines.get(name).add(sampleDynamics(solver, resource, now));
    });
  }

  private static <$Schema, $Timeline extends $Schema, Resource, Dynamics>
  SerializedValue sampleDynamics(
      final ResourceSolver<$Schema, Resource, Dynamics, ?> solver,
      final Resource resource,
      final History<$Timeline> now)
  {
    final var dynamics = solver.getDynamics(resource, now);

    return solver.approximate(new ResourceSolver.ApproximatorVisitor<>() {
      @Override
      public SerializedValue real(final RealApproximator<Dynamics> approximator) {
        final var part = approximator.approximate(dynamics.getDynamics()).iterator().next();
        return SerializedValue.of(part.getDynamics().initial);
      }

      @Override
      public SerializedValue discrete(final DiscreteApproximator<Dynamics> approximator) {
        final var part = approximator.approximate(dynamics.getDynamics()).iterator().next();
        return part.getDynamics();
      }
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
