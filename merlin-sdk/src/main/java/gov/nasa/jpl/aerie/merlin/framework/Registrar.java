package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Registrar<$Schema> {
  private final AdaptationBuilder<$Schema> builder;
  private final Scoped<Context<$Schema>> rootContext;
  private final String namespace;

  /*package-local*/
  Registrar(
      final AdaptationBuilder<$Schema> builder,
      final Scoped<Context<$Schema>> rootContext,
      final String namespace)
  {
    this.builder = Objects.requireNonNull(builder);
    this.rootContext = rootContext;
    this.namespace = Objects.requireNonNull(namespace);
  }

  /*package-local*/
  Supplier<? extends Context<$Schema>> getRootContext() {
    return this.builder.getRootContext();
  }

  public Registrar<$Schema> descend(final String namespace) {
    return new Registrar<>(this.builder, this.rootContext, this.namespace + "/" + namespace);
  }

  public <Event, Effect, CellType extends Cell<Effect, CellType>>
  Query<$Schema, Event, CellType>
  cell(final CellType initialState, final Function<Event, Effect> interpreter)
  {
    return this.builder.register(
        Projection.from(initialState.effectTrait(), interpreter),
        new CellApplicator<>(initialState));
  }

  public <Resource>
  DiscreteResource<$Schema, Resource>
  discrete(
      final String name,
      final Property<History<? extends $Schema>, Resource> property,
      final ValueMapper<Resource> mapper)
  {
    final var resource = DiscreteResource.atom(property);
    this.builder.discrete(this.namespace + "/" + name, resource, mapper);
    return resource;
  }

  public
  RealResource<$Schema>
  real(final String name, final Property<History<? extends $Schema>, RealDynamics> property) {
    final var resource = RealResource.atom(property);
    this.builder.real(this.namespace + "/" + name, resource);
    return resource;
  }

  public void constraint(final String id, final Condition<$Schema> condition) {
    this.builder.constraint(this.namespace + "/" + id, condition);
  }

  public void daemon(final String id, final Runnable task) {
    this.builder.daemon("/daemons" + this.namespace + "/" + id, task);
  }
}
