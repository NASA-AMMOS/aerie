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
  // Range of -2^63 to 2^63 - 1.
  private final long durationInMicroseconds;

  private Duration(final long durationInMicroseconds) {
    this.durationInMicroseconds = durationInMicroseconds;
  }

  public static final Duration EPSILON = new Duration(1);
  public static final Duration ZERO = new Duration(0);
  public static final Duration MIN_VALUE = new Duration(Long.MIN_VALUE);
  public static final Duration MAX_VALUE = new Duration(Long.MAX_VALUE);

  public static final Duration MICROSECOND = new Duration(1);
  public static final Duration MILLISECOND = MICROSECOND.times(1000);
  public static final Duration SECOND = MILLISECOND.times(1000);
  public static final Duration MINUTE = SECOND.times(60);
  public static final Duration HOUR = MINUTE.times(60);

  public static final Duration MICROSECONDS = MICROSECOND;
  public static final Duration MILLISECONDS = MILLISECOND;
  public static final Duration SECONDS = SECOND;
  public static final Duration MINUTES = MINUTE;
  public static final Duration HOURS = HOUR;

  public static Duration of(final long quantity, final Duration unit) {
    return unit.times(quantity);
  }

  public static Duration duration(final long quantity, final Duration unit) {
    return unit.times(quantity);
  }

  public static Duration negate(final Duration duration) {
    // amusingly, -MIN_VALUE = MIN_VALUE in 2's complement -- `multiplyExact` will correctly fail out in that case.
    return new Duration(Math.multiplyExact(-1, duration.durationInMicroseconds));
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

  public Duration plus(final long quantity, final Duration unit) throws ArithmeticException {
    return Duration.add(this, duration(quantity, unit));
  }

  public Duration minus(final Duration other) throws ArithmeticException {
    return Duration.subtract(this, other);
  }

  public Duration minus(final long quantity, final Duration unit) throws ArithmeticException {
    return Duration.subtract(this, duration(quantity, unit));
  }

  public Duration times(final long scalar) throws ArithmeticException {
    return Duration.multiply(scalar, this);
  }

  public long dividedBy(final Duration unit) {
    return Duration.divide(this, unit);
  }

  public long dividedBy(final long quantity, final Duration unit) {
    return Duration.divide(this, duration(quantity, unit));
  }

  public Duration remainderOf(final Duration unit) {
    return Duration.remainder(this, unit);
  }

  public Duration remainderOf(final long quantity, final Duration unit) {
    return Duration.remainder(this, duration(quantity, unit));
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
