package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;

import java.util.List;
import java.util.Optional;

public final class RealResourceSolver<$Schema>
    implements ResourceSolver<$Schema, RealResource, RealDynamics, RealCondition>
{
  @Override
  public RealDynamics getDynamics(
      final RealResource resource,
      final History<? extends $Schema> now)
  {
    return resource.getDynamicsAt(now);
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
