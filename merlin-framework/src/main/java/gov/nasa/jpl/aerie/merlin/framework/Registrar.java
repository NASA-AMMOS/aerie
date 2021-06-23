package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResourceFamily;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class Registrar {
  private final AdaptationFactory.Builder<?> builder;

  public Registrar(final AdaptationFactory.Builder<?> builder) {
    this.builder = Objects.requireNonNull(builder);
  }

  public boolean isInitializationComplete() {
    return this.builder.isBuilt();
  }

  public <Value>
  DiscreteResource<Value>
  discrete(final String name, final Resource<Value> resource, final ValueMapper<Value> mapper) {
    this.builder.resourceFamily(new DiscreteResourceFamily<>(
        ModelActions.context,
        mapper,
        Map.of(name, resource)));

    return resource::getDynamics;
  }

  public
  RealResource
  real(final String name, final Resource<RealDynamics> resource) {
    this.builder.resourceFamily(new RealResourceFamily<>(
        ModelActions.context,
        Map.of(name, resource)));

    return resource::getDynamics;
  }

  public <Activity> void threadedTask(final ActivityMapper<Activity> mapper, final Consumer<Activity> task) {
    this.threadedTaskHelper(this.builder, mapper, task);
  }

  public <Activity> void replayingTask(final ActivityMapper<Activity> mapper, final Consumer<Activity> task) {
    this.replayingTaskHelper(this.builder, mapper, task);
  }

  public <Activity> void noopTask(final ActivityMapper<Activity> mapper) {
    this.noopTaskHelper(this.builder, mapper);
  }

  private <$Schema, Activity>
  void threadedTaskHelper(
      final AdaptationFactory.Builder<$Schema> builder,
      final ActivityMapper<Activity> mapper,
      final Consumer<Activity> task
  ) {
    builder.taskSpecType(mapper.getName(), new ActivityType<>(mapper) {
      @Override
      public <$Timeline extends $Schema> Task<$Timeline> createTask(final Activity activity) {
        return new ThreadedTask<>(ModelActions.context, () -> task.accept(activity));
      }
    });
  }

  private <$Schema, Activity>
  void replayingTaskHelper(
      final AdaptationFactory.Builder<$Schema> builder,
      final ActivityMapper<Activity> mapper,
      final Consumer<Activity> task
  ) {
    builder.taskSpecType(mapper.getName(), new ActivityType<>(mapper) {
      @Override
      public <$Timeline extends $Schema> Task<$Timeline> createTask(final Activity activity) {
        return new ReplayingTask<>(ModelActions.context, () -> task.accept(activity));
      }
    });
  }

  private <$Schema, Activity>
  void noopTaskHelper(final AdaptationFactory.Builder<$Schema> builder, final ActivityMapper<Activity> mapper) {
    builder.taskSpecType(mapper.getName(), new ActivityType<>(mapper) {
      @Override
      public <$Timeline extends $Schema> Task<$Timeline> createTask(final Activity activity) {
        return new OneShotTask<>($ -> {});
      }
    });
  }
}
