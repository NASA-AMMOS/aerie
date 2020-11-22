package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Solver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

public final class RealSolver implements Solver<Double, RealDynamics, RealCondition> {
  @Override
  public Double valueAt(final RealDynamics dynamics, final Duration elapsedTime) {
    return dynamics.initial + dynamics.rate * elapsedTime.ratioOver(Duration.SECONDS);
  }

  @Override
  public Windows whenSatisfied(final RealDynamics dynamics, final RealCondition condition) {
    final var windows = new Windows();

    if (dynamics.rate == 0) {
      windows.add((condition.includesPoint(dynamics.initial)) ? Window.FOREVER : Window.EMPTY);
    } else if (dynamics.rate > 0) {
      for (final var interval : condition.ascendingOrder()) {
        // It starts too high (and it's going higher); no intersection.
        if (dynamics.initial > interval.max) continue;

        final var start = (interval.min - dynamics.initial) / dynamics.rate;
        final var end = (interval.max - dynamics.initial) / dynamics.rate;
        final var window = Window.roundIn(start, end, Duration.SECONDS);

        windows.add(window);
      }
    } else {
      for (final var interval : condition.descendingOrder()) {
        // It starts too low (and it's going lower); no intersection.
        if (dynamics.initial < interval.min) continue;

        final var start = (interval.max - dynamics.initial) / dynamics.rate;
        final var end = (interval.min - dynamics.initial) / dynamics.rate;
        final var window = Window.roundIn(start, end, Duration.SECONDS);

        windows.add(window);
      }
    }

    return windows;
  }
}
