package gov.nasa.jpl.aerie.timeline.ops.coalesce

import gov.nasa.jpl.aerie.timeline.payloads.IntervalLike
import gov.nasa.jpl.aerie.timeline.ops.GeneralOps

/** A no-op implementation of coalesce. */
interface CoalesceNoOp<V: IntervalLike<V>, THIS: CoalesceNoOp<V, THIS>>: GeneralOps<V, THIS> {
  override fun shouldCoalesce() = null
}
