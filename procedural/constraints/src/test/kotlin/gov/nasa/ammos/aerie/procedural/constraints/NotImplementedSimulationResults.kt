package gov.nasa.ammos.aerie.procedural.constraints

import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.ammos.aerie.procedural.timeline.collections.Instances
import gov.nasa.ammos.aerie.procedural.timeline.ops.coalesce.CoalesceSegmentsOp
import gov.nasa.ammos.aerie.procedural.timeline.payloads.Segment
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue

open class NotImplementedSimulationResults: SimulationResults {
  override fun isStale(): Boolean = TODO()
  override fun simBounds(): Interval = TODO()
  override fun <V : Any, TL : CoalesceSegmentsOp<V, TL>> resource(
    name: String,
    deserializer: (List<Segment<SerializedValue>>) -> TL
  ): TL = TODO()
  override fun <A : Any> instances(type: String?, deserializer: (SerializedValue) -> A): Instances<A> = TODO()
}
