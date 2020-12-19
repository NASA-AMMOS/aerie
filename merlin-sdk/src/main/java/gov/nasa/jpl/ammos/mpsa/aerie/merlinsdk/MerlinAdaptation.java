package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A mission-agnostic interface to the capabilities provided by a mission-specific adaptation.
 *
 * <p>
 * The Merlin system, and Aerie in a broader sense, must be able to extract information
 * from an adaptation in order to tune its multi-mission capabilities to the needs of
 * a specific mission. This interface is the top-level entry point: the first adaptation object
 * that Merlin will interact with is an {@code MerlinAdaptation}.
 * </p>
 *
 * <p>
 * An implementation of {@code MerlinAdaptation} must be registered in a file in the adaptation JAR
 * named {@code META-INF/services/gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation}. This file
 * shall contain the fully-qualified name of the implementing class, such as
 * {@code gov.nasa.jpl.ammos.mpsa.aerie.banananation.Banananation}.
 * </p>
 *
 * <p>
 * An implementation of {@code MerlinAdaptation} must provide a no-arguments constructor. This constructor
 * should not generally perform any work, as the adaptation object may be constructed simply to check metadata
 * or perform other validation.
 * </p>
 *
 * <p>
 * An implementation of {@code MerlinAdaptation} must be annotated with @{@link Adaptation}.
 * </p>
 *
 * @see AbstractMerlinAdaptation
 */
@Deprecated
public interface MerlinAdaptation<Event> {
  /**
   * Provides a mission-agnostic representation of the activity types provided by this adaptation.
   *
   * @return An activity mapper.
   */
  ActivityMapper getActivityMapper();
  List<ViolableConstraint> getViolableConstraints();
  Map<String, ValueSchema> getStateSchemas();
  <T> Querier<T, Event> makeQuerier(final SimulationTimeline<T, Event> database);

  interface Querier<T, Event> {
    void runActivity(ReactionContext<T, Event, Activity> ctx, String activityId, Activity activity);

    Set<String> states();
    SerializedValue getSerializedStateAt(String name, History<T, Event> history);
    List<ConstraintViolation> getConstraintViolationsAt(History<T, Event> history);
  }
}
