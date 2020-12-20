package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.ClosedInterval;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Objects;

public abstract class RealResource<$Schema> {
  private RealResource() {}

  public abstract DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now);

  public static <$Schema>
  RealResource<$Schema>
  atom(final Property<History<? extends $Schema>, RealDynamics> property) {
    Objects.requireNonNull(property);

    return new RealResource<>() {
      @Override
      public DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now) {
        return property.ask(now);
      }
    };
  }

  public static <$Schema>
  RealResource<$Schema>
  scaleBy(final double scalar, final RealResource<$Schema> resource) {
    Objects.requireNonNull(resource);

    return new RealResource<>() {
      @Override
      public DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now) {
        return resource.getDynamics(now).map($ -> $.scaledBy(scalar));
      }
    };
  }

  public static <$Schema>
  RealResource<$Schema>
  add(final RealResource<$Schema> left, final RealResource<$Schema> right) {
    Objects.requireNonNull(left);
    Objects.requireNonNull(right);

    return new RealResource<>() {
      @Override
      public DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now) {
        return left.getDynamics(now).parWith(right.getDynamics(now), RealDynamics::plus);
      }
    };
  }

  public static <$Schema>
  RealResource<$Schema>
  subtract(final RealResource<$Schema> left, final RealResource<$Schema> right) {
    Objects.requireNonNull(left);
    Objects.requireNonNull(right);

    return new RealResource<>() {
      @Override
      public DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now) {
        return left.getDynamics(now).parWith(right.getDynamics(now), RealDynamics::minus);
      }
    };
  }


  public RealResource<$Schema> plus(final RealResource<$Schema> other) {
    return RealResource.add(this, other);
  }

  public RealResource<$Schema> minus(final RealResource<$Schema> other) {
    return RealResource.subtract(this, other);
  }

  public RealResource<$Schema> scaledBy(final double scalar) {
    return RealResource.scaleBy(scalar, this);
  }


  public final double ask(final History<? extends $Schema> now) {
    return new RealSolver().valueAt(this.getDynamics(now).getDynamics(), Duration.ZERO);
  }

  public Condition<$Schema> isBetween(final double lower, final double upper) {
    return Condition.atom(
          new RealResourceSolver<>(),
          this,
          new RealCondition(ClosedInterval.between(lower, upper)));
  }
}
