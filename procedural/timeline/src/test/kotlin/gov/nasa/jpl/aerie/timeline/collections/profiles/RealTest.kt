package gov.nasa.jpl.aerie.timeline.collections.profiles

import gov.nasa.jpl.aerie.timeline.Duration.Companion.seconds
import gov.nasa.jpl.aerie.timeline.payloads.LinearEquation
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test

class RealTest {
  @Test
  fun plusShiftsInitialTime() {
    val result = (Real(
        Segment(seconds(0)..seconds(2), LinearEquation(seconds(0), 1.0, 1.0))
    ) + Real(
        Segment(seconds(1)..seconds(3), LinearEquation(seconds(-2), -1.0, 3.0))
    )).collect()
    assertIterableEquals(
        listOf(Segment(seconds(1)..seconds(2), LinearEquation(seconds(1), 10.0, 4.0))),
        result
    )
  }
}
