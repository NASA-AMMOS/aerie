package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public interface DiscreteApproximator<Dynamics> extends Approximator<Dynamics, SerializedValue> {
  ValueSchema getSchema();
}
