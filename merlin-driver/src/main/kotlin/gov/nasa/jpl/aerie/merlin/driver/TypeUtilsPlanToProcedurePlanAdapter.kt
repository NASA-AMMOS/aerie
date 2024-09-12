package gov.nasa.jpl.aerie.merlin.driver

import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.ammos.aerie.procedural.timeline.collections.Directives
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.Directive
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.DirectiveStart
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan
import gov.nasa.ammos.aerie.procedural.timeline.util.duration.minus
import gov.nasa.ammos.aerie.procedural.timeline.util.duration.plus
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import java.time.Instant
import gov.nasa.jpl.aerie.types.Plan as TypeUtilsPlan;

data class TypeUtilsPlanToProcedurePlanAdapter(val plan: TypeUtilsPlan): Plan {
  override fun totalBounds() = Interval.between(Duration.ZERO, plan.duration())

  override fun toRelative(abs: Instant) = abs - plan.planStartInstant()

  override fun toAbsolute(rel: Duration) = plan.planStartInstant() + rel

  override fun <A : Any> directives(type: String?, deserializer: (SerializedValue) -> A): Directives<A> {
    val activities = if (type == null) plan.activityDirectives().entries.toList()
    else plan.activityDirectives().entries.filter { it.value.serializedActivity.typeName == type }.toList()

    return Directives(
      activities.map { Directive(
        deserializer(SerializedValue.of(it.value.serializedActivity.arguments)),
        "Name unavailable",
        it.key,
        it.value.serializedActivity.typeName,
        if (it.value.anchorId == null) {
          DirectiveStart.Absolute(it.value.startOffset)
        } else {
          DirectiveStart.Anchor(
            it.value.anchorId,
            it.value.startOffset,
            if (it.value.anchoredToStart) DirectiveStart.Anchor.AnchorPoint.Start
            else DirectiveStart.Anchor.AnchorPoint.End
          )
        }
      ) }
    )
  }
}
