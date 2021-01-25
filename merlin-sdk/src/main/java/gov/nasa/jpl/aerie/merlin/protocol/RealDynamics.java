package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealCondition;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import gov.nasa.jpl.aerie.time.Windows;

import java.util.Optional;

/**
 * A description of a time-dependent behavior for real-valued resources that may vary continuously.
 *
 * <p>
 *   This class currently only supports constant and linear dynamics, but we hope to add more in the future.
 * </p>
 */
public final class RealDynamics {
  public final double initial;
  public final double rate;

  private RealDynamics(final double initial, final double rate) {
    this.initial = initial;
    this.rate = rate;
  }

  public static RealDynamics constant(final double initial) {
    return new RealDynamics(initial, 0.0);
  }

  public static RealDynamics linear(final double initial, final double rate) {
    return new RealDynamics(initial, rate);
  }


  public final RealDynamics scaledBy(final double scalar) {
    return linear(this.initial * scalar, this.rate * scalar);
  }

  public final RealDynamics plus(final RealDynamics other) {
    return linear(this.initial + other.initial, this.rate + other.rate);
  }

  public final RealDynamics minus(final RealDynamics other) {
    return this.plus(other.scaledBy(-1.0));
  }


  public Optional<Duration> whenSatisfies(final RealCondition condition, final Window selection) {
    for (final var window : this.getSatisfyingWindows(condition, selection)) {
      return Optional.of(window.start);
    }

    return Optional.empty();
  }

  public Optional<Duration> whenDissatisfies(final RealCondition condition, final Window selection) {
    final var results = new Windows(selection);
    results.subtractAll(this.getSatisfyingWindows(condition, selection));

    for (final var window : results) {
      return Optional.of(window.start);
    }

    return Optional.empty();
  }

  public Windows getSatisfyingWindows(final RealCondition condition, final Window selection) {
    final var windows = new Windows();

    if (this.rate == 0) {
      windows.add((condition.includesPoint(this.initial)) ? selection : Window.EMPTY);
    } else if (this.rate > 0) {
      for (final var interval : condition.ascendingOrder()) {
        // It starts too high (and it's going higher); no intersection.
        if (this.initial > interval.max) continue;

        final var start = (interval.min - this.initial) / this.rate;
        final var end = (interval.max - this.initial) / this.rate;
        final var window = Window.roundIn(start, end, Duration.SECONDS);

        if (!window.overlaps(selection)) continue;

        windows.add(window);
      }
    } else {
      for (final var interval : condition.descendingOrder()) {
        // It starts too low (and it's going lower); no intersection.
        if (this.initial < interval.min) continue;

        final var start = (interval.max - this.initial) / this.rate;
        final var end = (interval.min - this.initial) / this.rate;
        final var window = Window.roundIn(start, end, Duration.SECONDS);

        if (!window.overlaps(selection)) continue;

        windows.add(window);
      }
    }

    return windows;
  }


  @Override
  public final String toString() {
    return "λt. " + this.initial + " + t * " + this.rate;
  }

  @Override
  public final boolean equals(final Object o) {
    if (!(o instanceof RealDynamics)) return false;
    final var other = (RealDynamics) o;

    return (this.initial == other.initial) && (this.rate == other.rate);
  }
}
