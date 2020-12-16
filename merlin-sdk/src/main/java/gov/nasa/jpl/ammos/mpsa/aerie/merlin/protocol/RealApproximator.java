package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;

public interface RealApproximator<Dynamics> {
  // TODO: Allow for finer control over the approximation, e.g. "approximate by steps".
  //   or "approximate by error tolerance".
  Iterable<DelimitedDynamics<RealDynamics>> approximate(Dynamics dynamics);
}
