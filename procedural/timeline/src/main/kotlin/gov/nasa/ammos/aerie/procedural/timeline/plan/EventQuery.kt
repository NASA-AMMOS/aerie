package gov.nasa.ammos.aerie.procedural.timeline.plan

/** Fields for filtering events as they are queried. */
data class EventQuery(
  /**
   * A nullable list of derivation groups; the event must belong to one of them if present.
   *
   * If null, all derivation groups are allowed.
   */
  val derivationGroups: List<String>?,

  /**
   * A nullable list of eventTypes; the event must belong to one of them if present.
   *
   * If null, all types are allowed.
   */
  val eventTypes: List<String>?,

  /**
   * A nullable list of sources; the event must belong to one of them if present.
   *
   * If null, all sources are allowed.
   */
  val sources: List<String>?,
) {
  constructor(derivationGroup: String?, eventType: String?, source: String?): this(
    derivationGroup?.let { listOf(it) },
    eventType?.let { listOf(it) },
    source?.let { listOf(it) }
  )
  constructor(): this(null as String?, null, null)
}
