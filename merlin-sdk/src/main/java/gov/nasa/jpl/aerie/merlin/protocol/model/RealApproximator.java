package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;

public interface RealApproximator<Dynamics> extends Approximator<Dynamics, RealDynamics> {
  // TODO: Allow for finer control over the approximation, e.g. "approximate by steps".
  //   or "approximate by error tolerance".
}
