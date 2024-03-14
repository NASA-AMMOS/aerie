package gov.nasa.jpl.aerie.scheduling.plan

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.Duration
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.collections.Instances
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceSegmentsOp
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.plan.Plan
import gov.nasa.jpl.aerie.timeline.plan.SimulationResults

class AerieInMemorySimulationResults(
    private val driverResults: gov.nasa.jpl.aerie.merlin.driver.SimulationResults,
    private val stale: Boolean,
    private val plan: Plan
): SimulationResults {
  override fun isStale() = stale

  override fun simBounds(): Interval {
    val start = plan.toRelative(driverResults.startTime)
    val end = start + Duration(driverResults.duration.dividedBy(gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECOND))
    return start .. end
  }

  override fun <V: Any, TL: CoalesceSegmentsOp<V, TL>> resource(name: String, deserializer: (List<Segment<SerializedValue>>) -> TL): TL {
    TODO("Not yet implemented")
  }

  override fun <A: Any> instances(type: String?, deserializer: (SerializedValue) -> A): Instances<A> {
    TODO("Not yet implemented")
  }
}
