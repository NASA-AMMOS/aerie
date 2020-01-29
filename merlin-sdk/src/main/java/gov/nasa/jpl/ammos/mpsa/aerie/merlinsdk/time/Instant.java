package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import java.util.Collections;
import java.util.List;

/**
 * Abstract representation of a point in time.
 *
 * The particular measure of an instant of time is specifically hidden from clients of this class.
 * Do not attempt to compare instances of different implementations of Instant -- without
 * a pair of correlated base points to use as a basis of conversion, such comparison is impossible.
 */
public interface Instant extends Comparable<Instant> {
  Instant plus(Duration2 duration);
  Instant minus(Duration2 duration);
  Duration2 durationFrom(Instant other);


  default Instant plus(final long quantity, final TimeUnit units) {
    return this.plus(Duration2.fromQuantity(quantity, units));
  }

  default Instant minus(final long quantity, final TimeUnit units) {
    return this.minus(Duration2.fromQuantity(quantity, units));
  }

  default boolean isBefore(final Instant other) {
    return this.compareTo(other) < 0;
  }

  default boolean isAfter(final Instant other) {
    return this.compareTo(other) > 0;
  }

  default Instant min(final Instant other) {
    return Collections.min(List.of(this, other));
  }

  default Instant max(final Instant other) {
    return Collections.max(List.of(this, other));
  }


  static Instant add(final Instant base, final Duration2 duration) {
    return base.plus(duration);
  }

  static Instant subtract(final Instant base, final Duration2 duration) {
    return base.minus(duration);
  }

  static Duration2 durationBetween(final Instant head, final Instant base) {
    return head.durationFrom(base);
  }

  static Instant min(final Instant x, final Instant y) {
    return x.min(y);
  }

  static Instant max(final Instant x, final Instant y) {
    return x.max(y);
  }
}
