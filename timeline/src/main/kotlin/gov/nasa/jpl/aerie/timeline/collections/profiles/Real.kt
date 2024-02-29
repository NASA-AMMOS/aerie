package gov.nasa.jpl.aerie.timeline.collections.profiles

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.BaseTimeline
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.Timeline
import gov.nasa.jpl.aerie.timeline.ops.numeric.SerialLinearOps
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceSegmentsOp
import gov.nasa.jpl.aerie.timeline.payloads.LinearEquation
import gov.nasa.jpl.aerie.timeline.util.preprocessList
import kotlin.jvm.optionals.getOrNull

/** A profile of [LinearEquations][LinearEquation]; a piece-wise linear real-number profile. */
data class Real(private val timeline: Timeline<Segment<LinearEquation>, Real>):
    Timeline<Segment<LinearEquation>, Real> by timeline,
    SerialLinearOps<Real>,
    CoalesceSegmentsOp<LinearEquation, Real>
{
  constructor(v: Int): this(v.toDouble())
  constructor(v: Long): this(v.toDouble())
  constructor(v: Double): this(LinearEquation(v))
  constructor(eq: LinearEquation): this(Segment(Interval.MIN_MAX, eq))
  constructor(vararg segments: Segment<LinearEquation>): this(segments.asList())
  constructor(segments: List<Segment<LinearEquation>>): this(BaseTimeline(::Real, preprocessList(segments, Segment<LinearEquation>::valueEquals)))

  /***/ companion object {
    /**
     * Converts a list of serialized value segments into a real profile; for use with [gov.nasa.jpl.aerie.timeline.plan.Plan.resource].
     *
     * Accepts either a map with the form `{initial: number, rate: number}`, or just a plain number for piecewise constant profiles.
     * While plain numbers are acceptable and will be converted to a [LinearEquation] without warning, consider using [Numbers]
     * to keep the precision.
     */
    @JvmStatic fun deserialize(list: List<Segment<SerializedValue>>): Real {
      val converted: List<Segment<LinearEquation>> = list.map { s ->
        s.value.asReal().getOrNull()?.let { return@map s.withNewValue(LinearEquation(it)) }
        val map = s.value.asMap().orElseThrow { RealDeserializeException("value was not a map or plain number: $s") }
        val initialValue = (map["initial"]
            ?: throw RealDeserializeException("initial value not found in map"))
            .asReal().orElseThrow { RealDeserializeException("initial value was not a double") }
        val rate = (map["rate"] ?: throw RealDeserializeException("rate not found in map"))
            .asReal().orElseThrow { RealDeserializeException("rate was not a double") }
        s.withNewValue(LinearEquation(s.interval.start, initialValue, rate))
      }
      return Real(converted)
    }

    /***/ class RealDeserializeException(message: String): Exception(message)
  }
}
