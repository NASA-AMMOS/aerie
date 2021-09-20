package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

public interface TaskSpecType<Model, Specification> {
  String getName();
  List<Parameter> getParameters();

  Specification instantiateDefault();
  Specification instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException;

  Map<String, SerializedValue> getArguments(Specification taskSpec);
  List<String> getValidationFailures(Specification taskSpec);

  <$Schema, $Timeline extends $Schema> Task<$Timeline> createTask(Phantom<$Schema, Model> model, Specification taskSpec);

  class UnconstructableTaskSpecException extends Exception {}
}
