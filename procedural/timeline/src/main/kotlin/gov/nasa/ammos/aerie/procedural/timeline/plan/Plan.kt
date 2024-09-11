package gov.nasa.ammos.aerie.procedural.timeline.plan

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.AnyDirective
import gov.nasa.ammos.aerie.procedural.timeline.collections.Directives
import java.time.Instant

/** An interface for querying plan information and simulation results. */
@JvmDefaultWithoutCompatibility
interface Plan {
  /** Total extent of the plan's bounds, whether it was simulated on the full extent or not. */
  fun totalBounds(): Interval

  /** The total duration of the plan, whether simulated on the full extent or not. */
  fun duration() = totalBounds().duration()

  /** Convert a time instant to a relative duration (relative to plan start). */
  fun toRelative(abs: Instant): Duration
  /** Convert a relative duration to a time instant. */
  fun toAbsolute(rel: Duration): Instant

  /**
   * Query activity directives.
   *
   * @param type Activity type name to filter by; queries all activities if null.
   * @param deserializer a function from [SerializedValue] to an inner payload type
   */
  fun <A: Any> directives(type: String?, deserializer: (SerializedValue) -> A): Directives<A>
  /** Queries activity directives, filtered by type, deserializing them as [AnyDirective]. **/
  fun directives(type: String) = directives(type, AnyDirective.deserializer())
  /** Queries all activity directives, deserializing them as [AnyDirective]. **/
  fun directives() = directives(null, AnyDirective.deserializer())
}
