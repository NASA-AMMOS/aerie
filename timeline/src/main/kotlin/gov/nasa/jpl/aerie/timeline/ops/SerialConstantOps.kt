package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.*
import gov.nasa.jpl.aerie.timeline.collections.profiles.Booleans
import gov.nasa.jpl.aerie.timeline.collections.profiles.Constants

/**
 * Operations mixin for segment-valued timelines whose payloads
 * represent constant values.
 */
interface SerialConstantOps<V: Any, THIS: SerialConstantOps<V, THIS>>: SerialSegmentOps<V, THIS>, ConstantOps<V, THIS> {

  /** [(DOC)][equalTo] Returns a [Booleans] that is `true` when this and another profile are equal. */
  infix fun <OTHER: SerialConstantOps<V, OTHER>> equalTo(other: OTHER) =
      map2Values(::Booleans, other) { l, r, _ -> l == r }

  /** [(DOC)][equalTo] Returns a [Booleans] that is `true` when this equals a constant value. */
  infix fun equalTo(v: V) = equalTo(Constants(v))

  /** [(DOC)][notEqualTo] Returns a [Booleans] that is `true` when this and another profile are not equal. */
  infix fun <OTHER: SerialConstantOps<V, OTHER>> notEqualTo(other: OTHER) =
      map2Values(::Booleans, other) { l, r, _ -> l != r }

  /** [(DOC)][notEqualTo] Returns a [Booleans] that is `true` when this is not equal to a constant value. */
  infix fun notEqualTo(v: V) = notEqualTo(Constants(v))

  override fun changes() = detectEdges(NullBinaryOperation.combineOrNull { l, r, _-> l != r })

  /**
   * [(DOC)][transitions] Returns a [Booleans] that is `true` when this profile's value changes between
   * two specific values.
   */
  fun transitions(from: V, to: V) = detectEdges(NullBinaryOperation.cases(
      { l, _ -> if (l == from) null else false },
      { r, _ -> if (r == to) null else false },
      { l, r, _ -> l == from && r == to }
  ))
}
