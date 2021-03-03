package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
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
    final var profile = List.of(
        Pair.of(
            Window.at(Duration.ZERO),
            RealDynamics.linear(5.0, 0.0)),
        Pair.of(
            Window.between(Duration.ZERO, duration(1, SECOND)),
            RealDynamics.linear(0.0, 4.0)),
        Pair.of(
            Window.between(duration(1, SECOND), duration(2, SECONDS)),
            RealDynamics.linear(8.0, 0.0)));

    final var expected = List.of(
        Pair.of(duration(0, MILLISECONDS), SerializedValue.of(5.0)),
        Pair.of(duration(0, MILLISECONDS), SerializedValue.of(0.0)),
        Pair.of(duration(1000, MILLISECONDS), SerializedValue.of(4.0)),
        Pair.of(duration(1000, MILLISECONDS), SerializedValue.of(8.0)),
        Pair.of(duration(2000, MILLISECONDS), SerializedValue.of(8.0)));

    final var samples = SampleTaker.sample(profile, new RealResourceSolver<>(Scoped.create()));

    assertEquals(expected, samples);
  }
}
