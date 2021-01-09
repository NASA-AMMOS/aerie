package gov.nasa.jpl.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public abstract class DiscreteResource<$Schema, T> {
  private DiscreteResource() {}

  public abstract T getDynamics(History<? extends $Schema> history);

  public static <$Schema, CellType, T>
  DiscreteResource<$Schema, T> atom(final Query<$Schema, ?, CellType> query, final Function<CellType, T> property) {
    Objects.requireNonNull(query);
    Objects.requireNonNull(property);

    return new DiscreteResource<>() {
      @Override
      public T getDynamics(final History<? extends $Schema> now) {
        return property.apply(now.ask(query));
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
