package gov.nasa.jpl.aerie.merlin.protocol;

public interface RealApproximator<Dynamics> {
  // TODO: Allow for finer control over the approximation, e.g. "approximate by steps".
  //   or "approximate by error tolerance".
  Iterable<DelimitedDynamics<RealDynamics>> approximate(Dynamics dynamics);
}
