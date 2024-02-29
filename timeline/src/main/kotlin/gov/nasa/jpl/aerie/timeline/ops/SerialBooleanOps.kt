package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.BinaryOperation
import gov.nasa.jpl.aerie.timeline.Duration
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.collections.profiles.Real
import gov.nasa.jpl.aerie.timeline.payloads.LinearEquation

/**
 * Operations mixin for timelines of booleans.
 */
interface SerialBooleanOps<THIS: SerialBooleanOps<THIS>>: SerialConstantOps<Boolean, THIS>, BooleanOps<THIS> {
  /** [(DOC)][and] Computes the AND operation between two boolean profiles. */
  infix fun <OTHER: SerialBooleanOps<OTHER>> and(other: SerialBooleanOps<OTHER>) = map2Values(other, BinaryOperation.cases(
      { l, _ -> if (l) null else false },
      { r, _ -> if (r) null else false },
      { l, r, _ -> l && r }
  ))

  /** [(DOC)][or] Computes the OR operation between two boolean profiles. */
  infix fun <OTHER: SerialBooleanOps<OTHER>> or(other: SerialBooleanOps<OTHER>) = map2Values(other, BinaryOperation.cases(
      { l, _ -> if (l) true else null },
      { r, _ -> if (r) true else null },
      { l, r, _ -> l || r }
  ))

  /** [(DOC)][xor] Computes the XOR operation between two boolean profiles. */
  infix fun <OTHER: SerialBooleanOps<OTHER>> xor(other: SerialBooleanOps<OTHER>) = map2Values(other, BinaryOperation.combineOrNull { l, r, _ -> l.xor(r) })

  /** [(DOC)][nor] the NOR operation between two boolean profiles. */
  infix fun <OTHER: SerialBooleanOps<OTHER>> nor(other: SerialBooleanOps<OTHER>) = map2Values(other, BinaryOperation.cases(
      { l, _ -> if (l) false else null },
      { r, _ -> if (r) false else null },
      { l, r, _ -> !(l || r) }
  ))

  /** [(DOC)][nand] Computes the NAND operation between two boolean profiles. */
  infix fun <OTHER: SerialBooleanOps<OTHER>> nand(other: SerialBooleanOps<OTHER>) = map2Values(other, BinaryOperation.cases(
      { l, _ -> if (l) null else true },
      { r, _ -> if (r) null else true },
      { l, r, _ -> !(l && r) }
  ))

  /**
   * [(DOC)][shiftEdges] Shifts the rising and falling edges of a boolean profile independently of each other.
   *
   * This allows for segments to not just be shifted around, but stretched or squished, or even
   * deleted.
   *
   * A rising edge is defined as the time just after a `false` segment ends - whether it meets a `true`
   * segment or a gap. Similarly, a falling edge is just after a `true` segment ends.
   *
   * @param shiftRising duration to shift the rising edges by
   * @param shiftFalling duration to shift the rising edges by
   */
  fun shiftEdges(shiftRising: Duration, shiftFalling: Duration) =
      unsafeMapIntervals(
          { i ->
            Interval.between(
                Duration.min(i.start.saturatingMinus(shiftRising), i.start.saturatingMinus(shiftFalling)),
                Duration.max(i.end.saturatingMinus(shiftRising), i.end.saturatingMinus(shiftFalling)),
                i.startInclusivity,
                i.endInclusivity
            )
          },
          true
      ) { t ->
        if (t.value) t.interval.shiftBy(shiftRising, shiftFalling)
        else t.interval.shiftBy(shiftFalling, shiftRising)
      }

  /**
   * [(DOC)][accumulatedTrueDuration] Creates a Real profile corresponding to the running total of time
   * that this profile has spent `true`.
   *
   * @param unit base unit of time to count. As in, the resulting real profile will increase by
   *             `1` for each `unit` duration spent in the `true` state.
   *
   * @see gov.nasa.jpl.aerie.timeline.ops.numeric.SerialNumericOps.integrate for further explanation of [unit].
   */
  fun accumulatedTrueDuration(unit: Duration) =
      mapValues(::Real) { LinearEquation(if (it.value) 1.0 else 0.0) }.integrate(unit)

  /**
   * [(DOC)][rollingTrueDuration] Calculates the sum of durations of true segments in a range leading the current time.
   *
   * This returns a real profile that equals, at each time `t`, the duration of true segments in the interval `[t, t+range]`.
   *
   * Real profiles can't actually represent durations, only unitless numbers, so the result is actually calculated
   * as a multiple of the provided [unit].
   *
   * Because this is a serial profile, the duration of true segments in the look-ahead range can't exceed [range] itself.
   * So the result is bounded by `[0, range/unit]`
   *
   * @param range how far into the future to look
   * @param unit the time basis vector of the result; the unit of time that the result counts.
   */
  fun rollingTrueDuration(range: Duration, unit: Duration) =
      accumulatedTrueDuration(unit).shiftedDifference(range)

  /** [(DOC)][risingEdges] Detects when this transitions from false to true. */
  fun risingEdges() = transitions(from = false, to = true)

  /** [(DOC)][fallingEdges] Detects when this transitions from false to true. */
  fun fallingEdges() = transitions(from = true, to = false)
}
