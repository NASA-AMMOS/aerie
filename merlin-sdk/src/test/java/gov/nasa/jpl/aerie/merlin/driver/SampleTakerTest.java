package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.List;

import static gov.nasa.jpl.aerie.time.Duration.MILLISECONDS;
import static gov.nasa.jpl.aerie.time.Duration.SECOND;
import static gov.nasa.jpl.aerie.time.Duration.SECONDS;
import static gov.nasa.jpl.aerie.time.Duration.duration;
import static org.junit.Assert.assertEquals;

public final class SampleTakerTest {
  @Test
  public void smokeTest() {
    final var profile =
        new Profile<>(new RealResourceSolver<>())
            .append(duration(0, SECONDS), RealDynamics.linear(5.0, 0.0))
            .append(duration(1, SECOND), RealDynamics.linear(0.0, 4.0))
            .append(duration(1, SECOND), RealDynamics.linear(8.0, 0.0));

    final var timestamps = List.of(
        duration(0, MILLISECONDS),
        duration(250, MILLISECONDS),
        duration(500, MILLISECONDS),
        duration(750, MILLISECONDS),
        duration(1000, MILLISECONDS),
        duration(1250, MILLISECONDS));

    final var expected = List.of(
        Pair.of(duration(0, MILLISECONDS), SerializedValue.of(5.0)),
        Pair.of(duration(0, MILLISECONDS), SerializedValue.of(5.0)),
        Pair.of(duration(0, MILLISECONDS), SerializedValue.of(0.0)),
        Pair.of(duration(250, MILLISECONDS), SerializedValue.of(1.0)),
        Pair.of(duration(500, MILLISECONDS), SerializedValue.of(2.0)),
        Pair.of(duration(750, MILLISECONDS), SerializedValue.of(3.0)),
        Pair.of(duration(1000, MILLISECONDS), SerializedValue.of(4.0)),
        Pair.of(duration(1000, MILLISECONDS), SerializedValue.of(8.0)),
        Pair.of(duration(1250, MILLISECONDS), SerializedValue.of(8.0)));

    final var samples = SampleTaker.sample(profile, timestamps);

    assertEquals(expected, samples);
  }
}
