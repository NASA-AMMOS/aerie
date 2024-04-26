package gov.nasa.jpl.aerie.scheduler.plan

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.collections.Instances
import gov.nasa.jpl.aerie.timeline.durationUtils.rangeTo
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceSegmentsOp
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.payloads.activities.Instance
import gov.nasa.jpl.aerie.timeline.plan.Plan
import gov.nasa.jpl.aerie.timeline.plan.SimulationResults
import java.time.Instant
import kotlin.jvm.optionals.getOrNull
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults as MerlinSimResults

class InMemorySimulationResults(
    private val results: MerlinSimResults,
    private val stale: Boolean,
    private val plan: Plan
): SimulationResults {
  override fun isStale() = stale

  override fun simBounds(): Interval {
    val start = plan.toRelative(results.startTime)
    val end = start + results.duration
    return start .. end
  }

  companion object {
    private inline fun <D: Any> convertProfileWithoutGaps(old: List<ProfileSegment<D>>, converter: (D) -> SerializedValue): List<Segment<SerializedValue>> {
      val result: MutableList<Segment<SerializedValue>> = ArrayList(old.size)
      var elapsedTime = Duration.ZERO
      for (segment in old) {
        result.add(Segment(
            Interval.betweenClosedOpen(elapsedTime, elapsedTime + segment.extent),
            converter(segment.dynamics)
        ))
        elapsedTime += segment.extent
      }
      return result
    }
  }

  override fun <V: Any, TL: CoalesceSegmentsOp<V, TL>> resource(name: String, deserializer: (List<Segment<SerializedValue>>) -> TL): TL {
    val profile =
        if (results.discreteProfiles.containsKey(name)) convertProfileWithoutGaps(results.discreteProfiles[name]!!.right) { it }
        else if (results.realProfiles.containsKey(name)) convertProfileWithoutGaps(results.realProfiles[name]!!.right) {
          SerializedValue.of(mapOf(
              "initial" to SerializedValue.of(it.initial),
              "rate" to SerializedValue.of(it.rate)
          ))
        }
        else throw IllegalArgumentException("No such resource $name")
    return deserializer.invoke(profile)
  }

  private data class FinishedActivityAttributes(val duration: Duration, val computedAttributes: SerializedValue)
  private data class CommonActivity(
      val arguments: Map<String, SerializedValue>,
      val type: String,
      val directiveId: Long?,
      val spanId: Long,
      val startTime: Instant,
      val finishedActivityAttributes: FinishedActivityAttributes?
  )

  private val commonActivities by lazy {
    val result = mutableListOf<CommonActivity>()
    for ((key, a) in results.simulatedActivities) {
      result.add(CommonActivity(
          a.arguments,
          a.type,
          a.directiveId.map(ActivityDirectiveId::id).getOrNull(),
          key.id,
          a.start,
          FinishedActivityAttributes(a.duration, a.computedAttributes)
      ))
    }
    for ((key, a) in results.unfinishedActivities) {
      result.add(CommonActivity(
          a.arguments,
          a.type,
          a.directiveId.map(ActivityDirectiveId::id).getOrNull(),
          key.id,
          a.start,
          null
      ))
    }
    result
  }

  override fun <A: Any> instances(type: String?, deserializer: (SerializedValue) -> A): Instances<A> {
    val instances = mutableListOf<Instance<A>>()
    for (a in commonActivities) {
      if (type != null && a.type != type) continue
      val startTime = plan.toRelative(a.startTime)
      val endTime = a.finishedActivityAttributes?.let { it.duration + startTime }
          ?: simBounds().end
      val computedAttributes = a.finishedActivityAttributes?.computedAttributes ?: SerializedValue.of(mapOf())
      val serializedActivity = SerializedValue.of(mapOf(
          "arguments" to SerializedValue.of(a.arguments),
          "computedAttributes" to computedAttributes
      ))
      instances.add(Instance(
          deserializer(serializedActivity),
          a.type,
          a.spanId,
          a.directiveId,
          Interval(startTime, endTime)
      ))
    }
    return Instances(instances)
  }
}