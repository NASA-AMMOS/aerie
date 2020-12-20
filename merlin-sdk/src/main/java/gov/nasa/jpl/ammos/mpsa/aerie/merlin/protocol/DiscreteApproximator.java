package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

public interface DiscreteApproximator<Dynamics> {
  Iterable<DelimitedDynamics<SerializedValue>> approximate(Dynamics dynamics);
  ValueSchema getSchema();
}
