package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;
import org.junit.Test;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.MILLISECOND;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.MILLISECONDS;
import static org.junit.Assert.assertEquals;

public final class RealSolverTest {
  @Test
  public void testLinearSampling() {
    final var solver = new RealSolver();

    final var value = solver.valueAt(
        RealDynamics.linear(0.0, 10.0),
        Duration.of(2575, MILLISECOND));

    final var expected = 25.75;

    assertEquals(expected, value, 0.00000001);
  }

  @Test
  public void testLinearConditions() {
    final var solver = new RealSolver();

    final var windows = solver.whenSatisfied(
        RealDynamics.linear(0.0, 10.0),
        new RealCondition(
            ClosedInterval.between(1.0, 3.0),
            ClosedInterval.at(15.0),
            ClosedInterval.between(50.0, 75.0)),
        Window.FOREVER);

    final var expected = new Windows(
        Window.between(100, 300, MILLISECONDS),
        Window.at(1500, MILLISECONDS),
        Window.between(5000, 7500, MILLISECONDS));

    assertEquals(expected, windows);
  }
}
