package gov.nasa.jpl.aerie.scheduler.goals

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.collections.Directives
import gov.nasa.jpl.aerie.timeline.durationUtils.minus
import gov.nasa.jpl.aerie.timeline.durationUtils.plus
import java.time.Instant
import gov.nasa.jpl.aerie.timeline.plan.Plan as TimelinePlan
import gov.nasa.jpl.aerie.scheduler.model.Plan  as SchedulerPlan

data class InMemoryPlan(
    private val schedulerPlan: SchedulerPlan,
    private val planningHorizon: PlanningHorizon,
): TimelinePlan, SchedulerPlan by schedulerPlan {
  override fun totalBounds() = Interval.between(Duration.ZERO, planningHorizon.aerieHorizonDuration)

  override fun toRelative(abs: Instant) = abs - planningHorizon.startInstant

  override fun toAbsolute(rel: Duration) = planningHorizon.startInstant + rel

  override fun <A : Any> directives(type: String?, deserializer: (SerializedValue) -> A): Directives<A> {
    TODO("Not yet implemented")
  }

}
