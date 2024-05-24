package gov.nasa.ammos.aerie.timeline.collections

import gov.nasa.ammos.aerie.timeline.Timeline
import gov.nasa.ammos.aerie.timeline.BaseTimeline
import gov.nasa.ammos.aerie.timeline.payloads.activities.Instance
import gov.nasa.ammos.aerie.timeline.ops.*
import gov.nasa.ammos.aerie.timeline.ops.coalesce.CoalesceNoOp
import gov.nasa.ammos.aerie.timeline.util.preprocessList

/**
 * A timeline of activity instances.
 *
 * @param A the inner payload of the [Instance] type.
 */
data class Instances<A: Any>(private val timeline: Timeline<Instance<A>, Instances<A>>):
    Timeline<Instance<A>, Instances<A>> by timeline,
    NonZeroDurationOps<Instance<A>, Instances<A>>,
    ActivityOps<Instance<A>, Instances<A>>
{
  constructor(vararg instances: Instance<A>): this(instances.asList())
  constructor(instances: List<Instance<A>>): this(
      gov.nasa.ammos.aerie.timeline.BaseTimeline(
          ::Instances,
          preprocessList(instances, null)
      )
  )
}
