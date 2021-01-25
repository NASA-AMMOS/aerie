package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;

import java.util.Objects;
import java.util.function.Supplier;

public final class Registrar {
  private final AdaptationBuilder<?> builder;
  private final String namespace;

  public Registrar(final AdaptationBuilder<?> builder, final String namespace) {
    this.builder = Objects.requireNonNull(builder);
    this.namespace = Objects.requireNonNull(namespace);
  }

  /*package-local*/
  Supplier<? extends Context<?>> getRootContext() {
    return this.builder.getRootContext();
  }

  public boolean isInitializationComplete() {
    return builder.isBuilt();
  }

  public Registrar descend(final String namespace) {
    return new Registrar(this.builder, this.namespace + "/" + namespace);
  }

  public <Effect, CellType extends Cell<Effect, CellType>>
  CellRef<Effect, CellType>
  cell(final CellType initialState)
  {
    return this.builder.register(
        Projection.from(initialState.effectTrait(), ev -> ev),
        new CellApplicator<>(initialState));
  }

  public <State>
  DiscreteResource<State>
  resource(final String name, final DiscreteResource<State> resource, final ValueMapper<State> mapper) {
    this.builder.discrete(this.namespace + "/" + name, resource, mapper);
    return resource;
  }

  public
  RealResource
  resource(final String name, final RealResource resource) {
    this.builder.real(this.namespace + "/" + name, resource);
    return resource;
  }

  public void daemon(final String id, final Runnable task) {
    this.builder.daemon("/daemons" + this.namespace + "/" + id, task);
  }
}
