package gov.nasa.ammos.aerie.procedural.timeline.util

import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration.seconds
import gov.nasa.ammos.aerie.procedural.timeline.Interval.Companion.at
import gov.nasa.ammos.aerie.procedural.timeline.Interval.Companion.between
import gov.nasa.ammos.aerie.procedural.timeline.Interval.Inclusivity.*
import gov.nasa.ammos.aerie.procedural.timeline.payloads.Segment
import gov.nasa.ammos.aerie.procedural.timeline.util.duration.rangeTo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CoalesceTest {

  @Test
  fun isNoopOnAlreadyCoalescedList() {
    fun supplier() = listOf(
        Segment(between(seconds(0), seconds(1), Inclusive, Exclusive), false),
        Segment(between(seconds(1), seconds(2), Inclusive, Inclusive), true),
        Segment(between(seconds(2), seconds(3), Exclusive, Inclusive), false),
    )

    val expected = supplier()
    val result = coalesceList(supplier(), Segment<Boolean>::valueEquals)

    assertIterableEquals(expected, result)
  }

  @Test
  fun removeEmptySegment() {
    val result = coalesceList(listOf(
        Segment(at(seconds(1)), false),
        Segment(seconds(1) .. seconds(2), true)
    ), Segment<Boolean>::valueEquals)

    val expected = listOf(Segment(seconds(1) .. seconds(2), true))

    assertIterableEquals(expected, result)
  }

  @Test
  fun allIntervalsEmpty() {
    val result = coalesceList(listOf(
      Segment(Interval.EMPTY, false),
      Segment(seconds(1) .. seconds(-1), true)
    ), Segment<Boolean>::valueEquals)

    val expected = listOf<Segment<Boolean>>()

    assertIterableEquals(expected, result)
  }
}
