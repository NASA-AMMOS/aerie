package gov.nasa.jpl.aerie.timeline.collections.profiles

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.BaseTimeline
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.Timeline
import gov.nasa.jpl.aerie.timeline.ops.SerialConstantOps
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceSegmentsOp
import gov.nasa.jpl.aerie.timeline.util.preprocessList

/** A profile of piece-wise constant values. */
data class Constants<V: Any>(private val timeline: Timeline<Segment<V>, Constants<V>>):
    Timeline<Segment<V>, Constants<V>> by timeline,
    CoalesceSegmentsOp<V, Constants<V>>,
    SerialConstantOps<V, Constants<V>>
{
  constructor(v: V): this(Segment(Interval.MIN_MAX, v))
  constructor(vararg segments: Segment<V>): this(segments.asList())
  constructor(segments: List<Segment<V>>): this(BaseTimeline(::Constants, preprocessList(segments, Segment<V>::valueEquals)))

  /***/ companion object {
    /**
     * Delegates to the constructor, for use with [gov.nasa.jpl.aerie.timeline.plan.Plan.resource].
     *
     * Does not convert the serialized values because it does not know what it should be converted into.
     * You will need to do that yourself using [mapValues].
     */
    @JvmStatic fun deserialize(list: List<Segment<SerializedValue>>) = Constants(list)
  }
}
