package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.DelimitedDynamics;

public interface Approximator<Original, Derived> {
  Iterable<DelimitedDynamics<Derived>> approximate(Original dynamics);
}
