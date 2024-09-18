package gov.nasa.ammos.aerie.procedural.timeline.plan

/** Fields for filtering events as they are queried. */
data class EventQuery(
  /** The derivation group the events belong to. */
  val derivationGroup: String?,

  /** The event type to query for. */
  val type: String?,

  /** The event source the event came from. */
  val source: String?,
)
