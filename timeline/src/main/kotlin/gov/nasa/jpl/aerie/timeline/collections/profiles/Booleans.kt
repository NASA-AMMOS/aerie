package gov.nasa.jpl.aerie.timeline.collections.profiles

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.BaseTimeline
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.Timeline
import gov.nasa.jpl.aerie.timeline.ops.SerialBooleanOps
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceSegmentsOp
import gov.nasa.jpl.aerie.timeline.util.preprocessList

/** A profile of booleans. */
data class Booleans(private val timeline: Timeline<Segment<Boolean>, Booleans>):
    Timeline<Segment<Boolean>, Booleans> by timeline,
    SerialBooleanOps<Booleans>
{
  constructor(v: Boolean): this(Segment(Interval.MIN_MAX, v))
  constructor(vararg segments: Segment<Boolean>): this(segments.asList())
  constructor(segments: List<Segment<Boolean>>): this(BaseTimeline(::Booleans, preprocessList(segments, Segment<Boolean>::valueEquals)))

  /***/ companion object {
    /**
     * Converts a list of serialized value segments into a [Booleans] profile;
     * for use with [gov.nasa.jpl.aerie.timeline.plan.Plan.resource].
     */
    @JvmStatic fun deserialize(list: List<Segment<SerializedValue>>) = Booleans(list.map { it.withNewValue(it.value.asBoolean().get()) })
  }
}
