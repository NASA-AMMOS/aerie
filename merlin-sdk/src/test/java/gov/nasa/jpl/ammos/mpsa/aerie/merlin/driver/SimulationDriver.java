package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.FooAdaptationFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import org.apache.commons.lang3.tuple.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.MILLISECONDS;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.duration;


public final class SimulationDriver {
  public static void main(final String[] args) {
    try {
      foo(new FooAdaptationFactory().instantiate());
    } catch (final TaskSpecType.UnconstructableTaskSpecException ex) {
      ex.printStackTrace();
    }
  }

  private static <$Schema>
  void foo(final Adaptation<$Schema> adaptation)
  throws TaskSpecType.UnconstructableTaskSpecException
  {
    final List<Pair<Duration, SerializedActivity>> schedule = List.of(
        Pair.of(duration(0, MILLISECONDS),
                new SerializedActivity("foo", Map.of("x", SerializedValue.of(1), "y", SerializedValue.of("test_1")))),
        Pair.of(duration(50, MILLISECONDS),
                new SerializedActivity("foo", Map.of("x", SerializedValue.of(2), "y", SerializedValue.of("test_2")))),
        Pair.of(duration(100, MILLISECONDS),
                new SerializedActivity("foo", Map.of("x", SerializedValue.of(3), "y", SerializedValue.of("test_3"))))
    );

    bar(schedule, adaptation, SimulationTimeline.create(adaptation.getSchema()));
  }

  private static <$Schema, $Timeline extends $Schema>
  void bar(
      final List<Pair<Duration, SerializedActivity>> schedule,
      final Adaptation<$Schema> adaptation,
      final SimulationTimeline<$Timeline> timeline)
  throws TaskSpecType.UnconstructableTaskSpecException
  {
    final var activityTypes = adaptation.getTaskSpecificationTypes();
    final var taskSpecs = new ArrayList<Pair<Duration, TaskSpec<$Schema, ?>>>();

    for (final var x : adaptation.getDaemons()) {
      taskSpecs.add(Pair.of(Duration.ZERO, TaskSpec.instantiate(activityTypes.get(x.getKey()), x.getValue())));
    }

    for (final var entry : schedule) {
      final var startTime = entry.getLeft();
      final var type = entry.getRight().getTypeName();
      final var arguments = entry.getRight().getParameters();

      final var taskSpec = TaskSpec.instantiate(activityTypes.get(type), arguments);
      final var validationFailures = taskSpec.getValidationFailures();

      validationFailures.forEach(System.out::println);
      taskSpecs.add(Pair.of(startTime, taskSpec));
    }

    final var simulator = new SimulationEngine<>(timeline.origin());
    for (final var entry : taskSpecs) {
      final var startDelta = entry.getLeft();
      final var taskSpec = entry.getRight();

      taskSpec.enqueueTask(startDelta, simulator);
    }

    while (simulator.hasMoreTasks()) simulator.step();

    System.out.print(simulator.getCurrentHistory().getDebugTrace());
    adaptation.getRealResources().forEach((name, resource) -> {
      System.out.printf("%-12s%s%n", name, resource.getDynamics(simulator.getCurrentHistory()));
    });
    adaptation.getDiscreteResources().forEach((name, resource) -> {
      System.out.printf("%-12s%s%n", name, resource.getRight().getDynamics(simulator.getCurrentHistory()));
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

    public <$Timeline extends $Schema> void enqueueTask(final Duration delay, final SimulationEngine<$Timeline> engine) {
      engine.defer(delay, this.spec, this.specType);
    }
  }
}
