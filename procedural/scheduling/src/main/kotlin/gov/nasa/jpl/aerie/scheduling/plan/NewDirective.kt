package gov.nasa.jpl.aerie.scheduling.plan

import gov.nasa.jpl.aerie.timeline.Duration
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyDirective
import gov.nasa.jpl.aerie.timeline.payloads.activities.Directive

data class NewDirective(
    val inner: AnyDirective,
    val name: String,
    val type: String,
    val startTime: Duration
) {
  fun withId(id: Long) = Directive(
      inner,
      name,
      id,
      type,
      startTime
  )
}
