package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.aerie.merlin.timeline.SimulationTimeline;

import java.util.function.Consumer;
import java.util.function.Function;

public class ModelTestFramework {
  private static <$Schema, $Timeline extends $Schema>
  void simulate(
      final BuiltAdaptation<$Schema> adaptation,
      final SimulationTimeline<$Timeline> timeline,
      final Runnable taskWrapper)
  {
    final var task = new ThreadedTask<$Timeline>(ModelActions.context, taskWrapper);

    try {
      SimulationDriver.simulateTask(adaptation, timeline, task);
    } catch (final SimulationDriver.TaskSpecInstantiationException ex) {
      throw new Error(ex);
    }
  }

  private static <$Schema>
  void simulate(final BuiltAdaptation<$Schema> adaptation, final Runnable taskWrapper) {
    simulate(adaptation, SimulationTimeline.create(adaptation.getSchema()), taskWrapper);
  }

  private static <M> void test(final AdaptationBuilder<?> builder, final Function<Registrar, M> makeModel, final Consumer<M> modelTask) {
    final var registrar = new Registrar(builder);
    final var model = makeModel.apply(registrar);

    final var taskRan = new Object() { boolean value = false; };
    final Runnable taskWrapper = () -> {
      modelTask.accept(model);
      taskRan.value = true;
    };

    simulate(builder.build(), taskWrapper);

    // Sanity check: assert simulation driver actually ran this task
    // TODO a separate engine test should eventually make this check obsolete.
    //  See: https://github.jpl.nasa.gov/Aerie/aerie/pull/695#discussion_r116172
    if (!taskRan.value) {
      throw new Error("Internal test framework misconfiguration: task was never invoked by simulation driver");
    }
  }

  /** Test with default adaptation builder. */
  public static <M> void test(final Function<Registrar, M> makeModel, final Consumer<M> task) {
    test(new AdaptationBuilder<>(Schema.builder()), makeModel, task);
  }

  /** Test with adaptation builder provided by adaptation factory. */
  public static <M> void test(
      final AdaptationFactory adaptationFactory,
      final SerializedValue configuration,
      final Function<Registrar, M> makeModel,
      final Consumer<M> task)
  {
    test(adaptationFactory.makeBuilder(Schema.builder(), configuration), makeModel, task);
  }
}
