package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public interface Resource<Dynamics> {
  OutputType<Dynamics> getOutputType();

  String getType();
  Dynamics getDynamics(Querier querier);
}
