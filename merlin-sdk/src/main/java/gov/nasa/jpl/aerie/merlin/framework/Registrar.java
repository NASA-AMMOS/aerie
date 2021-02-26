package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;

import java.util.Objects;
import java.util.function.Supplier;

public final class Registrar {
  private final AdaptationBuilder<?> builder;

  public Registrar(final AdaptationBuilder<?> builder) {
    this.builder = Objects.requireNonNull(builder);
  }

  /*package-local*/
  Supplier<? extends Context> getRootContext() {
    return this.builder.getRootContext();
  }

  public boolean isInitializationComplete() {
    return this.builder.isBuilt();
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
    this.builder.discrete(name, resource, mapper);
    return resource;
  }

  public
  RealResource
  resource(final String name, final RealResource resource) {
    this.builder.real(name, resource);
    return resource;
  }

  public void daemon(final Runnable task) {
    this.builder.daemon(task);
  }
}
