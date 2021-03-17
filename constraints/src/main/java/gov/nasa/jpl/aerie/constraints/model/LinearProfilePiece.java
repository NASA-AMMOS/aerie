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

  public double valueAt(final Duration time) {
    return this.initialValue + this.rate*(time.minus(this.window.start)).ratioOver(Duration.SECOND);
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
