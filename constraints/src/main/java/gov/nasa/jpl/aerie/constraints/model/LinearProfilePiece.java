package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Optional;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;

public final class LinearProfilePiece {
  public final Window window;
  public final double initialValue;
  public final double rate;

  public LinearProfilePiece(final Window window, final double initialValue, final double rate) {
    this.window = window;
    this.initialValue = initialValue;
    this.rate = rate;
  }

  public double finalValue() {
    return this.valueAt(this.window.end);
  }

  public double valueAt(final Duration time) {
    return this.initialValue + this.rate*(time.minus(this.window.start)).ratioOver(Duration.SECOND);
  }

  public Window changePoints() {
    return (this.rate == 0.0) ? Window.EMPTY : this.window;
  }

  private Optional<Duration> intersectionPointWith(final LinearProfilePiece other) {
    if (this.rate == other.rate) return Optional.empty();
    return Optional.of(
        this.window.start.plus(
            Duration.roundNearest(
                (other.valueAt(this.window.start) - this.initialValue) / (this.rate - other.rate)*1000000,
                Duration.MICROSECONDS)
        )
    );
  }

  public Windows windowsLessThan(final LinearProfilePiece other) {
    final var overlap = Window.intersect(this.window, other.window);
    final var intersection = this.intersectionPointWith(other);

    final Window window;
    if (intersection.isEmpty()) {
      if (this.initialValue < other.valueAt(this.window.start)) {
        window = Window.FOREVER;
      } else {
        window = Window.EMPTY;
      }
    } else {
      final var t = intersection.get();
      if (this.rate < other.rate) {
        window = Window.between(t, Exclusive, Duration.MAX_VALUE, Inclusive);
      } else {
        window = Window.between(Duration.MIN_VALUE, Inclusive, t, Exclusive);
      }
    }

    return Windows.intersection(new Windows(window), new Windows(overlap));
  }

  public Windows windowsLessThanOrEqualTo(final LinearProfilePiece other) {
    final var overlap = Window.intersect(this.window, other.window);
    final var intersection = this.intersectionPointWith(other);

    final Window window;
    if (intersection.isEmpty()) {
      if (this.initialValue <= other.valueAt(this.window.start)) {
        window = Window.FOREVER;
      } else {
        window = Window.EMPTY;
      }
    } else {
      final var t = intersection.get();
      if (this.rate < other.rate) {
        window = Window.between(t, Duration.MAX_VALUE);
      } else {
        window = Window.between(Duration.MIN_VALUE, t);
      }
    }

    return Windows.intersection(new Windows(window), new Windows(overlap));
  }

  public Windows windowsGreaterThan(final LinearProfilePiece other) {
    final var overlap = Window.intersect(this.window, other.window);
    final var intersection = this.intersectionPointWith(other);

    final Window window;
    if (intersection.isEmpty()) {
      if (this.initialValue > other.valueAt(this.window.start)) {
        window = Window.FOREVER;
      } else {
        window = Window.EMPTY;
      }
    } else {
      final var t = intersection.get();
      if (this.rate > other.rate) {
        window = Window.between(t, Exclusive, Duration.MAX_VALUE, Inclusive);
      } else {
        window = Window.between(Duration.MIN_VALUE, Inclusive, t, Exclusive);
      }
    }

    return Windows.intersection(new Windows(window), new Windows(overlap));
  }

  public Windows windowsGreaterThanOrEqualTo(final LinearProfilePiece other) {
    final var overlap = Window.intersect(this.window, other.window);
    final var intersection = this.intersectionPointWith(other);

    final Window window;
    if (intersection.isEmpty()) {
      if (this.initialValue >= other.valueAt(this.window.start)) {
        window = Window.FOREVER;
      } else {
        window = Window.EMPTY;
      }
    } else {
      final var t = intersection.get();
      if (this.rate > other.rate) {
        window = Window.between(t, Duration.MAX_VALUE);
      } else {
        window = Window.between(Duration.MIN_VALUE, t);
      }
    }

    return Windows.intersection(new Windows(window), new Windows(overlap));
  }

  public Windows windowsEqualTo(final LinearProfilePiece other) {
    final var overlap = Window.intersect(this.window, other.window);
    final var intersection = this.intersectionPointWith(other);

    final Window window;
    if (intersection.isEmpty()) {
      if (this.initialValue == other.valueAt(this.window.start)) {
        window = Window.FOREVER;
      } else {
        window = Window.EMPTY;
      }
    } else {
      final var t = intersection.get();
      window = Window.at(t);
    }

    return Windows.intersection(new Windows(window), new Windows(overlap));
  }

  public Windows windowsNotEqualTo(final LinearProfilePiece other) {
    final var overlap = Window.intersect(this.window, other.window);
    final var intersection = this.intersectionPointWith(other);

    final Windows windows;
    if (intersection.isEmpty()) {
      if (this.initialValue == other.valueAt(this.window.start)) {
        windows = new Windows();
      } else {
        windows = new Windows(Window.FOREVER);
      }
    } else {
      final var t = intersection.get();
      windows = new Windows(
          Window.between(Duration.MIN_VALUE, Inclusive, t, Exclusive),
          Window.between(t, Exclusive, Duration.MAX_VALUE, Inclusive)
      );
    }

    return Windows.intersection(windows, new Windows(overlap));
  }

  public static Windows lessThan(LinearProfilePiece left, LinearProfilePiece right) {
    return left.windowsLessThan(right);
  }

  public static Windows lessThanOrEqualTo(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.windowsLessThanOrEqualTo(right);
  }

  public static Windows greaterThan(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.windowsGreaterThan(right);
  }

  public static Windows greaterThanOrEqualTo(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.windowsGreaterThanOrEqualTo(right);
  }

  public static Windows equalTo(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.windowsEqualTo(right);
  }

  public static Windows notEqualTo(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.windowsNotEqualTo(right);
  }

  public String toString() {
    return String.format(
            "{\n" +
            "  Window: %s\n" +
            "  Initial Value: %s\n" +
            "  Rate: %s\n" +
            "}",
            this.window,
            this.initialValue,
            this.rate
    );
  }
}
