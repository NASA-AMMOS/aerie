package gov.nasa.jpl.aerie.timeline.ops.numeric

import gov.nasa.jpl.aerie.timeline.*
import gov.nasa.jpl.aerie.timeline.collections.profiles.Numbers
import gov.nasa.jpl.aerie.timeline.collections.profiles.Real
import gov.nasa.jpl.aerie.timeline.collections.profiles.Booleans
import gov.nasa.jpl.aerie.timeline.ops.SerialConstantOps
import gov.nasa.jpl.aerie.timeline.payloads.LinearEquation
import kotlin.math.pow

/** Operations for profiles of primitive numbers. */
interface SerialPrimitiveNumberOps<N: Number, THIS: SerialPrimitiveNumberOps<N, THIS>>: SerialNumericOps<N, THIS>, PrimitiveNumberOps<N, THIS>, SerialConstantOps<N, THIS> {
  override fun toSerialLinear() = mapValues(::Real) { LinearEquation(it.value.toDouble()) }


  /*
  Due to the fact there is no superinterface for numbers that includes any arithmetic
  or comparison operators, AFAICT this giant if-else statement needs to be copy-pasted
  for each operation. If you can find a better way, please do.
   */

  /** [(DOC)][plus] Adds this and another primitive numeric profile. */
  operator fun <M: Number, OTHER: SerialPrimitiveNumberOps<M, OTHER>> plus(other: SerialPrimitiveNumberOps<M, OTHER>) =
      map2Values(::Numbers, other, BinaryOperation.combineOrNull { l, r, _ ->
        if (l is Double || r is Double) l.toDouble() + r.toDouble()
        else if (l is Float || r is Float) l.toFloat() + r.toFloat()
        else if (l is Long || r is Long) l.toLong() + r.toLong()
        else if (l is Int || r is Int) l.toInt() + r.toInt()
        else if (l is Short || r is Short) l.toShort() + r.toShort()
        else if (l is Byte || r is Byte) l.toByte() + r.toByte()
        else throw PrimitiveNumberOps.UnreachablePrimitiveNumberException()
      })
  /** [(DOC)][plus] Adds this a constant number. */
  operator fun plus(n: Number) = plus(Numbers(n))
  /** [(DOC)][plus] Adds this and a linear profile. */
  operator fun <OTHER: SerialLinearOps<OTHER>> plus(other: SerialLinearOps<OTHER>) = other + this

  /** [(DOC)][minus] Subtracts another primitive numeric profile from this. */
  operator fun <M: Number, OTHER: SerialPrimitiveNumberOps<M, OTHER>> minus(other: SerialPrimitiveNumberOps<M, OTHER>) =
      map2Values(::Numbers, other, BinaryOperation.combineOrNull { l, r, _ ->
        if (l is Double || r is Double) l.toDouble() - r.toDouble()
        else if (l is Float || r is Float) l.toFloat() - r.toFloat()
        else if (l is Long || r is Long) l.toLong() - r.toLong()
        else if (l is Int || r is Int) l.toInt() - r.toInt()
        else if (l is Short || r is Short) l.toShort() - r.toShort()
        else if (l is Byte || r is Byte) l.toByte() - r.toByte()
        else throw PrimitiveNumberOps.UnreachablePrimitiveNumberException()
      })
  /** [(DOC)][minus] Subtracts a constant number from this. */
  operator fun minus(n: Number) = minus(Numbers(n))
  /** [(DOC)][minus] Subtracts a linear profile from this. */
  operator fun <OTHER: SerialLinearOps<OTHER>> minus(other: SerialLinearOps<OTHER>) = -other + this

  /** [(DOC)][times] Multiplies this and another primitive numeric profile. */
  operator fun <M: Number, OTHER: SerialPrimitiveNumberOps<M, OTHER>> times(other: SerialPrimitiveNumberOps<M, OTHER>) =
      map2Values(::Numbers, other, BinaryOperation.combineOrNull { l, r, _ ->
        if (l is Double || r is Double) l.toDouble() * r.toDouble()
        else if (l is Float || r is Float) l.toFloat() * r.toFloat()
        else if (l is Long || r is Long) l.toLong() * r.toLong()
        else if (l is Int || r is Int) l.toInt() * r.toInt()
        else if (l is Short || r is Short) l.toShort() * r.toShort()
        else if (l is Byte || r is Byte) l.toByte() * r.toByte()
        else throw PrimitiveNumberOps.UnreachablePrimitiveNumberException()
      })
  /** [(DOC)][times] Multiplies this by a constant number. */
  operator fun times(n: Number) = times(Numbers(n))
  /** [(DOC)][times] Multiplies this by a linear profile. */
  operator fun <OTHER: SerialLinearOps<OTHER>> times(other: SerialLinearOps<OTHER>) = other * this

  /** [(DOC)][div] Calculates this divided by another primitive numeric profile. */
  operator fun <M: Number, OTHER: SerialPrimitiveNumberOps<M, OTHER>> div(other: SerialPrimitiveNumberOps<M, OTHER>) =
      map2Values(::Numbers, other, BinaryOperation.combineOrNull { l, r, _ ->
        if (l is Double || r is Double) l.toDouble() / r.toDouble()
        else if (l is Float || r is Float) l.toFloat() / r.toFloat()
        else if (l is Long || r is Long) l.toLong() / r.toLong()
        else if (l is Int || r is Int) l.toInt() / r.toInt()
        else if (l is Short || r is Short) l.toShort() / r.toShort()
        else if (l is Byte || r is Byte) l.toByte() / r.toByte()
        else throw PrimitiveNumberOps.UnreachablePrimitiveNumberException()
      })
  /** [(DOC)][div] Divides this by a constant number. */
  operator fun div(n: Number) = div(Numbers(n))
  /** [(DOC)][div] Divides this by a linear profile. */
  operator fun <OTHER: SerialLinearOps<OTHER>> div(other: SerialLinearOps<OTHER>) = this / other.toSerialPrimitiveNumbers("Cannot divide by a non-piecewise-constant divisor.")

  /**
   * [(DOC)][pow] Calculates this raised to the power of another primitive numeric profile.
   *
   * Both profiles are converted to doubles first.
   */
  infix fun <M: Number, OTHER: SerialPrimitiveNumberOps<M, OTHER>> pow(exp: SerialPrimitiveNumberOps<M, OTHER>) =
      map2Values(::Numbers, exp, BinaryOperation.combineOrNull { l, r, _ ->
        l.toDouble().pow(r.toDouble())
      })
  /** [(DOC)][pow] Raises this to the power of a constant number. */
  infix fun pow(n: Number) = pow(Numbers(n))
  /** [(DOC)][pow] Raises this to the power of a linear profile. */
  infix fun <OTHER: SerialLinearOps<OTHER>> pow(other: SerialLinearOps<OTHER>) = this pow other.toSerialPrimitiveNumbers("Cannot apply a non-piecewise-constant exponent.")

  /** [(DOC)][lessThan] Returns a [Booleans] that is true when this is less than another primitive numeric profile. */
  infix fun <M: Number, OTHER: SerialPrimitiveNumberOps<M, OTHER>> lessThan(other: SerialPrimitiveNumberOps<M, OTHER>) =
      map2Values(::Booleans, other, BinaryOperation.combineOrNull { l, r, _ ->
        if (l is Double || r is Double) l.toDouble() < r.toDouble()
        else if (l is Float || r is Float) l.toFloat() < r.toFloat()
        else if (l is Long || r is Long) l.toLong() < r.toLong()
        else if (l is Int || r is Int) l.toInt() < r.toInt()
        else if (l is Short || r is Short) l.toShort() < r.toShort()
        else if (l is Byte || r is Byte) l.toByte() < r.toByte()
        else throw PrimitiveNumberOps.UnreachablePrimitiveNumberException()
      })
  /** [(DOC)][lessThan] Returns a [Booleans] that is true when this is less than a constant number. */
  infix fun lessThan(n: Number) = lessThan(Numbers(n))
  /** [(DOC)][lessThan] Returns a [Booleans] that is true when this is less than a linear profile. */
  infix fun <OTHER: SerialLinearOps<OTHER>> lessThan(other: SerialLinearOps<OTHER>) = other greaterThan this

  /** [(DOC)][lessThanOrEqualTo] Returns a [Booleans] that is true when this is less than or equal to another primitive numeric profile. */
  infix fun <M: Number, OTHER: SerialPrimitiveNumberOps<M, OTHER>> lessThanOrEqualTo(other: SerialPrimitiveNumberOps<M, OTHER>) =
      map2Values(::Booleans, other, BinaryOperation.combineOrNull { l, r, _ ->
        if (l is Double || r is Double) l.toDouble() <= r.toDouble()
        else if (l is Float || r is Float) l.toFloat() <= r.toFloat()
        else if (l is Long || r is Long) l.toLong() <= r.toLong()
        else if (l is Int || r is Int) l.toInt() <= r.toInt()
        else if (l is Short || r is Short) l.toShort() <= r.toShort()
        else if (l is Byte || r is Byte) l.toByte() <= r.toByte()
        else throw PrimitiveNumberOps.UnreachablePrimitiveNumberException()
      })
  /** [(DOC)][lessThanOrEqualTo] Returns a [Booleans] that is true when this is less than or equal to a constant number. */
  infix fun lessThanOrEqual(n: Number) = lessThanOrEqualTo(Numbers(n))
  /** [(DOC)][lessThanOrEqualTo] Returns a [Booleans] that is true when this is less than or equal to a linear profile. */
  infix fun <OTHER: SerialLinearOps<OTHER>> lessThanOrEqualTo(other: SerialLinearOps<OTHER>) = other greaterThanOrEqualTo this

  /** [(DOC)][greaterThan] Returns a [Booleans] that is true when this is greater than another primitive numeric profile. */
  infix fun <M: Number, OTHER: SerialPrimitiveNumberOps<M, OTHER>> greaterThan(other: SerialPrimitiveNumberOps<M, OTHER>) =
      map2Values(::Booleans, other, BinaryOperation.combineOrNull { l, r, _ ->
        if (l is Double || r is Double) l.toDouble() > r.toDouble()
        else if (l is Float || r is Float) l.toFloat() > r.toFloat()
        else if (l is Long || r is Long) l.toLong() > r.toLong()
        else if (l is Int || r is Int) l.toInt() > r.toInt()
        else if (l is Short || r is Short) l.toShort() > r.toShort()
        else if (l is Byte || r is Byte) l.toByte() > r.toByte()
        else throw PrimitiveNumberOps.UnreachablePrimitiveNumberException()
      })
  /** [(DOC)][greaterThan] Returns a [Booleans] that is true when this is greater than a constant number. */
  infix fun greaterThan(n: Number) = greaterThan(Numbers(n))
  /** [(DOC)][greaterThan] Returns a [Booleans] that is true when this is greater than a linear profile. */
  infix fun <OTHER: SerialLinearOps<OTHER>> greaterThan(other: SerialLinearOps<OTHER>) = other lessThan this

  /** [(DOC)][greaterThanOrEqualTo] Returns a [Booleans] that is true when this is greater than or equal to another primitive numeric profile. */
  infix fun <M: Number, OTHER: SerialPrimitiveNumberOps<M, OTHER>> greaterThanOrEqualTo(other: SerialPrimitiveNumberOps<M, OTHER>) =
      map2Values(::Booleans, other, BinaryOperation.combineOrNull { l, r, _ ->
        if (l is Double || r is Double) l.toDouble() >= r.toDouble()
        else if (l is Float || r is Float) l.toFloat() >= r.toFloat()
        else if (l is Long || r is Long) l.toLong() >= r.toLong()
        else if (l is Int || r is Int) l.toInt() >= r.toInt()
        else if (l is Short || r is Short) l.toShort() >= r.toShort()
        else if (l is Byte || r is Byte) l.toByte() >= r.toByte()
        else throw PrimitiveNumberOps.UnreachablePrimitiveNumberException()
      })
  /** [(DOC)][greaterThanOrEqualTo] Returns a [Booleans] that is true when this is greater than or equal to a constant number. */
  infix fun greaterThanOrEqual(n: Number) = greaterThanOrEqualTo(Numbers(n))
  /** [(DOC)][greaterThanOrEqualTo] Returns a [Booleans] that is true when this is greater than or equal to a linear profile. */
  infix fun <OTHER: SerialLinearOps<OTHER>> greaterThanOrEqualTo(other: SerialLinearOps<OTHER>) = other lessThanOrEqualTo this
}
