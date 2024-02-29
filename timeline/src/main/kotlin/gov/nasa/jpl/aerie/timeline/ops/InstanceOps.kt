package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.payloads.activities.Instance

/**
 * Operations mixin for timelines of activity instances.
 *
 * Used for both generic instances, and specific instance types from the mission model.
 */
interface InstanceOps<A: Any, THIS: InstanceOps<A, THIS>>: ActivityOps<Instance<A>, THIS>, NonZeroDurationOps<Instance<A>, THIS>
