package gov.nasa.jpl.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.timeline.History;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public abstract class DiscreteResource<$Schema, T> {
  private DiscreteResource() {}

  protected abstract T getDynamics(final CellGetter<$Schema> getter);

  private interface CellGetter<$Schema> {
    <CellType> CellType get(CellRef<$Schema, ?, CellType> ref);
  }


  public static <$Schema, CellType, T>
  DiscreteResource<$Schema, T> atom(final CellRef<$Schema, ?, CellType> ref, final Function<CellType, T> property) {
    Objects.requireNonNull(ref);
    Objects.requireNonNull(property);

    return new DiscreteResource<>() {
      @Override
      protected T getDynamics(final CellGetter<$Schema> getter) {
        return property.apply(getter.get(ref));
      }
    };
  }

  public static <$Schema, T, S>
  DiscreteResource<$Schema, S> mapped(final DiscreteResource<$Schema, T> resource, final Function<T, S> transform) {
    Objects.requireNonNull(resource);
    Objects.requireNonNull(transform);

    return new DiscreteResource<>() {
      @Override
      protected S getDynamics(final CellGetter<$Schema> getter) {
        return transform.apply(resource.getDynamics(getter));
      }
    };
  }


  public final T getDynamics() {
    return this.getDynamics(new CellGetter<>() {
      @Override
      public <CellType> CellType get(final CellRef<$Schema, ?, CellType> ref) {
        return ref.get();
      }
    });
  }

  public final T getDynamicsAt(final History<? extends $Schema> now) {
    Objects.requireNonNull(now);

    return this.getDynamics(new CellGetter<>() {
      @Override
      public <CellType> CellType get(final CellRef<$Schema, ?, CellType> ref) {
        return ref.getAt(now);
      }
    });
  }


  public T ask(final History<? extends $Schema> now) {
    return this.getDynamicsAt(now);
  }

  public <S> DiscreteResource<$Schema, S> map(final Function<T, S> transform) {
    return DiscreteResource.mapped(this, transform);
  }

  public Condition<$Schema> isOneOf(final Set<T> values, final ValueMapper<T> mapper) {
    return Condition.atom(
        new DiscreteResourceSolver<>(mapper),
        this,
        Set.copyOf(values));
  }
}
