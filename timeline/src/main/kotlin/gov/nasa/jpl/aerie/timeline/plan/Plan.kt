package gov.nasa.jpl.aerie.timeline.plan

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.Duration
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyDirective
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyInstance
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceSegmentsOp
import gov.nasa.jpl.aerie.timeline.collections.Directives
import gov.nasa.jpl.aerie.timeline.collections.Instances
import java.time.Instant

/** An interface for querying plan information and simulation results. */
interface Plan {
  /** Total extent of the plan's bounds, whether it was simulated on the full extent or not. */
  fun totalBounds(): Interval

  /** Bounds on which the plan was most recently simulated. */
  fun simBounds(): Interval

  /** Convert a time instant to a relative duration (relative to plan start). */
  fun toRelative(abs: Instant): Duration
  /** Convert a relative duration to a time instant. */
  fun toAbsolute(rel: Duration): Instant

  /**
   * Query a resource profile from the database
   *
   * @param ctor constructor of the profile, converting [SerializedValue]
   * @param name string name of the resource
   */
  fun <V: Any, TL: CoalesceSegmentsOp<V, TL>> resource(name: String, ctor: (List<Segment<SerializedValue>>) -> TL): TL

  /** Query all activity instances. */
  fun allActivityInstances(): Instances<AnyInstance>
  /** Query all activity directives. */
  fun allActivityDirectives(): Directives<AnyDirective>
}
