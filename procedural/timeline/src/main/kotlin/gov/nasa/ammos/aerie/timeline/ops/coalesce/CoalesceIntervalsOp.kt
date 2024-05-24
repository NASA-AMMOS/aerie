package gov.nasa.ammos.aerie.timeline.ops.coalesce

import gov.nasa.ammos.aerie.timeline.Interval
import gov.nasa.ammos.aerie.timeline.payloads.IntervalLike
import gov.nasa.ammos.aerie.timeline.ops.GeneralOps

/** A coalesce operation for intervals, which always coalesce. */
interface CoalesceIntervalsOp<THIS: CoalesceIntervalsOp<THIS>>: GeneralOps<Interval, THIS> {
  override fun shouldCoalesce() = { _: Interval, _: Interval -> true }
}
