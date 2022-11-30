package gov.nasa.jpl.aerie.constraints.time;

import java.util.Optional;
import java.util.function.Function;

/** A basic container used by {@link IntervalMap} that associates an interval with a value */
public record Segment<V>(Interval interval, V value) {
  public static <V> Segment<V> of(final Interval interval, final V value) {
    return new Segment<>(interval, value);
  }

  public <R> Segment<R> mapValue(final Function<V, R> transform) {
    return Segment.of(interval, transform.apply(value));
  }
  public Segment<V> mapInterval(final Function<Interval, Interval> transform) {
    return Segment.of(transform.apply(interval), value);
  }

  public static <V> Optional<Segment<V>> transpose(final Segment<Optional<V>> segment) {
    if (segment.value().isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(segment.mapValue(Optional::get));
    }
  }
}
