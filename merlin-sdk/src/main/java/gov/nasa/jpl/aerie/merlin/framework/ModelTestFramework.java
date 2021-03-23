package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.aerie.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

public class ModelTestFramework {

  private static <M> void test(final AdaptationBuilder<?> builder, final Function<Registrar, M> makeModel, final Consumer<M> task) {
    final var registrar = new Registrar(builder);
    final var model = makeModel.apply(registrar);

    final var taskRan = new Object() { boolean value = false; };
    final Runnable taskWrapper = () -> {
      task.accept(model);
      taskRan.value = true;
    };

    builder.daemon(taskWrapper);

    try {
      SimulationDriver.simulateTask(builder.build(), Duration.SECOND);
    } catch (final SimulationDriver.TaskSpecInstantiationException e) {
      throw new Error(e);
    }

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
  public static <M> void test(final AdaptationFactory adaptationFactory, final Function<Registrar, M> makeModel, final Consumer<M> task) {
    test(adaptationFactory.makeBuilder(Schema.builder()), makeModel, task);
  }
}
