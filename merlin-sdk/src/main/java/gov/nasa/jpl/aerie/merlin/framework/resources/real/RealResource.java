package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;

import java.util.Objects;
import java.util.function.Function;

public abstract class RealResource<$Schema> {
  private RealResource() {}

  public abstract RealDynamics getDynamics(final History<? extends $Schema> now);

  public static <$Schema, CellType>
  RealResource<$Schema>
  atom(final Query<$Schema, ?, CellType> query, final Function<CellType, RealDynamics> property) {
    Objects.requireNonNull(query);
    Objects.requireNonNull(property);

    return new RealResource<>() {
      @Override
      public RealDynamics getDynamics(final History<? extends $Schema> now) {
        return property.apply(now.ask(query));
      }
    };
  }

  public static <$Schema>
  RealResource<$Schema>
  scaleBy(final double scalar, final RealResource<$Schema> resource) {
    Objects.requireNonNull(resource);

    return new RealResource<>() {
      @Override
      public RealDynamics getDynamics(final History<? extends $Schema> now) {
        return resource.getDynamics(now).scaledBy(scalar);
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
      public RealDynamics getDynamics(final History<? extends $Schema> now) {
        return left.getDynamics(now).plus(right.getDynamics(now));
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
      public RealDynamics getDynamics(final History<? extends $Schema> now) {
        return left.getDynamics(now).minus(right.getDynamics(now));
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
    return this.getDynamics(now).initial;
  }

  public Condition<$Schema> isBetween(final double lower, final double upper) {
    return Condition.atom(
          new RealResourceSolver<>(),
          this,
          new RealCondition(ClosedInterval.between(lower, upper)));
  }
}
