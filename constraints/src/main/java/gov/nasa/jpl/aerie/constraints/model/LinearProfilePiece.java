package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;
import java.util.Optional;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;

public final class LinearProfilePiece {
  public final Interval interval;
  public final double initialValue;
  public final double rate;

  public LinearProfilePiece(final Interval interval, final double initialValue, final double rate) {
    this.interval = interval;
    this.initialValue = initialValue;
    this.rate = rate;
  }

  public double finalValue() {
    return this.valueAt(this.interval.end);
  }

  public double valueAt(final Duration time) {
    return this.initialValue + this.rate*(time.minus(this.interval.start)).ratioOver(Duration.SECOND);
  }

  public Interval changePoints() {
    return (this.rate == 0.0) ? Interval.EMPTY : this.interval;
  }

  private Optional<Duration> intersectionPointWith(final LinearProfilePiece other) {
    if (this.rate == other.rate) return Optional.empty();
    return Optional.of(
        this.interval.start.plus(
            Duration.roundNearest(
                (other.valueAt(this.interval.start) - this.initialValue) / (this.rate - other.rate)*1000000,
                Duration.MICROSECONDS)
        )
    );
  }

  public Windows windowsLessThan(final LinearProfilePiece other) {
    final var overlap = Interval.intersect(this.interval, other.interval);
    final var intersection = this.intersectionPointWith(other);

    final Interval interval;
    if (intersection.isEmpty()) {
      if (this.initialValue < other.valueAt(this.interval.start)) {
        interval = Interval.FOREVER;
      } else {
        interval = Interval.EMPTY;
      }
    } else {
      final var t = intersection.get();
      if (this.rate < other.rate) {
        interval = Interval.between(t, Exclusive, Duration.MAX_VALUE, Inclusive);
      } else {
        interval = Interval.between(Duration.MIN_VALUE, Inclusive, t, Exclusive);
      }
    }

    return Windows.intersection(new Windows(interval), new Windows(overlap));
  }

  public Windows windowsLessThanOrEqualTo(final LinearProfilePiece other) {
    final var overlap = Interval.intersect(this.interval, other.interval);
    final var intersection = this.intersectionPointWith(other);

    final Interval interval;
    if (intersection.isEmpty()) {
      if (this.initialValue <= other.valueAt(this.interval.start)) {
        interval = Interval.FOREVER;
      } else {
        interval = Interval.EMPTY;
      }
    } else {
      final var t = intersection.get();
      if (this.rate < other.rate) {
        interval = Interval.between(t, Duration.MAX_VALUE);
      } else {
        interval = Interval.between(Duration.MIN_VALUE, t);
      }
    }

    return Windows.intersection(new Windows(interval), new Windows(overlap));
  }

  public Windows windowsGreaterThan(final LinearProfilePiece other) {
    final var overlap = Interval.intersect(this.interval, other.interval);
    final var intersection = this.intersectionPointWith(other);

    final Interval interval;
    if (intersection.isEmpty()) {
      if (this.initialValue > other.valueAt(this.interval.start)) {
        interval = Interval.FOREVER;
      } else {
        interval = Interval.EMPTY;
      }
    } else {
      final var t = intersection.get();
      if (this.rate > other.rate) {
        interval = Interval.between(t, Exclusive, Duration.MAX_VALUE, Inclusive);
      } else {
        interval = Interval.between(Duration.MIN_VALUE, Inclusive, t, Exclusive);
      }
    }

    return Windows.intersection(new Windows(interval), new Windows(overlap));
  }

  public Windows windowsGreaterThanOrEqualTo(final LinearProfilePiece other) {
    final var overlap = Interval.intersect(this.interval, other.interval);
    final var intersection = this.intersectionPointWith(other);

    final Interval interval;
    if (intersection.isEmpty()) {
      if (this.initialValue >= other.valueAt(this.interval.start)) {
        interval = Interval.FOREVER;
      } else {
        interval = Interval.EMPTY;
      }
    } else {
      final var t = intersection.get();
      if (this.rate > other.rate) {
        interval = Interval.between(t, Duration.MAX_VALUE);
      } else {
        interval = Interval.between(Duration.MIN_VALUE, t);
      }
    }

    return Windows.intersection(new Windows(interval), new Windows(overlap));
  }

  public Windows windowsEqualTo(final LinearProfilePiece other) {
    final var overlap = Interval.intersect(this.interval, other.interval);
    final var intersection = this.intersectionPointWith(other);

    final Interval interval;
    if (intersection.isEmpty()) {
      if (this.initialValue == other.valueAt(this.interval.start)) {
        interval = Interval.FOREVER;
      } else {
        interval = Interval.EMPTY;
      }
    } else {
      final var t = intersection.get();
      interval = Interval.at(t);
    }

    return Windows.intersection(new Windows(interval), new Windows(overlap));
  }

  public Windows windowsNotEqualTo(final LinearProfilePiece other) {
    final var overlap = Interval.intersect(this.interval, other.interval);
    final var intersection = this.intersectionPointWith(other);

    final Windows windows;
    if (intersection.isEmpty()) {
      if (this.initialValue == other.valueAt(this.interval.start)) {
        windows = new Windows();
      } else {
        windows = new Windows(Interval.FOREVER);
      }
    } else {
      final var t = intersection.get();
      windows = new Windows(
          Interval.between(Duration.MIN_VALUE, Inclusive, t, Exclusive),
          Interval.between(t, Exclusive, Duration.MAX_VALUE, Inclusive)
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
            "  Interval: %s\n" +
            "  Initial Value: %s\n" +
            "  Rate: %s\n" +
            "}",
            this.interval,
            this.initialValue,
            this.rate
    );
  }

  @Override
  public boolean equals(final Object obj) {
    if(!(obj instanceof LinearProfilePiece)) return false;
    final var other = (LinearProfilePiece)obj;

    return this.interval.equals(other.interval) &&
           this.initialValue == other.initialValue &&
           this.rate == other.rate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.initialValue, this.interval, this.rate);
  }
}
