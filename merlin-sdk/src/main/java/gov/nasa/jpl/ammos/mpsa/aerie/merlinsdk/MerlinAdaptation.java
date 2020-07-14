package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;

import java.util.List;
import java.util.Set;

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
public interface MerlinAdaptation<Event> {
  /**
   * Gets the system-level representation of the activity types understood by this adaptation.
   *
   * @return The activity mapper for this adaptation.
   */
  ActivityMapper getActivityMapper();

  <T> Querier<T, Event> makeQuerier(final SimulationTimeline<T, Event> database);

  interface Querier<T, Event> {
    void runActivity(ReactionContext<T, Activity, Event> ctx, String activityId, Activity activity);

    Set<String> states();
    SerializedParameter getSerializedStateAt(String name, History<T, Event> history);
    List<ConstraintViolation> getConstraintViolationsAt(History<T, Event> history);
  }
}
