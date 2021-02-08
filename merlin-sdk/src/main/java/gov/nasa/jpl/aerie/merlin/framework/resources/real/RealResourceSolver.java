package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.QueryContext;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;
import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RealResourceSolver<$Schema>
    implements ResourceSolver<$Schema, RealResource, RealDynamics, RealCondition>
{
  private final Scoped<Context<$Schema>> rootContext;

  public RealResourceSolver(final Scoped<Context<$Schema>> rootContext) {
    this.rootContext = Objects.requireNonNull(rootContext);
  }

  @Override
  public RealDynamics getDynamics(final RealResource resource, final Checkpoint<? extends $Schema> now) {
    return this.rootContext.setWithin(new QueryContext<>(now), resource::getDynamics);
  }

  @Override
  public <Result> Result approximate(final ApproximatorVisitor<RealDynamics, Result> visitor) {
    return visitor.real(dynamics -> List.of(DelimitedDynamics.persistent(dynamics)));
  }

  @Override
  public Optional<Duration>
  firstSatisfied(final RealDynamics dynamics, final RealCondition condition, final Window selection) {
    return dynamics.whenSatisfies(condition, selection);
  }

  @Override
  public Optional<Duration>
  firstDissatisfied(final RealDynamics dynamics, final RealCondition condition, final Window selection) {
    return dynamics.whenDissatisfies(condition, selection);
  }
}
