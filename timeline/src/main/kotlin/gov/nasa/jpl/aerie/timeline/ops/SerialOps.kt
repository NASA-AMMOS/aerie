package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.payloads.IntervalLike

interface SerialOps<T: IntervalLike<T>, THIS: SerialOps<T, THIS>>: GeneralOps<T, THIS> {
  override fun isAlwaysSorted() = true
}
