package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.QueryContext;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;

import java.util.List;
import java.util.Objects;

public final class RealResourceSolver<$Schema>
    implements ResourceSolver<$Schema, RealResource, RealDynamics>
{
  private final Scoped<Context> rootContext;

  public RealResourceSolver(final Scoped<Context> rootContext) {
    this.rootContext = Objects.requireNonNull(rootContext);
  }

  @Override
  public RealDynamics getDynamics(final RealResource resource, final Querier<? extends $Schema> now) {
    return this.rootContext.setWithin(new QueryContext<>(now), resource::getDynamics);
  }

  @Override
  public <Result> Result approximate(final ApproximatorVisitor<RealDynamics, Result> visitor) {
    return visitor.real(dynamics -> List.of(DelimitedDynamics.persistent(dynamics)));
  }
}
