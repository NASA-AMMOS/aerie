package gov.nasa.jpl.aerie.timeline.ops.numeric

import gov.nasa.jpl.aerie.timeline.*
import gov.nasa.jpl.aerie.timeline.Interval.Inclusivity.Exclusive
import gov.nasa.jpl.aerie.timeline.collections.profiles.Real
import gov.nasa.jpl.aerie.timeline.collections.profiles.Booleans
import gov.nasa.jpl.aerie.timeline.collections.profiles.Numbers
import gov.nasa.jpl.aerie.timeline.payloads.LinearEquation
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.payloads.transpose
import gov.nasa.jpl.aerie.timeline.util.truncateList
import kotlin.math.pow

/**
 * Operations mixin for segment-valued profiles whose payloads
 * represent continuous, piecewise linear values.
 *
 * Currently only used for Real profiles, but in the future could be refactored for
 * duration profiles or parallel real profiles.
 */
interface SerialLinearOps<THIS: SerialLinearOps<THIS>>: SerialNumericOps<LinearEquation, THIS>, LinearOps<THIS> {
  override fun toSerialLinear() = unsafeCast(::Real)
  override fun LinearEquation.toLinear() = this

  /**
   * Converts this to a primitive number profile (i.e. [Numbers]), throwing an error if this is not piece-wise constant.
   *
   * @param message error message to throw if this is not piece-wise constant.
   */
  fun toSerialPrimitiveNumbers(message: String? = null) = mapValues(::Numbers) {
    if (it.value.isConstant()) it.value.initialValue
    else if (message == null) throw SerialLinearOpException("Cannot convert a non-piecewise-constant linear equation to a constant number. (at time ${it.interval.start})")
    else throw SerialLinearOpException("$message (at time ${it.interval.start})")
  }

  /** [(DOC)][plus] Adds this and another numeric profile. */
  operator fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> plus(other: SerialNumericOps<W, OTHER>) = map2Values(other.toSerialLinear(), BinaryOperation.combineOrNull { l, r, _ ->
    val shiftedRight = r.shiftInitialTime(l.initialTime)
    LinearEquation(l.initialTime, l.initialValue + shiftedRight.initialValue, l.rate + r.rate)
  })
  /** [(DOC)][plus] Adds a constant number to this. */
  operator fun plus(n: Number) = plus(Numbers(n))

  /** [(DOC)][minus] Subtracts another numeric profile from this. */
  operator fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> minus(other: SerialNumericOps<W, OTHER>) = map2Values(other.toSerialLinear(), BinaryOperation.combineOrNull { l, r, _ ->
    val shiftedRight = r.shiftInitialTime(l.initialTime)
    LinearEquation(l.initialTime, l.initialValue - shiftedRight.initialValue, l.rate - r.rate)
  })
  /** [(DOC)][minus] Subtracts a constant number from this. */
  operator fun minus(n: Number) = minus(Numbers(n))

  /**
   * [(DOC)][times] Multiplies this and another numeric profile.
   *
   * @throws SerialLinearOpException if both profiles have non-zero rate at the same time.
   */
  operator fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> times(other: SerialNumericOps<W, OTHER>) = map2Values(other.toSerialLinear(), BinaryOperation.combineOrNull { l, r, i ->
    if (!l.isConstant() && !r.isConstant()) throw SerialLinearOpException("Cannot multiply two linear equations that are non-constant at the same time (at time ${i.start})")
    val shiftedRight = r.shiftInitialTime(l.initialTime)
    val newRate = l.rate * shiftedRight.initialValue + r.rate * l.initialValue
    LinearEquation(l.initialTime, l.initialValue * shiftedRight.initialValue, newRate)
  })
  /** [(DOC)][times] Multiplies this by a constant number. */
  operator fun times(n: Number) = times(Numbers(n))

  /**
   * [(DOC)][div] Calculates this divided by another numeric profile.
   *
   * @throws SerialLinearOpException if the divisor has a non-zero rate at any time that the dividend is defined.
   */
  operator fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> div(other: SerialNumericOps<W, OTHER>) = map2Values(other.toSerialLinear(), BinaryOperation.combineOrNull { l, r, i ->
    if (!r.isConstant()) throw SerialLinearOpException("Cannot divide by a non-piecewise-constant linear equation (at time ${i.start})")
    LinearEquation(l.initialTime, l.initialValue / r.initialValue, l.rate / r.initialValue)
  })
  /** [(DOC)][div] Calculates this divided by a contant number. */
  operator fun div(n: Number) = div(Numbers(n))

  /**
   * [(DOC)][pow] Calculates this raised to the power of another numeric profile.
   *
   * @throws SerialLinearOpException if the exponent has a non-zero rate at any time that the base is defined,
   *                                 or if the base has a non-zero rate at any time that the exponent is defined and not
   *                                 either 0 or 1.
   */
  infix fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> pow(exp: SerialNumericOps<W, OTHER>) = map2Values(exp.toSerialLinear(), BinaryOperation.combineOrNull { l, r, i ->
    if (!r.isConstant()) throw SerialLinearOpException("Cannot apply a non-piecewise-constant exponent (at time ${i.start}")
    if (r.initialValue == 0.0) LinearEquation(1.0)
    else if (r.initialValue == 1.0) l
    else if (!l.isConstant()) throw SerialLinearOpException("Cannot apply an exponent to a non-piecewise-constant profile")
    else LinearEquation(l.initialValue.pow(r.initialValue))
  })
  /** [(DOC)][pow] Calculates this raised to the power of a constant number. */
  infix fun pow(n: Number) = pow(Numbers(n))

  /** [(DOC)][equalTo] Returns a [Booleans] that is true when this and another numeric profile are equal. */
  infix fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> equalTo(other: SerialNumericOps<W, OTHER>) = inequalityHelper(other, LinearEquation::intervalsEqualTo)
  /** [(DOC)][equalTo] Returns a [Booleans] that is true when this equals a constant number. */
  infix fun equalTo(n: Number) = equalTo(Numbers(n))

  /** [(DOC)][notEqualTo] Returns a [Booleans] that is true when this and another numeric profile are not equal. */
  infix fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> notEqualTo(other: SerialNumericOps<W, OTHER>) = inequalityHelper(other, LinearEquation::intervalsNotEqualTo)
  /** [(DOC)][notEqualTo] Returns a [Booleans] that is true when this does not equal a constant number. */
  infix fun notEqualTo(n: Number) = notEqualTo(Numbers(n))

  /** [(DOC)][lessThan] Returns a [Booleans] that is true when this is less than another numeric profile. */
  infix fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> lessThan(other: SerialNumericOps<W, OTHER>) = inequalityHelper(other, LinearEquation::intervalsLessThan)
  /** [(DOC)][lessThan] Returns a [Booleans] that is true when this is less than a constant number. */
  infix fun lessThan(n: Number) = lessThan(Numbers(n))

  /** [(DOC)][lessThanOrEqualTo] Returns a [Booleans] that is true when this is less than or equal to another numeric profile. */
  infix fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> lessThanOrEqualTo(other: SerialNumericOps<W, OTHER>) = inequalityHelper(other, LinearEquation::intervalsLessThanOrEqualTo)
  /** [(DOC)][lessThanOrEqualTo] Returns a [Booleans] that is true when this is less than or equal to a constant number. */
  infix fun lessThanOrEqualTo(n: Number) = lessThanOrEqualTo(Numbers(n))

  /** [(DOC)][greaterThan] Returns a [Booleans] that is true when this is greater than another numeric profile. */
  infix fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> greaterThan(other: SerialNumericOps<W, OTHER>) = inequalityHelper(other, LinearEquation::intervalsGreaterThan)
  /** [(DOC)][greaterThan] Returns a [Booleans] that is true when this is greater than a constant number. */
  infix fun greaterThan(n: Number) = greaterThan(Numbers(n))

  /** [(DOC)][greaterThanOrEqualTo] Returns a [Booleans] that is true when this is greater than or equal to another numeric profile. */
  infix fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> greaterThanOrEqualTo(other: SerialNumericOps<W, OTHER>) = inequalityHelper(other, LinearEquation::intervalsGreaterThanOrEqualTo)
  /** [(DOC)][greaterThanOrEqualTo] Returns a [Booleans] that is true when this is greater than or equal to a constant number. */
  infix fun greaterThanOrEqualTo(n: Number) = greaterThanOrEqualTo(Numbers(n))

  private fun <W: Any, OTHER: SerialNumericOps<W, OTHER>> inequalityHelper(other: SerialNumericOps<W, OTHER>, f: LinearEquation.(LinearEquation) -> Booleans) =
      flatMap2Values(::Booleans, other.toSerialLinear(), BinaryOperation.combineOrNull { l, r, _ -> l.f(r) })


  override fun changes() =
      unsafeOperate(::Booleans) { opts ->
        val bounds = opts.bounds
        var previous: Segment<LinearEquation>? = null
        val result = collect(CollectOptions(bounds, false)).flatMap { currentSegment: Segment<LinearEquation> ->
          val currentInterval = currentSegment.interval
          val leftEdge = if (
              previous !== null &&
              previous!!.interval.compareEndToStart(currentInterval) == 0 &&
              currentInterval.includesStart()
          ) {
            previous!!.value.valueAt(currentInterval.start) == currentSegment.value.valueAt(currentInterval.start)
          } else if (currentInterval.compareStarts(bounds) == 0) {
            currentSegment.value.rate != 0.0
          } else {
            null
          }
          previous = currentSegment
          listOfNotNull(
              Segment(Interval.at(currentInterval.start), leftEdge).transpose(),
              Segment(Interval.between(currentInterval.start, currentInterval.end, Exclusive), currentSegment.value.rate != 0.0)
          )
        }
        truncateList(result, opts)
      }

  /**
   * [(DOC)][transitions] Returns a [Booleans] that is true whenever this discontinuously transitions between
   * a specific pair of values, and false or gap everywhere else.
   */
  fun transitions(from: Double, to: Double) = detectEdges(BinaryOperation.cases(
      { l, i -> if (l.valueAt(i.start) == from) null else false },
      { r, i -> if (r.valueAt(i.start) == to) null else false },
      { l, r, i -> l.valueAt(i.start) == from && r.valueAt(i.start) == to }
  ))

  /**
   * An exception for linear profile operations; usually thrown in contexts that
   * require one or more of the operands to be piecewise constant.
   */
  class SerialLinearOpException(message: String): Exception(message)
}
