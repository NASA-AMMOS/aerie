package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.payloads.activities.Directive

/**
 * Operations mixin for timelines of activity directives.
 *
 * Used for both generic directives, and specific directive types from the mission model.
 */
interface DirectiveOps<A: Any, THIS: DirectiveOps<A, THIS>>: ActivityOps<Directive<A>, THIS>
