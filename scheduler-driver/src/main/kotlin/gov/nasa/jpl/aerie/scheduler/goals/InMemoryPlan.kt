package gov.nasa.jpl.aerie.scheduler.goals

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.scheduler.model.ActivityType
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.collections.Directives
import gov.nasa.jpl.aerie.timeline.durationUtils.minus
import gov.nasa.jpl.aerie.timeline.durationUtils.plus
import gov.nasa.jpl.aerie.timeline.payloads.activities.Directive
import gov.nasa.jpl.aerie.timeline.payloads.activities.DirectiveStart
import gov.nasa.jpl.aerie.timeline.payloads.activities.DirectiveStart.Anchor.AnchorPoint.Companion.anchorToStart
import java.time.Instant
import gov.nasa.jpl.aerie.scheduler.model.Plan as SchedulerPlan
import gov.nasa.jpl.aerie.timeline.plan.Plan as TimelinePlan

data class InMemoryPlan(
    private val schedulerPlan: SchedulerPlan,
    private val planningHorizon: PlanningHorizon,
): TimelinePlan, SchedulerPlan by schedulerPlan {
  override fun totalBounds() = Interval.between(Duration.ZERO, planningHorizon.aerieHorizonDuration)

  override fun toRelative(abs: Instant) = abs - planningHorizon.startInstant

  override fun toAbsolute(rel: Duration) = planningHorizon.startInstant + rel

  override fun <A : Any> directives(type: String?, deserializer: (SerializedValue) -> A): Directives<A> {
    val schedulerActivities = (if (type == null) schedulerPlan.activities else schedulerPlan.activitiesByType[ActivityType(type)])
        ?: throw Exception("could not find activities by type $type")

    val result = ArrayList<Directive<A>>(schedulerActivities.size)
    for (activity in schedulerActivities) {
      result.add(
          Directive(
              deserializer(SerializedValue.of(activity.arguments)),
              "Name unavailable",
              activity.id.id,
              activity.type.name,
              if (activity.anchorId == null) DirectiveStart.Absolute(activity.startOffset)
              else DirectiveStart.Anchor(activity.anchorId.id, activity.startOffset, anchorToStart(activity.anchoredToStart))
          )
      )
    }
    return Directives(result)
  }

}
