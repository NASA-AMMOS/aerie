package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SolvableDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.FooAdaptationFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


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
    final var activityTypes = adaptation.getTaskSpecificationTypes();

    final var taskSpecs = new ArrayList<TaskSpec<$Schema, ?>>();
    for (final var x : adaptation.getDaemons()) {
      taskSpecs.add(TaskSpec.instantiate(activityTypes.get(x.getKey()), x.getValue()));
    }
    taskSpecs.add(TaskSpec.instantiate(activityTypes.get("foo"), Map.of()));

    for (final var taskSpec : taskSpecs) {
      final var validationFailures = taskSpec.getValidationFailures();
      validationFailures.forEach(System.out::println);
    }

    bar(taskSpecs, adaptation, SimulationTimeline.create(adaptation.getSchema()));
  }

  private static <$Schema, $Timeline extends $Schema>
  void bar(
      final List<TaskSpec<$Schema, ?>> taskSpecs,
      final Adaptation<$Schema> adaptation,
      final SimulationTimeline<$Timeline> timeline)
  {
    final var simulator = new SimulationEngine<>(timeline.origin());

    for (final var taskSpec : taskSpecs) {
      System.out.println("Performing " + taskSpec.getTypeName()
                         + " with arguments " + taskSpec.getArguments());
      final var task = taskSpec.<$Timeline>createTask();

      simulator.defer(Duration.ZERO, task);
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
  }
}
