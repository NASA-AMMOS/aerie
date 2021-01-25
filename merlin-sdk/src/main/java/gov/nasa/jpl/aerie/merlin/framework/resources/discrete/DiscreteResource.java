package gov.nasa.jpl.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Condition;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public abstract class DiscreteResource<T> {
  private DiscreteResource() {}

  public abstract T getDynamics();


  public static <CellType, T>
  DiscreteResource<T> atom(final CellRef<?, CellType> ref, final Function<CellType, T> property) {
    Objects.requireNonNull(ref);
    Objects.requireNonNull(property);

    return new DiscreteResource<>() {
      @Override
      public T getDynamics() {
        return property.apply(ref.get());
      }
    };
  }

  public static <T, S>
  DiscreteResource<S> mapped(final DiscreteResource<T> resource, final Function<T, S> transform) {
    Objects.requireNonNull(resource);
    Objects.requireNonNull(transform);

    return new DiscreteResource<>() {
      @Override
      public S getDynamics() {
        return transform.apply(resource.getDynamics());
      }
    };
  }


  public T ask() {
    return this.getDynamics();
  }

  public <S> DiscreteResource<S> map(final Function<T, S> transform) {
    return DiscreteResource.mapped(this, transform);
  }

  public Condition isOneOf(final Set<T> values) {
    return (scope, positive) -> {
      final var dynamics = this.getDynamics();

      return (positive == values.contains(dynamics))
          ? Optional.of(scope.start)
          : Optional.empty();
    };
  }
}
