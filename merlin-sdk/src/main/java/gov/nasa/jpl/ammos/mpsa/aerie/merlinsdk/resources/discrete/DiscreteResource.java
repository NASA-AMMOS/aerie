package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;

import java.util.function.Function;

@FunctionalInterface
public interface DiscreteResource<Model, T> extends Resource<Model, T> {
  default <S> DiscreteResource<Model, S> map(final Function<T, S> transform) {
    return (state) -> this.getDynamics(state).map(transform);
  }

  @Override
  default <$> DiscreteResource<History<$, ?>, T> connect(final Query<$, ? extends Model> query) {
    return (history) -> this.getDynamics(query.getAt(history));
  }
}
