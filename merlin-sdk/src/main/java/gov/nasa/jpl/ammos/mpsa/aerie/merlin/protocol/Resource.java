package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics;

/**
 * An assignment of a dynamics to a resource given observable stimuli over time.
 *
 * @param <$Schema> The schema over which this resource's behavior can be inferred.
 * @param <Dynamics> The type of dynamics governing this resource's behavior over time.
 */
@FunctionalInterface
public interface Resource<$Schema, Dynamics> {
  /**
   * Get the dynamics associated to this resource given observable stimuli.
   *
   * @param history The stimuli that have occurred up to now.
   * @return The current dynamical behavior of the resource.
   */
  DelimitedDynamics<Dynamics> getDynamics(History<? extends $Schema> history);
}
