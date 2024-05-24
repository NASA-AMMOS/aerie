package gov.nasa.ammos.aerie.timeline.collections

import gov.nasa.ammos.aerie.timeline.payloads.IntervalLike
import gov.nasa.ammos.aerie.timeline.BaseTimeline
import gov.nasa.ammos.aerie.timeline.ops.ParallelOps
import gov.nasa.ammos.aerie.timeline.Timeline
import gov.nasa.ammos.aerie.timeline.ops.NonZeroDurationOps
import gov.nasa.ammos.aerie.timeline.ops.coalesce.CoalesceNoOp
import gov.nasa.ammos.aerie.timeline.util.preprocessList

/** A timeline of any [IntervalLike] object with no special operations. */
data class Intervals<T: IntervalLike<T>>(private val timeline: Timeline<T, Intervals<T>>):
    Timeline<T, Intervals<T>> by timeline,
    ParallelOps<T, Intervals<T>>,
    NonZeroDurationOps<T, Intervals<T>>
{
  constructor(vararg intervals: T): this(intervals.asList())
  constructor(intervals: List<T>): this(
      gov.nasa.ammos.aerie.timeline.BaseTimeline(
          ::Intervals,
          preprocessList(intervals, null)
      )
  )
}
