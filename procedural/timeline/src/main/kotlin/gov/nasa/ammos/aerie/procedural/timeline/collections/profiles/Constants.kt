package gov.nasa.ammos.aerie.procedural.timeline.collections.profiles

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.ammos.aerie.procedural.timeline.payloads.Segment
import gov.nasa.ammos.aerie.procedural.timeline.BaseTimeline
import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.ammos.aerie.procedural.timeline.Timeline
import gov.nasa.ammos.aerie.procedural.timeline.ops.SerialConstantOps
import gov.nasa.ammos.aerie.procedural.timeline.util.preprocessList

/** A profile of piece-wise constant values. */
data class Constants<V: Any>(private val timeline: Timeline<Segment<V>, Constants<V>>):
  Timeline<Segment<V>, Constants<V>> by timeline,
  SerialConstantOps<V, Constants<V>>
{
  constructor(v: V): this(Segment(Interval.MIN_MAX, v))
  constructor(vararg segments: Segment<V>): this(segments.asList())
  constructor(segments: List<Segment<V>>): this(BaseTimeline(::Constants, preprocessList(segments, Segment<V>::valueEquals)))

  /***/ companion object {
    /**
     * Returns a deserializer, for use with [gov.nasa.ammos.aerie.procedural.timeline.plan.Plan.resource].
     */
    @JvmStatic fun <V: Any> deserializer(mapper: (Segment<SerializedValue>) -> V): (List<Segment<SerializedValue>>) -> Constants<V> = {
        l: List<Segment<SerializedValue>> ->
      Constants(l.map { segment -> segment.mapValue(mapper) })
    }
  }
}
