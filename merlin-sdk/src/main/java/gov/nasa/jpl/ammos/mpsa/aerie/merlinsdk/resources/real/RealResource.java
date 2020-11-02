package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;

@FunctionalInterface
public interface RealResource<Model> extends Resource<Model, RealDynamics> {
  default RealResource<Model> scaledBy(final double scalar) {
    return (model) -> this.getDynamics(model).map(dynamics -> dynamics.scaledBy(scalar));
  }

  default RealResource<Model> plus(final Resource<Model, RealDynamics> other) {
    return (model) -> this.getDynamics(model).parWith(other.getDynamics(model), RealDynamics::plus);
  }

  default RealResource<Model> minus(final Resource<Model, RealDynamics> other) {
    return (model) -> this.getDynamics(model).parWith(other.getDynamics(model), RealDynamics::minus);
  }

  @Override
  default <$> RealResource<History<? extends $, ?>> connect(final Query<$, ? extends Model> query) {
    return (history) -> this.getDynamics(query.getAt(history));
  }
}
