package gov.nasa.jpl.aerie.timeline.collections

import gov.nasa.jpl.aerie.timeline.payloads.IntervalLike
import gov.nasa.jpl.aerie.timeline.BaseTimeline
import gov.nasa.jpl.aerie.timeline.ops.ParallelOps
import gov.nasa.jpl.aerie.timeline.Timeline
import gov.nasa.jpl.aerie.timeline.ops.NonZeroDurationOps
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceNoOp
import gov.nasa.jpl.aerie.timeline.util.preprocessList

/** A timeline of any [IntervalLike] object with no special operations. */
data class Intervals<T: IntervalLike<T>>(private val timeline: Timeline<T, Intervals<T>>):
    Timeline<T, Intervals<T>> by timeline,
    ParallelOps<T, Intervals<T>>,
    NonZeroDurationOps<T, Intervals<T>>
{
  constructor(vararg intervals: T): this(intervals.asList())
  constructor(intervals: List<T>): this(BaseTimeline(::Intervals, preprocessList(intervals, null)))
}
