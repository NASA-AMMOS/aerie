package gov.nasa.ammos.aerie.procedural.constraints

import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.ammos.aerie.procedural.timeline.collections.Instances
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Numbers
import gov.nasa.ammos.aerie.procedural.timeline.ops.coalesce.CoalesceSegmentsOp
import gov.nasa.ammos.aerie.procedural.timeline.payloads.Segment
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults
import gov.nasa.ammos.aerie.procedural.timeline.util.duration.rangeTo
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration.seconds
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import org.junit.jupiter.api.Assertions.assertIterableEquals
import kotlin.test.Test

class GeneratorTest: GeneratorConstraint() {
  override fun generate(plan: Plan, simResults: SimulationResults) {
    simResults.resource("/plant", Numbers.deserializer())
      .greaterThan(0)
      .violateOn(false)
  }

  override fun message() = "Plant must be greater than 0"

  @Test
  fun testGenerator() {
    val plan = NotImplementedPlan()
    val simResults = object : SimulationResults {
      override fun isStale(): Boolean {
        TODO("Not yet implemented")
      }

      override fun simBounds(): Interval {
        TODO("Not yet implemented")
      }

      override fun <V : Any, TL : CoalesceSegmentsOp<V, TL>> resource(
        name: String,
        deserializer: (List<Segment<SerializedValue>>) -> TL
      ): TL {
        if (name == "/plant") {
          val list = listOf(
            Segment(seconds(0) .. seconds(1), SerializedValue.of(3)),
            Segment(seconds(1) .. seconds(2), SerializedValue.of(-1)),
          )
          return deserializer(list)
        } else {
          TODO("Not yet implemented")
        }
      }

      override fun <A : Any> instances(type: String?, deserializer: (SerializedValue) -> A): Instances<A> {
        TODO("Not yet implemented")
      }
    }

    val result = run(plan, simResults).collect()

    assertIterableEquals(
      listOf(Violation(seconds(1) .. seconds(2))),
      result
    )
  }
}
