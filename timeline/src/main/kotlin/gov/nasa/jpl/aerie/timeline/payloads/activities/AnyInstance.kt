package gov.nasa.jpl.aerie.timeline.payloads.activities

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.plan.AeriePostgresPlan
import kotlin.jvm.optionals.getOrNull

/** A general-purpose container for representing the arguments and computed attributes of any type of activity instance. */
data class AnyInstance(
    /***/ @JvmField val arguments: Map<String, SerializedValue>,
    /***/ @JvmField val computedAttributes: SerializedValue
) {
  /***/ companion object {
    /**
     * Converts a [SerializedValue] object containing activity arguments and computed attributes to an [AnyInstance] object.
     */
    fun deserialize(attributes: SerializedValue): AnyInstance {
      val arguments = attributes.asMap().getOrNull()!!["arguments"]?.asMap()?.getOrNull()
          ?: throw AeriePostgresPlan.DatabaseError("Could not get arguments from attributes: $attributes")
      val computedAttributes = attributes.asMap().getOrNull()!!["computedAttributes"]
          ?: throw AeriePostgresPlan.DatabaseError("Could not get computed attributes from attributes: $attributes")
      return AnyInstance(arguments, computedAttributes)
    }
  }
}
