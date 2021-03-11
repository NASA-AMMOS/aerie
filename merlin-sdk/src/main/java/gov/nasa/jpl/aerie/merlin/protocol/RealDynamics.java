package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;

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


  public Optional<Duration> whenBetween(final double min, final double max, final Window scope) {
    final Window solution;
    if (this.rate == 0) {
      if (min <= this.initial && this.initial <= max) {
        solution = Window.FOREVER;
      } else {
        solution = Window.EMPTY;
      }
    } else if (this.rate > 0) {
      solution = Window.between(
          Duration.roundUpward((min - this.initial) / this.rate, Duration.SECONDS),
          Duration.roundDownward((max - this.initial) / this.rate, Duration.SECONDS));
    } else /* this.rate < 0 */ {
      solution = Window.between(
          Duration.roundUpward((max - this.initial) / this.rate, Duration.SECONDS),
          Duration.roundDownward((min - this.initial) / this.rate, Duration.SECONDS));
    }

    final var window = Window.greatestLowerBound(scope, solution);

    return (window.isEmpty())
        ? Optional.empty()
        : Optional.of(window.start);
  }

  public Optional<Duration> whenNotBetween(final double min, final double max, final Window scope) {
    if (this.rate == 0) {
      if (min <= this.initial && this.initial <= max) {
        return Optional.empty();
      } else {
        return Optional.of(scope.start);
      }
    }

    final Window beforeSolution, afterSolution;
    if (this.rate > 0) {
      beforeSolution = Window.between(
          Duration.MIN_VALUE,
          Duration.roundDownward((min - this.initial) / this.rate, Duration.SECONDS));
      afterSolution = Window.between(
          Duration.roundUpward((max - this.initial) / this.rate, Duration.SECONDS),
          Duration.MAX_VALUE);
    } else /* this.rate < 0 */ {
      beforeSolution = Window.between(
          Duration.MIN_VALUE,
          Duration.roundDownward((max - this.initial) / this.rate, Duration.SECONDS));
      afterSolution = Window.between(
          Duration.roundUpward((min - this.initial) / this.rate, Duration.SECONDS),
          Duration.MAX_VALUE);
    }

    final var beforeWindow = Window.greatestLowerBound(scope, beforeSolution);
    if (!beforeWindow.isEmpty()) return Optional.of(beforeWindow.start);

    final var afterWindow = Window.greatestLowerBound(scope, afterSolution);
    if (!afterWindow.isEmpty()) return Optional.of(afterWindow.start);

    return Optional.empty();
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
