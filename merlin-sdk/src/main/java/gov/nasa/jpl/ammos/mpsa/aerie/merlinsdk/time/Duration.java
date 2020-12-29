package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import java.util.Collections;
import java.util.List;

/**
 * A signed measure of the temporal distance between two instants.
 *
 * <p>
 * Durations are constructed by measuring a quantity of the provided units, such as {@link #SECOND} and {@link #HOUR}.
 * This can be done in multiple ways:
 * </p>
 *
 * <ul>
 * <li>
 *   Use the static factory method {@link Duration#of}, e.g. {@code Duration.of(5, SECONDS}
 *
 * <li>
 *   Use the static function {@link #duration}, e.g. {@code duration(5, SECONDS)}
 *
 * <li>
 *   Multiply an existing duration by a quantity, e.g. {@code SECOND.times(5)}
 * </ul>
 *
 * <p>
 * Note that derived units such as DAY, WEEK, MONTH, and YEAR are <em>not</em> included, because their values
 * depend on properties of the particular calendrical system in use. For example:
 * </p>
 *
 * <ul>
 * <li>
 *   The notion of "day" depends on the astronomical system against which time is measured.
 *   For example, the synodic (solar) day and the sidereal day are distinguished by which celestial body is held fixed
 *   in the sky by the passage of a day. (Synodic time fixes the body being orbited around; sidereal time
 *   fixes the far field of stars.)
 *
 * <li>
 *   The notion of "year" has precisely the same problem, with a similar synodic/sidereal distinction.
 *
 * <li>
 *   <p>
 *   The notion of "month" is worse, in that it depends on the presence of a *tertiary* body whose sygyzies with the
 *   other two bodies delimit integer quantities of the unit. (A syzygy is a collinear configuration of the bodies.)
 *   The lunar calendar (traditionally used in China) is based on a combination of lunar and solar
 *   synodic quantities. ("Month" derives from "moon".)
 *   </p>
 *
 *   <p>
 *   The month of the Gregorian calendar is approximately a lunar synodic month, except that the definition was
 *   intentionally de-regularized (including intercalary days) in deference to the Earth's solar year.
 *   (Other calendars even invoke days *outside of any month*, which Wikipedia claims are called "epagomenal days".)
 *   In retrospect, it is unsurprising that ISO 8601 ordinal dates drop the month altogether,
 *   since "month" is a (complicated) derived notion in the Gregorian calendar.
 *   </p>
 *
 * <li>
 *   The notion of "week" seemingly has no basis in the symmetries of celestial bodies, and is instead a derived unit.
 *   Unfortunately, not only is it fundamentally based on the notion of "day", different calendars assign a different
 *   number of days to the span of a week.
 * </ul>
 *
 * <p>
 * If you are working within the Gregorian calendar, the standard `java.time` package has you covered.
 * </p>
 *
 * <p>
 * If you are working with spacecraft, you may need to separate concepts such as "Earth day" and "Martian day", which
 * are synodic periods measured against the Sun but from different bodies. Worse, you likely need to convert between
 * such reference systems frequently, with a great deal of latitude in the choice of bodies being referenced.
 * The gold standard is the well-known SPICE toolkit, coupled with a good set of ephemerides and clock kernels.
 * </p>
 *
 * <p>
 * If you're just looking for a rough estimate, you can define 24-hour days and 7-day weeks and 30-day months
 * within your own domain in terms of the precise units we give here.
 * </p>
 */
public final class Duration implements Comparable<Duration> {
  // Range of (-2^63) to (2^63 - 1) microseconds.
  // This comes out to almost 600,000 years, at microsecond resolution.
  // Merlin was not designed for time scales longer than this.
  private final long durationInMicroseconds;

  private Duration(final long durationInMicroseconds) {
    this.durationInMicroseconds = durationInMicroseconds;
  }

  /**
   * The smallest observable span of time between instants.
   *
   * <p>
   * This quantity can be used to obtain the smallest representable deviation from a given instant, i.e.
   * `instant.minus(EPSILON)` and `instant.plus(EPSILON)`. The precise deviation from `instant` should not be assumed,
   * except that it is no larger than {@link Duration#MICROSECOND}.
   * </p>
   */
  public static final Duration EPSILON = new Duration(1);

  /**
   * The empty span of time.
   */
  public static final Duration ZERO = new Duration(0);

  /**
   * The largest observable negative span of time. Attempting to go "more negative" will cause an exception.
   *
   * <p>
   * The value of this quantity should not be assumed.
   * Currently, this is precisely -9,223,372,036,854,775,808 microseconds, or approximately -293,274 years.
   * </p>
   */
  public static final Duration MIN_VALUE = new Duration(Long.MIN_VALUE);

  /**
   * The largest observable positive span of time. Attempting to go "more positive" will cause an exception.
   *
   * <p>
   * The value of this quantity should not be assumed.
   * Currently, this is precisely +9,223,372,036,854,775,807 microseconds, or approximately 293,274 years.
   * </p>
   */
  public static final Duration MAX_VALUE = new Duration(Long.MAX_VALUE);

  /** One microsecond (μs). */
  public static final Duration MICROSECOND = new Duration(1);
  /** One millisecond (ms), equal to 1000μs. */
  public static final Duration MILLISECOND = MICROSECOND.times(1000);
  /** One second (s), equal to 1000ms. */
  public static final Duration SECOND = MILLISECOND.times(1000);
  /** One minute (m), equal to 60s. */
  public static final Duration MINUTE = SECOND.times(60);
  /** One hour (h), equal to 60m. */
  public static final Duration HOUR = MINUTE.times(60);

  /** The unit of measurement for microseconds. */
  public static final Duration MICROSECONDS = MICROSECOND;
  /** The unit of measurement for milliseconds. */
  public static final Duration MILLISECONDS = MILLISECOND;
  /** The unit of measurement for seconds. */
  public static final Duration SECONDS = SECOND;
  /** The unit of measurement for minutes. */
  public static final Duration MINUTES = MINUTE;
  /** The unit of measurement for hours. */
  public static final Duration HOURS = HOUR;

  /** Construct a duration in terms of a multiple of some unit. */
  public static Duration of(final long quantity, final Duration unit) {
    return unit.times(quantity);
  }

  /**
   * Construct a duration as a multiple of some unit.
   *
   * <p>
   * This factory method is intended to be imported statically and used unqualified.
   * </p>
   */
  public static Duration duration(final long quantity, final Duration unit) {
    return unit.times(quantity);
  }

  /**
   * Construct a duration in terms of a real quantity of some unit,
   * rounding to the nearest representable value above.
   */
  public static Duration roundUpward(final double quantity, final Duration unit) {
    return add(
        unit.times((long) Math.floor(quantity)),
        EPSILON.times((long) Math.ceil((quantity % 1) * unit.dividedBy(EPSILON))));
  }

  /**
   * Construct a duration in terms of a real quantity of some unit,
   * rounding to the nearest representable value below.
   */
  public static Duration roundDownward(final double quantity, final Duration unit) {
    return add(
        unit.times((long) Math.floor(quantity)),
        EPSILON.times((long) Math.floor((quantity % 1) * unit.dividedBy(EPSILON))));
  }

  /**
   * Construct a duration in terms of a real quantity of some unit,
   * rounding to the nearest representable value.
   */
  public static Duration roundNearest(final double quantity, final Duration unit) {
    return add(
        unit.times((long) Math.floor(quantity)),
        EPSILON.times((long) Math.rint((quantity % 1) * unit.dividedBy(EPSILON))));
  }

  /**
   * Flip the temporal direction of a duration. A duration into the past becomes one into the future, and vice versa.
   *
   * @param duration The duration to negate.
   * @return A new duration with its temporal direction flipped.
   * @throws ArithmeticException If the input duration is {@link #MIN_VALUE}.
   */
  public static Duration negate(final Duration duration) throws ArithmeticException {
    // amusingly, -MIN_VALUE = MIN_VALUE in 2's complement -- `multiplyExact` will correctly fail out in that case.
    return new Duration(Math.multiplyExact(-1, duration.durationInMicroseconds));
  }

  /**
   * Add two durations.
   *
   * @param left The first of the durations to sum.
   * @param right The second of the durations to sum.
   * @return The sum of the input durations.
   * @throws ArithmeticException If the result would be less than {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
   */
  public static Duration add(final Duration left, final Duration right) throws ArithmeticException {
    return new Duration(Math.addExact(left.durationInMicroseconds, right.durationInMicroseconds));
  }

  /**
   * Subtract one duration from another.
   *
   * @param left The duration to subtract from.
   * @param right The duration to subtract from the first.
   * @return The difference between the input durations.
   * @throws ArithmeticException If the result would be less than {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
   */
  public static Duration subtract(final Duration left, final Duration right) throws ArithmeticException {
    return new Duration(Math.subtractExact(left.durationInMicroseconds, right.durationInMicroseconds));
  }

  /**
   * Scale a duration by a multiplier.
   *
   * @param scalar The amount to scale the duration by.
   * @param unit The duration to be scaled.
   * @return The scaled duration.
   * @throws ArithmeticException If the result would be less than {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
   */
  public static Duration multiply(final long scalar, final Duration unit) throws ArithmeticException {
    return new Duration(Math.multiplyExact(scalar, unit.durationInMicroseconds));
  }

  /**
   * Obtain the number of integer quantities of one duration that fit completely within another duration.
   *
   * @param dividend The duration to be broken into quantities of the divisor.
   * @param divisor The duration to break the dividend into multiples of.
   * @return The integral number of times {@code divisor} goes into {@code dividend}.
   */
  public static long divide(final Duration dividend, final Duration divisor) {
    return dividend.durationInMicroseconds / divisor.durationInMicroseconds;
  }

  /**
   * Obtain the span of time remaining after dividing one duration by another.
   *
   * @param dividend The duration to be broken into quantities of the divisor.
   * @param divisor The duration to break the dividend into multiples of.
   * @return The span of time left over.
   */
  public static Duration remainder(final Duration dividend, final Duration divisor) {
    return new Duration(dividend.durationInMicroseconds % divisor.durationInMicroseconds);
  }

  /**
   * Get the ratio between two durations as a real quantity.
   *
   * @param dividend The numerator of the ratio.
   * @param divisor The denominator of the ratio.
   * @return The real-valued ratio between the given durations.
   */
  public static double ratio(final Duration dividend, final Duration divisor) {
    // Avoid casting potentially very large quantities to double before division.
    // We handle the integral part separately before casting.
    final long integralPart = dividend.durationInMicroseconds / divisor.durationInMicroseconds;
    final double fractionalPart =
        ((double) (dividend.durationInMicroseconds % divisor.durationInMicroseconds))
        / ((double) divisor.durationInMicroseconds);

    return integralPart + fractionalPart;
  }

  /** Obtain the smaller of two durations. */
  public static Duration min(final Duration x, final Duration y) {
    return Collections.min(List.of(x, y));
  }

  /** Obtain the larger of two durations. */
  public static Duration max(final Duration x, final Duration y) {
    return Collections.max(List.of(x, y));
  }

  /** Apply a duration to a {@link java.time.Instant}. */
  public static java.time.Instant addToInstant(final java.time.Instant instant, final Duration duration) {
    // Java Instants don't provide capability to add microseconds
    // Add millis and micros separately to avoid possible overflow
    return instant
        .plusMillis(duration.dividedBy(Duration.MILLISECONDS))
        .plusNanos(1000 * duration.remainderOf(Duration.MILLISECONDS).dividedBy(Duration.MICROSECONDS));
  }

  /** @see Duration#add(Duration, Duration) */
  public Duration plus(final Duration other) throws ArithmeticException {
    return Duration.add(this, other);
  }

  /** @see Duration#add(Duration, Duration) */
  public Duration plus(final long quantity, final Duration unit) throws ArithmeticException {
    return Duration.add(this, duration(quantity, unit));
  }

  /** @see Duration#subtract(Duration, Duration) */
  public Duration minus(final Duration other) throws ArithmeticException {
    return Duration.subtract(this, other);
  }

  /** @see Duration#subtract(Duration, Duration) */
  public Duration minus(final long quantity, final Duration unit) throws ArithmeticException {
    return Duration.subtract(this, duration(quantity, unit));
  }

  /** @see Duration#multiply(long, Duration) */
  public Duration times(final long scalar) throws ArithmeticException {
    return Duration.multiply(scalar, this);
  }

  /** @see Duration#divide(Duration, Duration) */
  public long dividedBy(final Duration unit) {
    return Duration.divide(this, unit);
  }

  /** @see Duration#divide(Duration, Duration) */
  public long dividedBy(final long quantity, final Duration unit) {
    return Duration.divide(this, duration(quantity, unit));
  }

  /** @see Duration#divide(Duration, Duration) */
  public long in(final Duration unit) {
    return dividedBy(unit);
  }

  /** @see Duration#remainder(Duration, Duration) */
  public Duration remainderOf(final Duration unit) {
    return Duration.remainder(this, unit);
  }

  /** @see Duration#remainder(Duration, Duration) */
  public Duration remainderOf(final long quantity, final Duration unit) {
    return Duration.remainder(this, duration(quantity, unit));
  }

  /** @see Duration#ratio(Duration, Duration) */
  public double ratioOver(final Duration unit) {
    return Duration.ratio(this, unit);
  }

  /**
   * Determine whether this duration is shorter than another.
   *
   * @see Duration#compareTo(Duration)
   */
  public boolean shorterThan(final Duration other) {
    return this.compareTo(other) < 0;
  }

  /**
   * Determine whether this duration is longer than another.
   *
   * @see Duration#compareTo(Duration)
   */
  public boolean longerThan(final Duration other) {
    return this.compareTo(other) > 0;
  }

  /**
   * Determine whether this duration points into the past. Shorthand for {@code duration.compareTo(ZERO) < 0}.
   *
   * @see Duration#compareTo(Duration)
   */
  public boolean isNegative() {
    return this.durationInMicroseconds < 0;
  }

  /**
   * Determine whether this duration points into the future. Shorthand for {@code duration.compareTo(ZERO) > 0}.
   *
   * @see Duration#compareTo(Duration)
   */
  public boolean isPositive() {
    return this.durationInMicroseconds > 0;
  }

  /**
   * Determine whether this duration points into the future. Shorthand for {@code duration.compareTo(ZERO) == 0}.
   *
   * @see Duration#compareTo(Duration)
   */

  public boolean isZero() {
    return this.durationInMicroseconds == 0;
  }

  /** Determine whether two durations are the same. */
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

  /**
   * Obtain a human-readable representation of this duration.
   *
   * <p>
   * For example, {@code duration(-5, MINUTES).plus(1, SECOND).plus(2, MICROSECONDS)} is rendered as "-00:04:58.999998".
   * </p>
   */
  @Override
  public String toString() {
    var rest = this;

    final var sign = (this.isNegative()) ? "-" : "+";

    final long hours;
    if (this.isNegative()) {
      hours = -rest.dividedBy(HOUR);
      rest = negate(rest.remainderOf(HOUR));
    } else {
      hours = rest.dividedBy(HOUR);
      rest = rest.remainderOf(HOUR);
    }

    final var minutes = rest.dividedBy(MINUTE);
    rest = rest.remainderOf(MINUTE);

    final var seconds = rest.dividedBy(SECOND);
    rest = rest.remainderOf(SECOND);

    final var microseconds = rest.dividedBy(MICROSECOND);

    return String.format("%s%02d:%02d:%02d.%06d", sign, hours, minutes, seconds, microseconds);
  }

  /** Determine whether this duration is greater than, less than, or equal to another duration. */
  @Override
  public int compareTo(final Duration other) {
    return Long.compare(this.durationInMicroseconds, other.durationInMicroseconds);
  }

  public static final class Trait implements Instant<Duration> {
    @Override
    public Duration origin() {
      return Duration.ZERO;
    }

    @Override
    public Duration plus(final Duration time, final Duration duration) {
      return Duration.add(time, duration);
    }

    @Override
    public Duration minus(final Duration time, final Duration duration) {
      return Duration.subtract(time, duration);
    }

    @Override
    public int compare(final Duration left, final Duration right) {
      return left.compareTo(right);
    }
  }
}
