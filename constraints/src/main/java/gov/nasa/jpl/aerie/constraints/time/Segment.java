package gov.nasa.jpl.aerie.constraints.time;

/** A basic container used by {@link IntervalMap} that associates an interval with a value */
public record Segment<V>(Interval interval, V value) {
  public static <V> Segment<V> of(final Interval interval, final V value) {
    return new Segment<>(interval, value);
  }
}
