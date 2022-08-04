package gov.nasa.jpl.aerie.constraints.time;

public interface IntervalContainer<T extends IntervalContainer<T>> {
  T split(final int numberOfSubIntervals);
}
