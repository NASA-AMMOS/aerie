package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ActivityType;

import java.util.List;

/**
 * A mission-specific representation of an activity.
 *
 * Mission activities should implement this interface, as well as the {@link ActivityType}
 * protocol. Implementations of this interface provide methods used by the Merlin system
 * to interact with activity instances.
 */
public interface Activity {
  /**
   * Checks if this activity instance is valid according to mission-specific criteria.
   *
   * @return A list of validation failures, or an empty list if no failures occurred.
   */
  default List<String> validateParameters() { return List.of(); }

  /**
   * Performs the effects of simulating this activity.
   */
  default void modelEffects() { }
}
