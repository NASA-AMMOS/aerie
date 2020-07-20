package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import java.util.Comparator;

/**
 * Trait describing operations on time-like data.
 */
public interface Instant<T> extends Comparator<T> {
  T origin();
  T plus(T time, Duration duration);
  T minus(T time, Duration duration);
  Duration minus(T end, T start);


  default T plus(final T time, final long quantity, final TimeUnit units) {
    return this.plus(time, Duration.of(quantity, units));
  }

  default T minus(final T time, final long quantity, final TimeUnit units) {
    return this.minus(time, Duration.of(quantity, units));
  }

  default boolean isBefore(final T left, final T right) {
    return this.compare(left, right) < 0;
  }

  default boolean isAfter(final T left, final T right) {
    return this.compare(left, right) > 0;
  }

  @SuppressWarnings("unchecked")
  default T earliestOf(final T first, final T... rest) {
    var best = first;
    for (final var next : rest) {
      best = (this.isBefore(next, best)) ? next : best;
    }
    return best;
  }

  @SuppressWarnings("unchecked")
  default T latestOf(final T first, final T... rest) {
    var best = first;
    for (final var next : rest) {
      best = (this.isAfter(next, best)) ? next : best;
    }
    return best;
  }
}
