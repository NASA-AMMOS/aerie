package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.timeline.History;

import java.util.Objects;
import java.util.function.Function;

public abstract class RealResource<$Schema> {
  private RealResource() {}

  protected abstract RealDynamics getDynamics(final CellGetter<$Schema> getter);

  private interface CellGetter<$Schema> {
    <CellType> CellType get(CellRef<$Schema, ?, CellType> ref);
  }


  public static <$Schema, CellType>
  RealResource<$Schema>
  atom(final CellRef<$Schema, ?, CellType> ref, final Function<CellType, RealDynamics> property) {
    Objects.requireNonNull(ref);
    Objects.requireNonNull(property);

    return new RealResource<>() {
      @Override
      public RealDynamics getDynamics(final CellGetter<$Schema> getter) {
        return property.apply(getter.get(ref));
      }
    };
  }

  public static <$Schema>
  RealResource<$Schema>
  scaleBy(final double scalar, final RealResource<$Schema> resource) {
    Objects.requireNonNull(resource);

    return new RealResource<>() {
      @Override
      public RealDynamics getDynamics(final CellGetter<$Schema> getter) {
        return resource.getDynamics(getter).scaledBy(scalar);
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
      public RealDynamics getDynamics(final CellGetter<$Schema> getter) {
        return left.getDynamics(getter).plus(right.getDynamics(getter));
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
      public RealDynamics getDynamics(final CellGetter<$Schema> getter) {
        return left.getDynamics(getter).minus(right.getDynamics(getter));
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


  public final RealDynamics getDynamics() {
    return this.getDynamics(new CellGetter<>() {
      @Override
      public <CellType> CellType get(final CellRef<$Schema, ?, CellType> ref) {
        return ref.get();
      }
    });
  }

  public final RealDynamics getDynamicsAt(final History<? extends $Schema> now) {
    Objects.requireNonNull(now);

    return this.getDynamics(new CellGetter<>() {
      @Override
      public <CellType> CellType get(final CellRef<$Schema, ?, CellType> ref) {
        return ref.getAt(now);
      }
    });
  }


  public final double ask() {
    return this.getDynamics().initial;
  }

  public Condition<$Schema> isBetween(final double lower, final double upper) {
    return Condition.atom(
          new RealResourceSolver<>(),
          this,
          new RealCondition(ClosedInterval.between(lower, upper)));
  }
}
