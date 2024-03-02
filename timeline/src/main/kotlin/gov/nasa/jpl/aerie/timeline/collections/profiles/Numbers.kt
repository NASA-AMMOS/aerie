package gov.nasa.jpl.aerie.timeline.collections.profiles

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.BaseTimeline
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.Timeline
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceSegmentsOp
import gov.nasa.jpl.aerie.timeline.ops.numeric.SerialPrimitiveNumberOps
import gov.nasa.jpl.aerie.timeline.util.preprocessList
import java.lang.ArithmeticException

/**
 * A profile of piece-wise constant numbers.
 *
 * Unlike [Real], this is not able to vary linearly. Instead,
 * it can contain either homogeneous (and strictly-typed) collection of
 * any numeric type (i.e. `Numbers<Integer>` (Java) or `Numbers<Int>` (Kotlin)),
 * or a heterogeneous collection of all numeric types (i.e. `Numbers<Number>`).
 *
 * Unfortunately Kotlin/Java is not smart enough to keep the type information during binary operations.
 * So the sum of two `Numbers<Double>` objects will become `Numbers<Number>`, although no precision
 * will be lost.
 *
 * @see Number
 */
data class Numbers<N: Number>(private val timeline: Timeline<Segment<N>, Numbers<N>>):
    Timeline<Segment<N>, Numbers<N>> by timeline,
    SerialPrimitiveNumberOps<N, Numbers<N>> {
  constructor(v: N): this(Segment(Interval.MIN_MAX, v))
  constructor(vararg segments: Segment<N>): this(segments.asList())
  constructor(segments: List<Segment<N>>): this(BaseTimeline(::Numbers, preprocessList(segments, Segment<N>::valueEquals)))

  /***/ companion object {
    /**
     * Converts a list of serialized value segments into a [Numbers] profile;
     * for use with [gov.nasa.jpl.aerie.timeline.plan.Plan.resource].
     *
     * Prefers converting to longs if possible, and falls back to doubles if not.
     */
    @JvmStatic fun deserialize(list: List<Segment<SerializedValue>>) = Numbers(list.map { seg ->
      val bigDecimal = seg.value.asNumeric().orElseThrow { Exception("value was not numeric: $seg") }
      val number: Number = try {
        bigDecimal.longValueExact()
      } catch (e: ArithmeticException) {
        bigDecimal.toDouble()
      }
      seg.withNewValue(number)
    })
  }
}
