package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;

/**
 * An assignment of a dynamics to a resource given the state of its governing model.
 *
 * @param <Model> The type of the information tracked by the resource's governing model.
 * @param <Dynamics> The type of dynamics governing this resource's behavior over time.
 */
@FunctionalInterface
public interface Resource<Model, Dynamics> {
  /**
   * Get the dynamics associated to this resource under the current state of its model.
   *
   * @param state The current state of the resource's governing model.
   * @return The current dynamical behavior of the resource.
   */
  DelimitedDynamics<Dynamics> getDynamics(Model state);

  /**
   * Derive this resource from an event timeline.
   *
   * @param query A query giving the model state for this resource from an event history.
   * @param <$> The scope/lifetime of the event timeline.
   * @param <Event> The type of event contained in the timeline.
   * @return A resource defined against the new model type.
   */
  default <$, Event> Resource<History<$, Event>, Dynamics> connect(final Query<$, Event, ? extends Model> query) {
    return (history) -> this.getDynamics(query.getAt(history));
  }
}
