package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;

import java.util.Objects;
import java.util.function.Function;

public abstract class RealResource {
  private RealResource() {}

  public abstract RealDynamics getDynamics();


  public static <CellType>
  RealResource
  atom(final CellRef<?, CellType> ref, final Function<CellType, RealDynamics> property) {
    Objects.requireNonNull(ref);
    Objects.requireNonNull(property);

    return new RealResource() {
      @Override
      public RealDynamics getDynamics() {
        return property.apply(ref.get());
      }
    };
  }

  public static
  RealResource
  scaleBy(final double scalar, final RealResource resource) {
    Objects.requireNonNull(resource);

    return new RealResource() {
      @Override
      public RealDynamics getDynamics() {
        return resource.getDynamics().scaledBy(scalar);
      }
    };
  }

  public static
  RealResource
  add(final RealResource left, final RealResource right) {
    Objects.requireNonNull(left);
    Objects.requireNonNull(right);

    return new RealResource() {
      @Override
      public RealDynamics getDynamics() {
        return left.getDynamics().plus(right.getDynamics());
      }
    };
  }

  public static
  RealResource
  subtract(final RealResource left, final RealResource right) {
    Objects.requireNonNull(left);
    Objects.requireNonNull(right);

    return new RealResource() {
      @Override
      public RealDynamics getDynamics() {
        return left.getDynamics().minus(right.getDynamics());
      }
    };
  }


  public RealResource plus(final RealResource other) {
    return RealResource.add(this, other);
  }

  public RealResource minus(final RealResource other) {
    return RealResource.subtract(this, other);
  }

  public RealResource scaledBy(final double scalar) {
    return RealResource.scaleBy(scalar, this);
  }


  public final double ask() {
    return this.getDynamics().initial;
  }

  public Condition isBetween(final double lower, final double upper) {
    final var condition = new RealCondition(ClosedInterval.between(lower, upper));

    return (scope, positive) -> {
      final var dynamics = this.getDynamics();

      return (positive)
          ? dynamics.whenSatisfies(condition, scope)
          : dynamics.whenDissatisfies(condition, scope);
    };
  }
}
