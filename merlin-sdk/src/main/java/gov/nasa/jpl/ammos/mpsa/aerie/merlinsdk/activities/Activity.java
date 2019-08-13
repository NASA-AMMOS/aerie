package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;

import java.util.List;

/**
 * A mission-specific representation of an activity.
 *
 * Mission activities should implement this interface, as well as the {@link ActivityType}
 * protocol. Implementations of this interface provide methods used by the Merlin system
 * to interact with activity instances.
 */
public interface Activity<T extends StateContainer> {
  /**
   * Checks if this activity instance is valid according to mission-specific criteria.
   *
   * @return A list of validation failures, or an empty list if no failures occurred.
   */
  default List<String> validateParameters() { return List.of(); }

  /**
   * Performs the effects of simulating this activity.
   *
   * It is expected that effects are effected upon state acquired from a State Controller,
   * injected into the activity by the Merlin Framework.
   */
  default void modelEffects(SimulationContext<T> ctx, T states) { }

}
