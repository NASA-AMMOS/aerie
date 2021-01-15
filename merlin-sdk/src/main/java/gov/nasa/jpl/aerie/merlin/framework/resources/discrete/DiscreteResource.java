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

  public abstract T getDynamics(History<? extends $Schema> history);

  public static <$Schema, CellType, T>
  DiscreteResource<$Schema, T> atom(final CellRef<$Schema, ?, CellType> ref, final Function<CellType, T> property) {
    Objects.requireNonNull(ref);
    Objects.requireNonNull(property);

    return new DiscreteResource<>() {
      @Override
      public T getDynamics(final History<? extends $Schema> now) {
        return property.apply(ref.getAt(now));
      }
    };
  }

  public static <$Schema, T, S>
  DiscreteResource<$Schema, S> mapped(final DiscreteResource<$Schema, T> resource, final Function<T, S> transform) {
    Objects.requireNonNull(resource);
    Objects.requireNonNull(transform);

    return new DiscreteResource<>() {
      @Override
      public S getDynamics(final History<? extends $Schema> now) {
        return transform.apply(resource.getDynamics(now));
      }
    };
  }


  public T ask(final History<? extends $Schema> now) {
    return this.getDynamics(now);
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
