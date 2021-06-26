package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.ParameterSchema;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

public interface TaskSpecType<$Schema, Specification> {
  String getName();
  List<ParameterSchema> getParameters();

  Specification instantiateDefault();
  Specification instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException;

  Map<String, SerializedValue> getArguments(Specification taskSpec);
  List<String> getValidationFailures(Specification taskSpec);

  <$Timeline extends $Schema> Task<$Timeline> createTask(Specification taskSpec);

  class UnconstructableTaskSpecException extends Exception {}
}
