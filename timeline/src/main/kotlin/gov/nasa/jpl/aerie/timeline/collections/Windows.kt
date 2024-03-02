package gov.nasa.jpl.aerie.timeline.collections

import gov.nasa.jpl.aerie.timeline.BaseTimeline
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.Timeline
import gov.nasa.jpl.aerie.timeline.ops.NonZeroDurationOps
import gov.nasa.jpl.aerie.timeline.ops.SerialIntervalOps
import gov.nasa.jpl.aerie.timeline.ops.SerialSegmentOps
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceIntervalsOp
import gov.nasa.jpl.aerie.timeline.util.preprocessList

/** A coalescing timeline of [Intervals][Interval] with no extra data. */
data class Windows(private val timeline: Timeline<Interval, Windows>):
    Timeline<Interval, Windows> by timeline,
    SerialIntervalOps<Windows>,
    NonZeroDurationOps<Interval, Windows>
{
  constructor(vararg intervals: Interval): this(intervals.asList())
  constructor(intervals: List<Interval>): this(BaseTimeline(::Windows, preprocessList(intervals)))
}
