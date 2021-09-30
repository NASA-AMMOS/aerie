package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.QueryContext;
import gov.nasa.jpl.aerie.merlin.framework.Resource;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.types.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;

import java.util.List;
import java.util.Objects;

public final class RealResourceSolver<$Schema>
    implements ResourceSolver<$Schema, Resource<RealDynamics>, RealDynamics>
{
  private final Scoped<Context> rootContext;

  public RealResourceSolver(final Scoped<Context> rootContext) {
    this.rootContext = Objects.requireNonNull(rootContext);
  }

  @Override
  public RealDynamics getDynamics(final Resource<RealDynamics> resource, final Querier<? extends $Schema> now) {
    try (final var restore = this.rootContext.set(new QueryContext<>(now))) {
      return resource.getDynamics();
    }
  }

  @Override
  public <Result> Result approximate(final ApproximatorVisitor<RealDynamics, Result> visitor) {
    return visitor.real(dynamics -> List.of(DelimitedDynamics.persistent(dynamics)));
  }
}
