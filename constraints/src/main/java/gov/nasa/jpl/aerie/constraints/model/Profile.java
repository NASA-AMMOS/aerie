package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;

public interface Profile<P extends Profile<P>> {
  Windows equalTo(P other, Interval bounds);
  Windows notEqualTo(P other, Interval bounds);
  Windows changePoints(Interval bounds);
}
