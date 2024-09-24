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
   * A nullable list of types; the event must belong to one of them if present.
   *
   * If null, all types are allowed.
   */
  val types: List<String>?,

  /**
   * A nullable list of sources; the event must belong to one of them if present.
   *
   * If null, all sources are allowed.
   */
  val sources: List<String>?,
) {
  constructor(derivationGroup: String?, type: String?, source: String?): this(
    derivationGroup?.let { listOf(it) },
    type?.let { listOf(it) },
    source?.let { listOf(it) }
  )
  constructor(): this(null as String?, null, null)
}
