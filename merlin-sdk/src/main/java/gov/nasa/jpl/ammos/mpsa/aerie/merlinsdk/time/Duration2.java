package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import java.util.Collections;
import java.util.List;

public final class Duration2 implements Comparable<Duration2> {
  // Range of -2^63 to 2^63 - 1.
  public final long durationInMicroseconds;

  private Duration2(final long durationInMicroseconds) {
    this.durationInMicroseconds = durationInMicroseconds;
  }

  public static Duration2 fromQuantity(final long quantity, final TimeUnit units) {
    switch (units) {
      case MICROSECONDS: return new Duration2(quantity);
      case MILLISECONDS: return new Duration2(quantity * 1000L);
      case SECONDS:      return new Duration2(quantity * 1000000L);
      case MINUTES:      return new Duration2(quantity * 1000000L * 60L);
      case HOURS:        return new Duration2(quantity * 1000000L * 60L * 60L);
      case DAYS:         return new Duration2(quantity * 1000000L * 60L * 60L * 24L);
      case WEEKS:        return new Duration2(quantity * 1000000L * 60L * 60L * 24L * 7L);
      default: throw new Error("Unknown TimeUnit value: " + units);
    }
  }

  public static Duration2 add(final Duration2 left, final Duration2 right) throws ArithmeticException {
    return new Duration2(Math.addExact(left.durationInMicroseconds, right.durationInMicroseconds));
  }

  public static Duration2 subtract(final Duration2 left, final Duration2 right) throws ArithmeticException {
    return new Duration2(Math.subtractExact(left.durationInMicroseconds, right.durationInMicroseconds));
  }

  public static Duration2 min(final Duration2 x, final Duration2 y) {
    return Collections.min(List.of(x, y));
  }

  public static Duration2 max(final Duration2 x, final Duration2 y) {
    return Collections.max(List.of(x, y));
  }

  public boolean shorterThan(final Duration2 other) {
    return this.compareTo(other) < 0;
  }

  public boolean longerThan(final Duration2 other) {
    return this.compareTo(other) > 0;
  }

  public boolean isNegative() {
    return this.durationInMicroseconds < 0;
  }

  public boolean isPositive() {
    return this.durationInMicroseconds > 0;
  }

  public boolean isZero() {
    return this.durationInMicroseconds == 0;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Duration2)) return false;
    final var other = (Duration2)o;

    return (this.durationInMicroseconds == other.durationInMicroseconds);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.durationInMicroseconds);
  }

  @Override
  public String toString() {
    return "" + this.durationInMicroseconds + "µs";
  }

  @Override
  public int compareTo(final Duration2 other) {
    return Long.compareUnsigned(this.durationInMicroseconds, other.durationInMicroseconds);
  }
}
