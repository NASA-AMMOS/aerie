package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;

public interface AdaptationFactory {
  List<Parameter> getParameters();

  <$Schema> void instantiate(SerializedValue configuration, Initializer<$Schema> builder);

  record Parameter(String name, ValueSchema schema) {}
}
