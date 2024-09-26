package gov.nasa.ammos.aerie.procedural.timeline.util

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration.seconds
import gov.nasa.ammos.aerie.procedural.timeline.CollectOptions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import gov.nasa.ammos.aerie.procedural.timeline.Interval.Companion.between
import gov.nasa.ammos.aerie.procedural.timeline.collections.Universal
import gov.nasa.ammos.aerie.procedural.timeline.util.duration.rangeTo

val bounds = (seconds(0) .. seconds(10))

class ListCollectorTest {
  @Test
  fun isNoopOnArrayInBounds() {
    val input = listOf(
        seconds(2) .. seconds(3),
        seconds(4) .. seconds(5),
    )
    val result = Universal(input).collect(bounds)

    assertIterableEquals(input, result)
  }

  @Test
  fun removeSegmentOutOfBounds() {
    val input = listOf(
        seconds(2) .. seconds(3),
        seconds(14) .. seconds(15),
    )
    val result = Universal(input).collect(bounds)

    val expected = listOf(
        seconds(2) .. seconds(3),
    )

    assertIterableEquals(expected, result)
  }

  @Test
  fun splitSegmentOnBounds() {
    val input = listOf(
        seconds(2) .. seconds(3),
        seconds(9) .. seconds(11),
    )
    val result = Universal(input).collect(bounds)

    val expected = listOf(
        seconds(2) .. seconds(3),
        (seconds(9) .. seconds(10))
    )

    assertIterableEquals(expected, result)
  }

  @Test
  fun dontTruncateMarginal() {
    val input = listOf(
        between(seconds(-3), seconds(-2)),
        between(seconds(-1), seconds(1)),
        seconds(4) .. seconds(5),
        seconds(9) .. seconds(11),
        seconds(13) .. seconds(14)
    )

    val result = Universal(input).collect(CollectOptions(bounds, false))

    val expected = listOf(
        between(seconds(-1), seconds(1)),
        seconds(4) .. seconds(5),
        seconds(9) .. seconds(11),
    )

    assertIterableEquals(expected, result)
  }
}
