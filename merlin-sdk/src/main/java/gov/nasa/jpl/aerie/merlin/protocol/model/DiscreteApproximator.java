package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public interface DiscreteApproximator<Dynamics> {
  Iterable<DelimitedDynamics<SerializedValue>> approximate(Dynamics dynamics);
  ValueSchema getSchema();
}
