package gov.nasa.jpl.aerie.constraints.time;

public record Segment<V>(Interval interval, V value) {
  public static <V> Segment<V> of(final Interval interval, final V value) {
    return new Segment<>(interval, value);
  }
}
