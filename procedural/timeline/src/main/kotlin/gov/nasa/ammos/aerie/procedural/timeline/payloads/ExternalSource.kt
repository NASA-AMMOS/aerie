package gov.nasa.ammos.aerie.procedural.timeline.payloads

import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue

/**
 * An external source instance. Used for querying purposes - see EventQuery.kt.
 * The included fields represent the primary key used to identify External Sources.
 */
data class ExternalSource(
  /** The string name of this source. */
  val key: String,
  /** The derivation group that this source is a member of. */
  val derivationGroup: String,
)
