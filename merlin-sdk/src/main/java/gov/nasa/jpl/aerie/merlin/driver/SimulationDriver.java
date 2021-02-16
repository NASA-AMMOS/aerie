package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskRecord;
import gov.nasa.jpl.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
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
import java.util.function.BiConsumer;
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

    final var simulator = new SimulationEngine<>(createTask);
    final var taskIdToActivityId = new HashMap<String, String>();
    for (final var entry : taskSpecs) {
      final var activityId = entry.getMiddle();
      final var startDelta = entry.getLeft();
      final var taskSpec = entry.getRight();

      final var taskId = taskSpec.enqueueTask(startDelta, simulator);
      taskIdToActivityId.put(taskId, activityId);
    }

    final var trace = new ArrayList<Pair<Duration, History<$Timeline>>>();
    {
      // The trace of transaction points.
      var now = database.origin();
      trace.add(Pair.of(Duration.ZERO, now));

      while (simulator.getNextJobTime().map(simulationDuration::noShorterThan).orElse(false)) {
        final var nextJobTime = simulator.getNextJobTime().orElseThrow();

        now = simulator.step(now);
        trace.add(Pair.of(nextJobTime, now));
      }
    }

    // Collect profiles for all resources.
    final var profiles = new HashMap<String, ProfileBuilder<$Schema, ?, ?, ?>>();
    for (final var family : adaptation.getResourceFamilies()) {
      createProfilesForFamily(family, profiles::put);
    }
    computeProfiles(trace, profiles.values());

    // Identify all sample times.
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

    // Collect samples for all resources.
    final var resourceSamples = new HashMap<String, List<Pair<Duration, SerializedValue>>>();
    profiles.forEach((name, profile) -> {
      resourceSamples.put(name, SampleTaker.sample(profile, timestamps));
    });

    // Collect windows for all conditions.
    final var constraintViolations = new ArrayList<ConstraintViolation>();
    adaptation.getConstraints().forEach((id, condition) -> {
      final var windows = condition.interpret(
          new ConditionSolver<>(
              trace,
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
        resourceSamples,
        constraintViolations,
        mappedTaskRecords,
        mappedTaskWindows,
        startTime);

    if (!daemonSet.containsAll(results.unfinishedActivities.keySet())) {
      throw new Error("There should be no unfinished activities when simulating to completion.");
    }

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

  public static <$Schema, $Timeline extends $Schema>
  void
  computeProfiles(
      final List<Pair<Duration, History<$Timeline>>> trace,
      final Iterable<ProfileBuilder<$Schema, ?, ?, ?>> foos)
  {
    final var iter = trace.iterator();
    History<$Timeline> lastCheckedTime;

    {
      final var info = iter.next();
      final var history = info.getRight();
      final var delta = info.getLeft();

      lastCheckedTime = history;

      for (final var foo : foos) {
        foo.extendBy(delta);
        foo.updateAt(history);
      }
    }

    while (iter.hasNext()) {
      final var info = iter.next();
      final var delta = info.getLeft();
      final var history = info.getRight();

      final var changedTables = history.getChangedTablesSince(lastCheckedTime);
      lastCheckedTime = history;

      for (final var foo : foos) {
        foo.extendBy(delta);
        for (final var dependency : foo.lastDependencies) {
          if (changedTables[dependency.getTableIndex()]) {
            foo.updateAt(history);
            break;
          }
        }
      }
    }
  }

  public static final class ProfileBuilder<$Schema, Resource, Dynamics, Condition> {
    public final ResourceSolver<$Schema, Resource, Dynamics, Condition> solver;
    public final Resource resource;
    public final List<Pair<Window, Dynamics>> pieces;
    public final Set<Query<? super $Schema, ?, ?>> lastDependencies;

    public ProfileBuilder(
        final ResourceSolver<$Schema, Resource, Dynamics, Condition> solver,
        final Resource resource)
    {
      this.solver = solver;
      this.resource = resource;
      this.pieces = new ArrayList<>();
      this.lastDependencies = new HashSet<>();
    }

    public void updateAt(final History<? extends $Schema> history) {
      final var start =
          (this.pieces.isEmpty())
              ? Duration.ZERO
              : this.pieces.get(this.pieces.size() - 1).getLeft().end;

      this.lastDependencies.clear();

      final var dynamics = solver.getDynamics(resource, new Checkpoint<>() {
        @Override
        public <Event, Model> Model ask(final Query<? super $Schema, Event, Model> query) {
          ProfileBuilder.this.lastDependencies.add(query);
          return history.ask(query);
        }
      });

      this.pieces.add(Pair.of(Window.at(start), dynamics));
    }

    public void extendBy(final Duration duration) {
      if (duration.isNegative()) throw new IllegalArgumentException("cannot extend by a negative duration");
      else if (duration.isZero()) return;

      if (this.pieces.isEmpty()) throw new IllegalStateException("cannot extend an empty profile");

      final var lastSegment = this.pieces.get(this.pieces.size() - 1);
      final var lastWindow = lastSegment.getLeft();
      final var dynamics = lastSegment.getRight();

      this.pieces.set(
          this.pieces.size() - 1,
          Pair.of(
              Window.between(lastWindow.start, lastWindow.end.plus(duration)),
              dynamics));
    }

    public List<Pair<Window, Dynamics>> build() {
      return Collections.unmodifiableList(this.pieces);
    }
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
