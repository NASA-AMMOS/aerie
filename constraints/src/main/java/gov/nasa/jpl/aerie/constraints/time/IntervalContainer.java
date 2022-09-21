package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity;

public interface IntervalContainer<T extends IntervalContainer<T>> {
  Spans split(final int numberOfSubIntervals, final Inclusivity internalStartInclusivity, final Inclusivity internalEndInclusivity);
}
