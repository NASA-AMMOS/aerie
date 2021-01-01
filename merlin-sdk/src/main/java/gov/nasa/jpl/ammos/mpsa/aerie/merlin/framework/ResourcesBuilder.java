package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.discrete.DiscreteResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.real.RealResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Projection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  public Registrar<$Schema> getRegistrar() {
    return new Registrar<>(this, "");
  }

  public Supplier<? extends Context<$Schema>> getRootContext() {
    return this.rootContext;
  }

  public <Event, Effect, ModelType>
  Query<$Schema, Event, ModelType>
  register(final Projection<Event, Effect> projection, final Applicator<Effect, ModelType> applicator) {
    return this.schemaBuilder.register(projection, applicator);
  }

  public <Resource>
  void
  discrete(final String name,
           final DiscreteResource<$Schema, Resource> resource,
           final ValueMapper<Resource> mapper)
  {
    this.state.discrete(name, resource, mapper);
  }

  public void real(final String name, final RealResource<$Schema> resource) {
    this.state.real(name, resource);
  }

  public void constraint(final String id, final Condition<$Schema> condition) {
    this.state.constraint(id, condition);
  }

  public void daemon(final String id, final Runnable task) {
    this.state.daemon(id, task);
  }

  public BuiltResources<$Schema> build() {
    return this.state.build(this.schemaBuilder.build());
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
