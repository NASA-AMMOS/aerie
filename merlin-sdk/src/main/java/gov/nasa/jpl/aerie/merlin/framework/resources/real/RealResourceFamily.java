package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class RealResourceFamily<$Schema>
    implements ResourceFamily<$Schema, RealResource>
{
  private final Scoped<Context> rootContext;
  private final Map<String, RealResource> resources;

  public RealResourceFamily(final Scoped<Context> rootContext, final Map<String, RealResource> resources) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.resources = Objects.requireNonNull(resources);
  }

  @Override
  public Map<String, RealResource> getResources() {
    return Collections.unmodifiableMap(this.resources);
  }

  @Override
  public ResourceSolver<$Schema, RealResource, RealDynamics> getSolver() {
    return new RealResourceSolver<>(this.rootContext);
  }
}
