package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.UnconstructableException;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;
import java.util.Map;

public interface TaskSpecType<Model, Specification, Return> {
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  Specification instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableException;

  Map<String, SerializedValue> getArguments(Specification taskSpec);
  List<String> getValidationFailures(Specification taskSpec);

  Task<Return> createTask(Model model, Specification taskSpec);
  ValueSchema getReturnValueSchema();
  SerializedValue serializeReturnValue(Return returnValue);
}
