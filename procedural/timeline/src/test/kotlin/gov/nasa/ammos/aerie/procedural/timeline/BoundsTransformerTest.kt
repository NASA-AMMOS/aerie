package gov.nasa.ammos.aerie.procedural.timeline

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration.seconds
import gov.nasa.ammos.aerie.procedural.timeline.Interval.Companion.between
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BoundsTransformerTest {
  @Test
  fun shift() {
    val transformer = BoundsTransformer.Companion.shift(seconds(1))

    assertEquals(
        between(seconds(-1), seconds(1)),
        transformer(between(seconds(0), seconds(2)))
    )
  }
}
