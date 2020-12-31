package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Property;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueMapper;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public abstract class DiscreteResource<$Schema, T> {
  private DiscreteResource() {}

  public abstract DelimitedDynamics<T> getDynamics(History<? extends $Schema> history);

  public static <$Schema, T>
  DiscreteResource<$Schema, T> atom(final Property<History<? extends $Schema>, T> property) {
    Objects.requireNonNull(property);

    return new DiscreteResource<>() {
      @Override
      public DelimitedDynamics<T> getDynamics(final History<? extends $Schema> now) {
        return property.ask(now);
      }
    };
  }

  public static <$Schema, T, S>
  DiscreteResource<$Schema, S> mapped(final DiscreteResource<$Schema, T> resource, final Function<T, S> transform) {
    Objects.requireNonNull(resource);
    Objects.requireNonNull(transform);

    return new DiscreteResource<>() {
      @Override
      public DelimitedDynamics<S> getDynamics(final History<? extends $Schema> now) {
        return resource.getDynamics(now).map(transform);
      }
    };
  }


  public T ask(final History<? extends $Schema> now) {
    return this.getDynamics(now).getDynamics();
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
