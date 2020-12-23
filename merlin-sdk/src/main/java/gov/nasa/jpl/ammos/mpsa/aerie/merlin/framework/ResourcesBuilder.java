package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.ModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ResourcesBuilder<$Schema> {
  private final Supplier<? extends Context<$Schema>> rootContext;
  private final Schema.Builder<$Schema> schemaBuilder;
  private ResourcesBuilderState<$Schema> state;

  public ResourcesBuilder(final Supplier<? extends Context<$Schema>> rootContext, final Schema.Builder<$Schema> schemaBuilder) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.schemaBuilder = Objects.requireNonNull(schemaBuilder);
    this.state = new UnbuiltResourcesBuilderState();
  }

  public Cursor<$Schema> getCursor() {
    return new Cursor<>(this, "");
  }

  public BuiltResources<$Schema> build() {
    return this.state.build(this.schemaBuilder.build());
  }


  public static final class Cursor<$Schema> {
    private final ResourcesBuilder<$Schema> builder;
    private final String namespace;

    private Cursor(final ResourcesBuilder<$Schema> builder, final String namespace) {
      this.builder = Objects.requireNonNull(builder);
      this.namespace = Objects.requireNonNull(namespace);
    }

    /*package-local*/
    Supplier<? extends Context<$Schema>> getRootContext() {
      return this.builder.rootContext;
    }

    public Cursor<$Schema> descend(final String namespace) {
      return new Cursor<>(this.builder, this.namespace + "/" + namespace);
    }

    public <Event, Effect, ModelType extends Model<Effect, ModelType>>
    Query<$Schema, Event, ModelType>
    model(final ModelType initialState, final Function<Event, Effect> interpreter)
    {
      return this.builder.schemaBuilder.register(
          Projection.from(initialState.effectTrait(), interpreter),
          new ModelApplicator<>(initialState));
    }

    public <Resource>
    DiscreteResource<$Schema, Resource>
    discrete(final String name,
             final Property<History<? extends $Schema>, Resource> property,
             final ValueMapper<Resource> mapper)
    {
      final var resource = DiscreteResource.atom(property);
      this.builder.state.discrete(this.namespace + "/" + name, resource, mapper);
      return resource;
    }

    public RealResource<$Schema>
    real(final String name,
         final Property<History<? extends $Schema>, RealDynamics> property)
    {
      final var resource = RealResource.atom(property);
      this.builder.state.real(this.namespace + "/" + name, resource);
      return resource;
    }

    public void constraint(final String id, final Condition<$Schema> condition) {
      this.builder.state.constraint(this.namespace + "/" + id, condition);
    }

    public void daemon(final String id, final Runnable task) {
      this.builder.state.daemon(this.namespace + "/" + id, task);
    }
  }


  private interface ResourcesBuilderState<$Schema> {
    <Resource>
    void
    discrete(String name,
             DiscreteResource<$Schema, Resource> resource,
             ValueMapper<Resource> mapper);

    void
    real(String name,
         RealResource<$Schema> resource);

    void
    constraint(String id,
               Condition<$Schema> condition);

    void
    daemon(String id,
           Runnable task);

    BuiltResources<$Schema>
    build(Schema<$Schema> schema);
  }

  private final class UnbuiltResourcesBuilderState implements ResourcesBuilderState<$Schema> {
    private final List<ResourceFamily<$Schema, ?, ?>> resourceFamilies = new ArrayList<>();
    private final Map<String, RealResource<$Schema>> realResources = new HashMap<>();
    private final Map<String, Condition<$Schema>> constraints = new HashMap<>();
    private final Map<String, Runnable> daemons = new HashMap<>();

    @Override
    public <Resource>
    void discrete(
        final String name,
        final DiscreteResource<$Schema, Resource> resource,
        final ValueMapper<Resource> mapper)
    {
      this.resourceFamilies.add(new DiscreteResourceFamily<>(mapper, Map.of(name, resource)));
    }

    @Override
    public void real(final String name, final RealResource<$Schema> resource) {
      this.realResources.put(name, resource);
    }

    @Override
    public void constraint(final String id, final Condition<$Schema> condition) {
      this.constraints.put(id, condition);
    }

    @Override
    public void daemon(final String id, final Runnable task) {
      this.daemons.put(id, task);
    }

    @Override
    public BuiltResources<$Schema> build(final Schema<$Schema> schema) {
      this.resourceFamilies.add(new RealResourceFamily<>(this.realResources));

      final var resources = new BuiltResources<>(schema, this.resourceFamilies, this.constraints, this.daemons);

      ResourcesBuilder.this.state = new BuiltResourcesBuilderState(resources);
      return resources;
    }
  }

  private final class BuiltResourcesBuilderState implements ResourcesBuilderState<$Schema> {
    private final BuiltResources<$Schema> resources;

    public BuiltResourcesBuilderState(final BuiltResources<$Schema> resources) {
      this.resources = resources;
    }

    @Override
    public <Resource>
    void discrete(
        final String name,
        final DiscreteResource<$Schema, Resource> resource,
        final ValueMapper<Resource> mapper)
    {
      throw new IllegalStateException("Resources cannot be added after the schema is built");
    }

    @Override
    public void real(final String name, final RealResource<$Schema> resource) {
      throw new IllegalStateException("Resources cannot be added after the schema is built");
    }

    @Override
    public void constraint(final String id, final Condition<$Schema> condition) {
      throw new IllegalStateException("Constraints cannot be added after the schema is built");
    }

    @Override
    public void daemon(final String id, final Runnable task) {
      throw new IllegalStateException("Daemons cannot be added after the schema is built");
    }

    @Override
    public BuiltResources<$Schema> build(final Schema<$Schema> schema) {
      return this.resources;
    }
  }
}
