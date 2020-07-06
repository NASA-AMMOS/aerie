package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import java.util.Collections;
import java.util.List;

public final class Duration implements Comparable<Duration> {
  // Range of -2^63 to 2^63 - 1.
  private final long durationInMicroseconds;

  private Duration(final long durationInMicroseconds) {
    this.durationInMicroseconds = durationInMicroseconds;
  }

  public static final Duration EPSILON = new Duration(1);
  public static final Duration ZERO = new Duration(0);
  public static final Duration MIN_VALUE = new Duration(Long.MIN_VALUE);
  public static final Duration MAX_VALUE = new Duration(Long.MAX_VALUE);

  public static final Duration MICROSECOND = duration(1, TimeUnit.MICROSECONDS);
  public static final Duration MILLISECONDS = duration(1, TimeUnit.MILLISECONDS);
  public static final Duration SECOND = duration(1, TimeUnit.SECONDS);
  public static final Duration MINUTE = duration(1, TimeUnit.MINUTES);
  public static final Duration HOUR = duration(1, TimeUnit.HOURS);

  public static Duration of(final long quantity, final TimeUnit units) {
    switch (units) {
      case MICROSECONDS: return new Duration(quantity);
      case MILLISECONDS: return new Duration(quantity * 1000L);
      case SECONDS:      return new Duration(quantity * 1000000L);
      case MINUTES:      return new Duration(quantity * 1000000L * 60L);
      case HOURS:        return new Duration(quantity * 1000000L * 60L * 60L);
      case DAYS:         return new Duration(quantity * 1000000L * 60L * 60L * 24L);
      case WEEKS:        return new Duration(quantity * 1000000L * 60L * 60L * 24L * 7L);
      default: throw new Error("Unknown TimeUnit value: " + units);
    }
  }

  public static Duration duration(final long quantity, final TimeUnit units) {
    return of(quantity, units);
  }

  public static Duration add(final Duration left, final Duration right) throws ArithmeticException {
    return new Duration(Math.addExact(left.durationInMicroseconds, right.durationInMicroseconds));
  }

  public static Duration subtract(final Duration left, final Duration right) throws ArithmeticException {
    return new Duration(Math.subtractExact(left.durationInMicroseconds, right.durationInMicroseconds));
  }

  public static Duration multiply(final long scalar, final Duration unit) throws ArithmeticException {
    return new Duration(Math.multiplyExact(scalar, unit.durationInMicroseconds));
  }

  public static long divide(final Duration left, final Duration right) {
    return left.durationInMicroseconds / right.durationInMicroseconds;
  }

  public static Duration remainder(final Duration left, final Duration right) {
    return new Duration(left.durationInMicroseconds % right.durationInMicroseconds);
  }

  public static Duration min(final Duration x, final Duration y) {
    return Collections.min(List.of(x, y));
  }

  public static Duration max(final Duration x, final Duration y) {
    return Collections.max(List.of(x, y));
  }

  public Duration plus(final Duration other) throws ArithmeticException {
    return Duration.add(this, other);
  }

  public Duration plus(final long quantity, final TimeUnit units) throws ArithmeticException {
    return Duration.add(this, duration(quantity, units));
  }

  public Duration minus(final Duration other) throws ArithmeticException {
    return Duration.subtract(this, other);
  }

  public Duration minus(final long quantity, final TimeUnit units) throws ArithmeticException {
    return Duration.subtract(this, duration(quantity, units));
  }

  public Duration times(final long scalar) throws ArithmeticException {
    return Duration.multiply(scalar, this);
  }

  public long dividedBy(final Duration other) {
    return Duration.divide(this, other);
  }

  public long dividedBy(final long quantity, final TimeUnit units) {
    return Duration.divide(this, duration(quantity, units));
  }

  public Duration remainderOf(final Duration other) {
    return Duration.remainder(this, other);
  }

  public Duration remainderOf(final long quantity, final TimeUnit units) {
    return Duration.remainder(this, duration(quantity, units));
  }

  public boolean shorterThan(final Duration other) {
    return this.compareTo(other) < 0;
  }

  public boolean longerThan(final Duration other) {
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
    if (!(o instanceof Duration)) return false;
    final var other = (Duration)o;

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
  public int compareTo(final Duration other) {
    return Long.compare(this.durationInMicroseconds, other.durationInMicroseconds);
  }
}
