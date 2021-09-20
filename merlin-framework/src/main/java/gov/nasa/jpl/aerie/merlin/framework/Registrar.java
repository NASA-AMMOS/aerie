package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResourceFamily;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class Registrar {
  private final Initializer<?> builder;

  public Registrar(final Initializer<?> builder) {
    this.builder = Objects.requireNonNull(builder);
  }

  public boolean isInitializationComplete() {
    return (ModelActions.context.get().getContextType() != Context.ContextType.Initializing);
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
}
