package gov.nasa.ammos.aerie.procedural.scheduling.plan

import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.AnyDirective
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.Directive

/**
 * Edits that can be made to the plan.
 *
 * Currently only creating new activities is supported.
 */
sealed interface Edit {
  /** Create a new activity from a given directive. */
  data class Create(/***/ val directive: Directive<AnyDirective>): Edit
}
