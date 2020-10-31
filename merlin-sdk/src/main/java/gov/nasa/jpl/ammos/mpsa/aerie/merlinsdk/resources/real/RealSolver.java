package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Solver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

public final class RealSolver implements Solver<Double, RealDynamics, RealCondition> {
  @Override
  public Double valueAt(final RealDynamics dynamics, final Duration elapsedTime) {
    return dynamics.match(new RealDynamics.Visitor<>() {
      @Override
      public Double constant(final double value) {
        return value;
      }

      @Override
      public Double linear(final double intercept, final double slope) {
        return intercept + slope * elapsedTime.ratioOver(Duration.SECONDS);
      }
    });
  }

  @Override
  public Windows whenSatisfied(final RealDynamics dynamics, final RealCondition condition) {
    return dynamics.match(new RealDynamics.Visitor<>() {
      @Override
      public Windows constant(final double value) {
        return new Windows((condition.includesPoint(value)) ? Window.FOREVER : Window.EMPTY);
      }

      @Override
      public Windows linear(final double initial, final double changePerSecond) {
        if (changePerSecond == 0) return this.constant(initial);

        final var windows = new Windows();

        if (changePerSecond > 0) {
          for (final var interval : condition.ascendingOrder()) {
            // It starts too high (and it's going higher); no intersection.
            if (initial > interval.max) continue;

            final var start = (interval.min - initial) / changePerSecond;
            final var end = (interval.max - initial) / changePerSecond;
            final var window = Window.roundIn(start, end, Duration.SECONDS);

            windows.add(window);
          }
        } else {
          for (final var interval : condition.descendingOrder()) {
            // It starts too low (and it's going lower); no intersection.
            if (initial < interval.min) continue;

            final var start = (interval.max - initial) / changePerSecond;
            final var end = (interval.min - initial) / changePerSecond;
            final var window = Window.roundIn(start, end, Duration.SECONDS);

            windows.add(window);
          }
        }

        return windows;
      }
    });
  }
}
