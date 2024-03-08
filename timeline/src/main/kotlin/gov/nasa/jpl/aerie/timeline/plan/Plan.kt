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

  /**
   * Query activity instances.
   *
   * @param type Activity type name to filter by; queries all activities if null.
   * @param deserializer a function from [SerializedValue] to an inner payload type
   */
  fun <A: Any> instances(type: String?, deserializer: (SerializedValue) -> A): Instances<A>
  /** Queries activity instances, filtered by type, deserializing them as [AnyInstance]. **/
  fun instances(type: String) = instances(type, AnyInstance::deserialize)
  /** Queries all activity instances, deserializing them as [AnyInstance]. **/
  fun instances() = instances(null, AnyInstance::deserialize)

  /**
   * Query activity directives.
   *
   * @param type Activity type name to filter by; queries all activities if null.
   * @param deserializer a function from [SerializedValue] to an inner payload type
   */
  fun <A: Any> directives(type: String?, deserializer: (SerializedValue) -> A): Directives<A>
  /** Queries activity directives, filtered by type, deserializing them as [AnyDirective]. **/
  fun directives(type: String) = directives(type, AnyDirective::deserialize)
  /** Queries all activity directives, deserializing them as [AnyDirective]. **/
  fun directives() = directives(null, AnyDirective::deserialize)
}
