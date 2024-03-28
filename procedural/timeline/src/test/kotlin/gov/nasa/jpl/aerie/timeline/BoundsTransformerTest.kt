package gov.nasa.jpl.aerie.timeline

import gov.nasa.jpl.aerie.timeline.Duration.Companion.seconds
import gov.nasa.jpl.aerie.timeline.Interval.Companion.between
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BoundsTransformerTest {
  @Test
  fun shift() {
    val transformer = BoundsTransformer.shift(seconds(1))

    assertEquals(
        between(seconds(-1), seconds(1)),
        transformer(between(seconds(0), seconds(2)))
    )
  }
}
