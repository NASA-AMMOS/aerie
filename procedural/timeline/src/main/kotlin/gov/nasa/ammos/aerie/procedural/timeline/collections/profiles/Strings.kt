package gov.nasa.ammos.aerie.procedural.timeline.collections.profiles

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.ammos.aerie.procedural.timeline.*
import gov.nasa.ammos.aerie.procedural.timeline.util.duration.unaryMinus
import gov.nasa.ammos.aerie.procedural.timeline.payloads.Segment
import gov.nasa.ammos.aerie.procedural.timeline.ops.SerialConstantOps
import gov.nasa.ammos.aerie.procedural.timeline.ops.numeric.PrimitiveNumberOps
import gov.nasa.ammos.aerie.procedural.timeline.ops.numeric.SerialNumericOps
import gov.nasa.ammos.aerie.procedural.timeline.payloads.LinearEquation
import gov.nasa.ammos.aerie.procedural.timeline.util.preprocessList
import java.lang.ArithmeticException
import kotlin.math.pow

/**
 * A profile of Strings.
 *
 * @see String
 */
data class Strings(private val timeline: Timeline<Segment<String>, Strings>):
  Timeline<Segment<String>, Strings> by timeline,
  SerialConstantOps<String, Strings>
{
  constructor(v: String): this(Segment(Interval.MIN_MAX, v))
  constructor(vararg segments: Segment<String>): this(segments.asList())
  constructor(segments: List<Segment<String>>): this(
      gov.nasa.ammos.aerie.procedural.timeline.BaseTimeline(
          ::Strings,
          preprocessList(segments, Segment<String>::valueEquals)
      )
  )

  /** [(DOC)][length] Returns a [Numbers] profile of the lengths. */
  fun length() = mapValues(::Numbers) { it.value.length }

  /** Check for string equality, case-insensitive. */
  fun caseInsensitiveEqualTo(other: SerialConstantOps<String, *>) =
    map2Values(::Booleans, other) { l, r, _ -> l.equals(r, ignoreCase = true) }

  /** Check for string equality with a single string, case-insensitive. */
  fun caseInsensitiveEqualTo(other: String) = caseInsensitiveEqualTo(Strings(other))

  /** Check for string inequality, case-insensitive. */
  fun caseInsensitiveNotEqualTo(other: SerialConstantOps<String, *>) =
    map2Values(::Booleans, other) { l, r, _ -> !l.equals(r, ignoreCase = true) }

  /** Check for string inequality with a single string, case-insensitive. */
  fun caseInsensitiveNotEqualTo(other: String) = caseInsensitiveNotEqualTo(Strings(other))

  /** Returns a [Booleans] that is true when this is the empty string. */
  fun isEmpty() = mapValues(::Booleans) { it.value.isEmpty() }

  /** Returns a [Booleans] that is true when this is not the empty string. */
  fun isNotEmpty() = mapValues(::Booleans) { it.value.isNotEmpty() }

  /***/ companion object {
    /**
     * Converts a list of serialized value segments into a [Strings] profile;
     * for use with [gov.nasa.ammos.aerie.procedural.timeline.plan.Plan.resource].
     */
    @JvmStatic fun deserialize(list: List<Segment<SerializedValue>>) = Strings(list.map { seg ->
      val string = seg.value.asString().orElseThrow { Exception("value was not a string: $seg") }
      seg.withNewValue(string)
    })
  }
}
