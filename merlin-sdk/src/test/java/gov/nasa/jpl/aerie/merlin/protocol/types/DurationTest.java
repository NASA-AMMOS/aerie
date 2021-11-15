package gov.nasa.jpl.aerie.merlin.protocol.types;

import org.junit.jupiter.api.Test;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.EPSILON;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MILLISECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.roundDownward;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.roundNearest;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.roundUpward;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DurationTest {
  @Test
  public void testRoundNearest() {
    assertEquals(duration(2756, MILLISECONDS), roundNearest(2.756, SECONDS));
    assertEquals(duration(2756, MILLISECONDS), roundNearest(Math.nextUp(2.756), SECONDS));
    assertEquals(duration(2756, MILLISECONDS), roundNearest(Math.nextDown(2.756), SECONDS));
  }

  @Test
  public void testRoundDown() {
    assertEquals(duration(2756, MILLISECONDS), roundNearest(2.756, SECONDS));
    assertEquals(duration(2756, MILLISECONDS), roundDownward(Math.nextUp(2.756), SECONDS));
    assertEquals(duration(2756, MILLISECONDS).minus(EPSILON), roundDownward(Math.nextDown(2.756), SECONDS));
  }

  @Test
  public void testRoundUp() {
    assertEquals(duration(2756, MILLISECONDS), roundNearest(2.756, SECONDS));
    assertEquals(duration(2756, MILLISECONDS), roundUpward(Math.nextDown(2.756), SECONDS));
    assertEquals(duration(2756, MILLISECONDS).plus(EPSILON), roundUpward(Math.nextUp(2.756), SECONDS));
  }
}
