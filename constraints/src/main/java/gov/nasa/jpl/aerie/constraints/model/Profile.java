package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Windows;

public interface Profile<P extends Profile<P>> {
  Windows equalTo(P other);
  Windows notEqualTo(P other);
  Windows changePoints();

  P assignGaps(P def);
}
