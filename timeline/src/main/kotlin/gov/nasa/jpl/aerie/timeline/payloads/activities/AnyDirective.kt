package gov.nasa.jpl.aerie.timeline.payloads.activities

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue

/** A general-purpose container for representing the arguments any type of activity directive. */
data class AnyDirective(
    /***/ val arguments: Map<String, SerializedValue>
)
