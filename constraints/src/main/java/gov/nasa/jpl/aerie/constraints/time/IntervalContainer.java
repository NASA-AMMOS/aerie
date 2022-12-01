package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity;

public interface IntervalContainer<T extends IntervalContainer<T>> {
  Spans split(final Interval bounds, final int numberOfSubIntervals, final Inclusivity internalStartInclusivity, final Inclusivity internalEndInclusivity);
  T starts();
  T ends();
}
