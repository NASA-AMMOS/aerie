package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;

/**
 * A {@link LifeCycleEvent} is a streamable representation of lifecycle events
 * relating to activity instances during simulation. A {@link LifeCycleEvent}
 * belongs to exactly one {@link TimelineSegment}, but a {@link TimelineSegment}
 * can have any number of associated {@link LifeCycleEvent}s.
 *
 * @param activity The associated activity for this {@link LifeCycleEvent} applies to.
 * @param eventType The event to be logged to the timeline. Possible values are "begin", "end".
 */
public final record LifeCycleEvent(
    ActivityInstance activity,
    String eventType) {}
