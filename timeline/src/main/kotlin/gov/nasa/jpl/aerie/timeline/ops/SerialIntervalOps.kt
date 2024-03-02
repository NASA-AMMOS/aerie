package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.*
import gov.nasa.jpl.aerie.timeline.collections.Intervals
import gov.nasa.jpl.aerie.timeline.collections.Windows
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceIntervalsOp
import gov.nasa.jpl.aerie.timeline.util.sorted

/** Operations for coalescing intervals. */
interface SerialIntervalOps<THIS: SerialIntervalOps<THIS>>: SerialOps<Interval, THIS>, CoalesceIntervalsOp<THIS> {

  /** [(DOC)][union] Calculates the union of this and another [Windows]. */
  infix fun <OTHER: SerialIntervalOps<OTHER>> union(other: SerialIntervalOps<OTHER>) = unsafeOperate { opts ->
    val combined = collect(opts) + other.collect(opts)
    combined.sorted()
  }

  /** [(DOC)][intersection] Calculates the intersection of this and another [Windows]. */
  infix fun <OTHER: SerialIntervalOps<OTHER>> intersection(other: SerialIntervalOps<OTHER>) =
      unsafeMap2(::Windows, other) { _, _, i -> i }

  /** [(DOC)][complement] Calculates the complement; i.e. highlights everything that is not highlighted in this timeline. */
  fun complement() = unsafeOperate { opts ->
    val result = mutableListOf(opts.bounds)
    for (interval in collect(opts)) {
      result += result.removeLast() - interval
    }
    result
  }
}
