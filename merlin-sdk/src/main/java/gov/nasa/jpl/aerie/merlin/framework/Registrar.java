package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

import java.util.Objects;

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
}
