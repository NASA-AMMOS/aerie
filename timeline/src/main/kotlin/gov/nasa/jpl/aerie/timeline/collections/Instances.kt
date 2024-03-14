package gov.nasa.jpl.aerie.timeline.collections

import gov.nasa.jpl.aerie.timeline.Timeline
import gov.nasa.jpl.aerie.timeline.BaseTimeline
import gov.nasa.jpl.aerie.timeline.payloads.activities.Instance
import gov.nasa.jpl.aerie.timeline.ops.*
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceNoOp
import gov.nasa.jpl.aerie.timeline.util.preprocessList

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
  constructor(instances: List<Instance<A>>): this(BaseTimeline(::Instances, preprocessList(instances, null)))
}
