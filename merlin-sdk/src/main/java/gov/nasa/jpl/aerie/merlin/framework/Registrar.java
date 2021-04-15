package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

import java.util.Objects;
import java.util.function.Consumer;

public final class Registrar {
  private final AdaptationBuilder<?> builder;

  public Registrar(final AdaptationBuilder<?> builder) {
    this.builder = Objects.requireNonNull(builder);
  }

  public boolean isInitializationComplete() {
    return this.builder.isBuilt();
  }

  public <State>
  DiscreteResource<State>
  resource(final String name, final DiscreteResource<State> resource, final ValueMapper<State> mapper) {
    this.builder.discrete(name, resource, mapper);
    return resource;
  }

  public
  RealResource
  resource(final String name, final RealResource resource) {
    this.builder.real(name, resource);
    return resource;
  }

  public <Activity> void threadedTask(final ActivityMapper<Activity> mapper, final Consumer<Activity> task) {
    this.builder.taskType(mapper, new AdaptationBuilder.TaskMaker<>() {
      @Override
      public <$Timeline> Task<$Timeline> make(final Activity activity) {
        return new ThreadedTask<>(ModelActions.context, () -> task.accept(activity));
      }
    });
  }

  public <Activity> void replayingTask(final ActivityMapper<Activity> mapper, final Consumer<Activity> task) {
    this.builder.taskType(mapper, new AdaptationBuilder.TaskMaker<>() {
      @Override
      public <$Timeline> Task<$Timeline> make(final Activity activity) {
        return new ReplayingTask<>(ModelActions.context, () -> task.accept(activity));
      }
    });
  }

  public <Activity> void noopTask(final ActivityMapper<Activity> mapper) {
    this.builder.taskType(mapper, new AdaptationBuilder.TaskMaker<>() {
      @Override
      public <$Timeline> Task<$Timeline> make(final Activity activity) {
        return $ -> TaskStatus.completed();
      }
    });
  }
}
