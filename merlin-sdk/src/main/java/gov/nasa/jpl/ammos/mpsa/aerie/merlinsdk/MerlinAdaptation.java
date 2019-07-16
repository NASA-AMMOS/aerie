package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;

/**
 * A system-level representation of a mission-specific adaptation.
 *
 * The Merlin system, and Aerie in a broader sense, must be able to extract information from
 * an adaptation in order to tune its multi-mission capabilities to the needs of a specific
 * mission. This interface is the top-level entry point: the first adaptation object that
 * Merlin will interact with is an {@code MerlinAdaptation}.
 *
 * An implementation of {@code MerlinAdaptation} ought to announce itself in an associated
 * {@code module-info.java} file in the adaptation bundle. For instance:
 *
 * <pre>
 *   import gov.nasa.jpl.ammos.mpsa.aerie.bananatation.Bananatation;
 *   import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
 *
 *   module gov.nasa.jpl.ammos.mpsa.aerie.bananatation {
 *     requires gov.nasa.jpl.ammos.mpsa.aerie.merlin;
 *
 *     provides MerlinAdaptation with Bananatation;
 *   }
 * </pre>
 */
public interface MerlinAdaptation {
  /**
   * Gets the system-level representation of the activity types understood by this adaptation.
   *
   * @return The activity mapper for this adaptation.
   */
  ActivityMapper getActivityMapper();
}
