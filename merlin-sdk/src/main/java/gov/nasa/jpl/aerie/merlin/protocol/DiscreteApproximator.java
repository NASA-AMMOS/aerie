package gov.nasa.jpl.aerie.merlin.protocol;

public interface DiscreteApproximator<Dynamics> {
  Iterable<DelimitedDynamics<SerializedValue>> approximate(Dynamics dynamics);
  ValueSchema getSchema();
}
