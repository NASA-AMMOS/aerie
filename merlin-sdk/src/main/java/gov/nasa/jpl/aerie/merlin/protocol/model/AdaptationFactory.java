package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

public interface AdaptationFactory<Model> {
  Map<String, TaskSpecType<Model, ?>> getTaskSpecTypes();
  List<Parameter> getParameters();
  <$Schema> Phantom<$Schema, Model> instantiate(SerializedValue configuration, Initializer<$Schema> builder);
}
