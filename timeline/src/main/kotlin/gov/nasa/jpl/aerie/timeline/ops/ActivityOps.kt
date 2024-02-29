package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.payloads.activities.Activity

/**
 * Operations mixin for timelines of activities.
 */
interface ActivityOps<A: Activity<A>, THIS: ActivityOps<A, THIS>>: GeneralOps<A, THIS>
