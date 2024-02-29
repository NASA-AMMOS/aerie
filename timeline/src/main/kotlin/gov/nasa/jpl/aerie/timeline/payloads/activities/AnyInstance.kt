package gov.nasa.jpl.aerie.timeline.payloads.activities

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue

/** A general-purpose container for representing the arguments and computed attributes of any type of activity instance. */
data class AnyInstance(
    /***/ val arguments: Map<String, SerializedValue>,
    /***/ val computedAttributes: Map<String, SerializedValue>
)
