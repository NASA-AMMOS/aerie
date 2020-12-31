package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

public interface DiscreteApproximator<Dynamics> {
  Iterable<DelimitedDynamics<SerializedValue>> approximate(Dynamics dynamics);
  ValueSchema getSchema();
}
