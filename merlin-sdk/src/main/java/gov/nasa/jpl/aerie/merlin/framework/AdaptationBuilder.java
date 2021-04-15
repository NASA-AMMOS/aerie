package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResourceFamily;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AdaptationBuilder<$Schema> {
  private final Schema.Builder<$Schema> schemaBuilder;

  private AdaptationBuilderState<$Schema> state = new UnbuiltState();

  public AdaptationBuilder(final Schema.Builder<$Schema> schemaBuilder) {
    this.schemaBuilder = Objects.requireNonNull(schemaBuilder);
  }

  public boolean isBuilt() {
    return state.isBuilt();
  }

  public <Event, Effect, CellType>
  Query<?, Event, CellType>
  allocate(final Projection<Event, Effect> projection, final Applicator<Effect, CellType> applicator) {
    return this.schemaBuilder.register(projection, applicator);
  }

  public <Resource>
  void
  discrete(final String name,
           final DiscreteResource<Resource> resource,
           final ValueMapper<Resource> mapper)
  {
    this.state.discrete(name, resource, mapper);
  }

  public void real(final String name, final RealResource resource) {
    this.state.real(name, resource);
  }

  public void daemon(final Runnable task) {
    this.state.daemon(task);
  }

  public <Activity> void taskType(final ActivityMapper<Activity> mapper, final TaskMaker<Activity> maker) {
    this.state.taskType(mapper.getName(), new ActivityType<>(mapper) {
      @Override
      public <$Timeline extends $Schema> Task<$Timeline> createTask(final Activity activity) {
        return maker.make(activity);
      }
    });
  }

  public BuiltAdaptation<$Schema> build() {
    return this.state.build(this.schemaBuilder.build());
  }


  public interface TaskMaker<Activity> {
    <$Timeline> Task<$Timeline> make(Activity activity);
  }


  private interface AdaptationBuilderState<$Schema> {
    boolean isBuilt();

    <Resource>
    void
    discrete(String name,
             DiscreteResource<Resource> resource,
             ValueMapper<Resource> mapper);

    void
    real(String name,
         RealResource resource);

    void
    daemon(Runnable task);

    void
    taskType(String id,
             TaskSpecType<$Schema, ?> taskSpecType);

    BuiltAdaptation<$Schema>
    build(Schema<$Schema> schema);
  }

  private final class UnbuiltState implements AdaptationBuilderState<$Schema> {
    private final List<ResourceFamily<$Schema, ?>> resourceFamilies = new ArrayList<>();
    private final List<Runnable> daemons = new ArrayList<>();
    private final Map<String, RealResource> realResources = new HashMap<>();
    private final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes = new HashMap<>();

    @Override
    public boolean isBuilt() {
      return false;
    }

    @Override
    public <Resource>
    void discrete(
        final String name,
        final DiscreteResource<Resource> resource,
        final ValueMapper<Resource> mapper)
    {
      this.resourceFamilies.add(new DiscreteResourceFamily<>(
          ModelActions.context,
          mapper,
          Map.of(name, resource)));
    }

    @Override
    public void real(final String name, final RealResource resource) {
      this.realResources.put(name, resource);
    }

    @Override
    public void daemon(final Runnable task) {
      this.daemons.add(task);
    }

    @Override
    public void taskType(final String id, final TaskSpecType<$Schema, ?> taskSpecType) {
      this.taskSpecTypes.put(id, taskSpecType);
    }

    @Override
    public BuiltAdaptation<$Schema> build(final Schema<$Schema> schema) {
      this.resourceFamilies.add(new RealResourceFamily<>(ModelActions.context, this.realResources));

      final var adaptation = new BuiltAdaptation<>(
          ModelActions.context,
          schema,
          this.resourceFamilies,
          this.daemons,
          this.taskSpecTypes);

      AdaptationBuilder.this.state = new BuiltState(adaptation);

      return adaptation;
    }
  }

  private final class BuiltState implements AdaptationBuilderState<$Schema> {
    private final BuiltAdaptation<$Schema> adaptation;

    public BuiltState(final BuiltAdaptation<$Schema> adaptation) {
      this.adaptation = adaptation;
    }

    @Override
    public boolean isBuilt() {
      return true;
    }

    @Override
    public <Resource>
    void discrete(
        final String name,
        final DiscreteResource<Resource> resource,
        final ValueMapper<Resource> mapper)
    {
      throw new IllegalStateException("Resources cannot be added after the schema is built");
    }

    @Override
    public void real(final String name, final RealResource resource) {
      throw new IllegalStateException("Resources cannot be added after the schema is built");
    }

    @Override
    public void daemon(final Runnable task) {
      throw new IllegalStateException("Daemons cannot be added after the schema is built");
    }

    @Override
    public void taskType(final String id, final TaskSpecType<$Schema, ?> taskSpecType) {
      throw new IllegalStateException("Activity types cannot be added after the schema is built");
    }

    @Override
    public BuiltAdaptation<$Schema> build(final Schema<$Schema> schema) {
      return this.adaptation;
    }
  }
}
