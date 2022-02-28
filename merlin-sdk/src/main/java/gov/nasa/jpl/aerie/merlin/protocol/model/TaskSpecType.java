package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;
import java.util.Map;

public interface TaskSpecType<Model, Specification, Return> {
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  Specification instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException, MissingArgumentsException;

  Map<String, SerializedValue> getArguments(Specification taskSpec);
  List<String> getValidationFailures(Specification taskSpec);

  Task<Return> createTask(Model model, Specification taskSpec);
  ValueSchema getReturnValueSchema();
  SerializedValue serializeReturnValue(Return returnValue);

  final class UnconstructableTaskSpecException extends Exception {}
}
