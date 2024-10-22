package gov.nasa.ammos.aerie.procedural.timeline.payloads

import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue

/** An external event instance. */
data class ExternalEvent(
  /** The string name of this event. */
  val key: String,
  /** The type of the event. */
  val type: String,
  /** The source this event comes from. */
  val source: String,
  /** The derivation group that this event comes from. */
  val derivationGroup: String,
  override val interval: Interval,
): IntervalLike<ExternalEvent> {
  override fun withNewInterval(i: Interval) = ExternalEvent(key, type, source, derivationGroup, i)
}
