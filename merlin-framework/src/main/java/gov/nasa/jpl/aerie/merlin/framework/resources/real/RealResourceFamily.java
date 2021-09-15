package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.Resource;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class RealResourceFamily<$Schema>
    implements ResourceFamily<$Schema, Resource<RealDynamics>>
{
  private final Scoped<Context> rootContext;
  private final Map<String, Resource<RealDynamics>> resources;

  public RealResourceFamily(final Scoped<Context> rootContext, final Map<String, Resource<RealDynamics>> resources) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.resources = Objects.requireNonNull(resources);
  }

  @Override
  public Map<String, Resource<RealDynamics>> getResources() {
    return Collections.unmodifiableMap(this.resources);
  }

  @Override
  public ResourceSolver<$Schema, Resource<RealDynamics>, RealDynamics> getSolver() {
    return new RealResourceSolver<>(this.rootContext);
  }
}
