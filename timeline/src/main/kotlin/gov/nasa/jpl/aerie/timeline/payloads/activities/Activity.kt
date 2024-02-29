package gov.nasa.jpl.aerie.timeline.payloads.activities

import gov.nasa.jpl.aerie.timeline.Duration
import gov.nasa.jpl.aerie.timeline.payloads.IntervalLike

/** Unifying interface for activity instances and directives. */
interface Activity<A: Any>: IntervalLike<A> {
  /** String type name of the activity. */
  val type: String

  /** Time that the activity starts. */
  val startTime: Duration
}
