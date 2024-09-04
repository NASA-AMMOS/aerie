package gov.nasa.ammos.aerie.procedural.constraints

import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.ammos.aerie.procedural.timeline.collections.Directives
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import java.time.Instant

class NotImplementedPlan: Plan {
  override fun totalBounds(): Interval {
    TODO("Not yet implemented")
  }

  override fun toRelative(abs: Instant): Duration {
    TODO("Not yet implemented")
  }

  override fun toAbsolute(rel: Duration): Instant {
    TODO("Not yet implemented")
  }

  override fun <A : Any> directives(type: String?, deserializer: (SerializedValue) -> A): Directives<A> {
    TODO("Not yet implemented")
  }
}
