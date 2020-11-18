package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.ModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class ResourcesBuilder<$Schema> {
  private final Schema.Builder<$Schema> schemaBuilder;
  private ResourcesBuilderState<$Schema> state;

  public ResourcesBuilder(final Schema.Builder<$Schema> schemaBuilder) {
    this.schemaBuilder = Objects.requireNonNull(schemaBuilder);
    this.state = new UnbuiltResourcesBuilderState();
  }

  public
  BuiltResources<$Schema>
  build() {
    return this.state.build(this.schemaBuilder.build());
  }

  public <Event, Effect, ModelType extends Model<Effect, ModelType>>
  Query<$Schema, Event, ModelType>
  model(final ModelType initialState, final Function<Event, Effect> interpreter)
  {
    return this.schemaBuilder.register(
        Projection.from(initialState.effectTrait(), interpreter),
        new ModelApplicator<>(initialState));
  }

  public <E extends Enum<E>>
  Resource<History<? extends $Schema>, E>
  enumerated(final String name,
             final Resource<History<? extends $Schema>, E> resource)
  {
    this.state.enumerated(name, resource);
    return resource;
  }

  public <ResourceType>
  Resource<History<? extends $Schema>, ResourceType>
  discrete(final String name,
           final Resource<History<? extends $Schema>, ResourceType> resource,
           final ValueMapper<ResourceType> mapper)
  {
    this.state.discrete(name, resource, mapper);
    return resource;
  }

  public
  Resource<History<? extends $Schema>, RealDynamics>
  real(final String name,
       final Resource<History<? extends $Schema>, RealDynamics> resource)
  {
    this.state.real(name, resource);
    return resource;
  }

  private interface ResourcesBuilderState<$Schema> {
    <E extends Enum<E>>
    void
    enumerated(String name,
               Resource<History<? extends $Schema>, E> resource);

    <ResourceType>
    void
    discrete(String name,
             Resource<History<? extends $Schema>, ResourceType> resource,
             ValueMapper<ResourceType> mapper);

    void
    real(String name,
         Resource<History<? extends $Schema>, RealDynamics> resource);

    BuiltResources<$Schema>
    build(Schema<$Schema> schema);
  }

  private final class UnbuiltResourcesBuilderState implements ResourcesBuilderState<$Schema> {
    private final Map<String, Resource<History<? extends $Schema>, RealDynamics>> realResources = new HashMap<>();
    private final Map<String, Pair<ValueSchema, Resource<History<? extends $Schema>, SerializedValue>>> discreteResources = new HashMap<>();
    private final Map<String, EnumResource<$Schema, ?>> enumResources = new HashMap<>();

    @Override
    public <E extends Enum<E>>
    void enumerated(final String name,
        final Resource<History<? extends $Schema>, E> resource)
    {
      this.enumResources.put(name, new EnumResource<>(resource));
    }

    @Override
    public <ResourceType>
    void discrete(
        final String name,
        final Resource<History<? extends $Schema>, ResourceType> resource,
        final ValueMapper<ResourceType> mapper)
    {
      this.discreteResources.put(name, Pair.of(
          mapper.getValueSchema(),
          (time) -> resource.getDynamics(time).map(mapper::serializeValue)));
    }

    @Override
    public void real(
        final String name,
        final Resource<History<? extends $Schema>, RealDynamics> resource)
    {
      this.realResources.put(name, resource);
    }

    @Override
    public BuiltResources<$Schema> build(final Schema<$Schema> schema) {
      for (final var entry : this.enumResources.entrySet()) {
        this.discreteResources.put(entry.getKey(), entry.getValue().getBinding(schema));
      }

      final var resources = new BuiltResources<>(schema, this.realResources, this.discreteResources);

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
    public <E extends Enum<E>> void enumerated(
        final String name,
        final Resource<History<? extends $Schema>, E> resource)
    {
      throw new IllegalStateException("Resources cannot be added after the schema is built");
    }

    @Override
    public <ResourceType> void discrete(
        final String name,
        final Resource<History<? extends $Schema>, ResourceType> resource,
        final ValueMapper<ResourceType> mapper)
    {
      throw new IllegalStateException("Resources cannot be added after the schema is built");
    }

    @Override
    public void real(
        final String name,
        final Resource<History<? extends $Schema>, RealDynamics> resource)
    {
      throw new IllegalStateException("Resources cannot be added after the schema is built");
    }

    @Override
    public BuiltResources<$Schema> build(final Schema<$Schema> schema) {
      return this.resources;
    }
  }

  private static final class EnumResource<$Schema, E extends Enum<E>> {
    private final Resource<History<? extends $Schema>, E> resource;

    public EnumResource(final Resource<History<? extends $Schema>, E> resource) {
      this.resource = resource;
    }

    public
    Pair<ValueSchema, Resource<History<? extends $Schema>, SerializedValue>>
    getBinding(final Schema<$Schema> schema) {
      // Get the initial value of this resource.
      // Yeah... yikes. Amazing that this works though.
      final var startTime = SimulationTimeline.create(schema).origin();
      final var initialValue = this.resource.getDynamics(startTime).getDynamics();

      // SAFETY: Enums are implicitly final, and the only way to define one is to use the `enum` syntax,
      //   so `Class<? extends Enum<E>> == Class<E>`.
      @SuppressWarnings("unchecked")
      final var enumClass = (Class<E>) initialValue.getClass();
      final var mapper = new EnumValueMapper<>(enumClass);

      return Pair.of(mapper.getValueSchema(), (time) -> resource.getDynamics(time).map(mapper::serializeValue));
    }
  }
}
