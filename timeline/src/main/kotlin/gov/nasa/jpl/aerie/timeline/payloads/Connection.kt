package gov.nasa.jpl.aerie.timeline.payloads

import gov.nasa.jpl.aerie.timeline.Interval

/** Represents a pairing of two other [IntervalLike] objects. */
data class Connection<FROM: IntervalLike<FROM>, TO: IntervalLike<TO>>(
    override val interval: Interval,
    /** Object at the start of the connection. */
    val from: FROM?,
    /** Object at the end of the connection. */
    val to: TO?
): IntervalLike<Connection<FROM, TO>> {
  override fun withNewInterval(i: Interval) = Connection(i, from, to)
}
