package gov.nasa.ammos.aerie.timeline.collections

import gov.nasa.ammos.aerie.timeline.BaseTimeline
import gov.nasa.ammos.aerie.timeline.payloads.activities.Directive
import gov.nasa.ammos.aerie.timeline.Timeline
import gov.nasa.ammos.aerie.timeline.ops.ActivityOps
import gov.nasa.ammos.aerie.timeline.util.preprocessList

/**
 * A timeline of activity directives.
 *
 * @param A the inner payload of the [Directive] type.
 */
data class Directives<A: Any>(private val timeline: Timeline<Directive<A>, Directives<A>>):
    Timeline<Directive<A>, Directives<A>> by timeline,
    ActivityOps<Directive<A>, Directives<A>>
{
  constructor(vararg directives: Directive<A>): this(directives.asList())
  constructor(directives: List<Directive<A>>): this(
      gov.nasa.ammos.aerie.timeline.BaseTimeline(
          ::Directives,
          preprocessList(directives, null)
      )
  )
}
