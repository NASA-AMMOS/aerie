package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.timeline.History;

import java.util.Objects;
import java.util.function.Function;

public abstract class RealResource {
  private RealResource() {}

  protected abstract RealDynamics getDynamics(final CellGetter getter);

  private interface CellGetter {
    <CellType> CellType get(CellRef<?, CellType> ref);
  }


  public static <CellType>
  RealResource
  atom(final CellRef<?, CellType> ref, final Function<CellType, RealDynamics> property) {
    Objects.requireNonNull(ref);
    Objects.requireNonNull(property);

    return new RealResource() {
      @Override
      public RealDynamics getDynamics(final CellGetter getter) {
        return property.apply(getter.get(ref));
      }
    };
  }

  public static
  RealResource
  scaleBy(final double scalar, final RealResource resource) {
    Objects.requireNonNull(resource);

    return new RealResource() {
      @Override
      public RealDynamics getDynamics(final CellGetter getter) {
        return resource.getDynamics(getter).scaledBy(scalar);
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
      public RealDynamics getDynamics(final CellGetter getter) {
        return left.getDynamics(getter).plus(right.getDynamics(getter));
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
      public RealDynamics getDynamics(final CellGetter getter) {
        return left.getDynamics(getter).minus(right.getDynamics(getter));
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


  public final RealDynamics getDynamics() {
    return this.getDynamics(new CellGetter() {
      @Override
      public <CellType> CellType get(final CellRef<?, CellType> ref) {
        return ref.get();
      }
    });
  }

  public final RealDynamics getDynamicsAt(final History<?> now) {
    Objects.requireNonNull(now);

    return this.getDynamics(new CellGetter() {
      @Override
      public <CellType> CellType get(final CellRef<?, CellType> ref) {
        return ref.getAt(now);
      }
    });
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
