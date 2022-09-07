package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.InvalidArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;
import java.util.Map;

public interface TaskSpecType<Model, Specification, Return> {
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  Specification instantiate(Map<String, SerializedValue> arguments)
  throws InvalidArgumentsException;

  Map<String, SerializedValue> getArguments(Specification taskSpec);
  List<ValidationNotice> getValidationFailures(Specification taskSpec);

  Task<Return> createTask(Model model, Specification taskSpec);
  ValueSchema getReturnValueSchema();
  SerializedValue serializeReturnValue(Return returnValue);

  final class UnconstructableTaskSpecException extends Exception {
    public UnconstructableTaskSpecException(final String message) {
      super(message);
    }
  }

  /**
   * This method must behave as though implemented as:
   * {@snippet :
   * return this.getValidationFailures(this.instantiate(activity));
   * }
   */
  default List<ValidationNotice> validateArguments(final Map<String, SerializedValue> arguments)
  throws InvalidArgumentsException {
    return this.getValidationFailures(this.instantiate(arguments));
  }

  /**
   * This method must behave as though implemented as:
   * {@snippet :
   * return this.getArguments(this.instantiate(arguments));
   * }
   */
  default Map<String, SerializedValue> getEffectiveArguments(final Map<String, SerializedValue> arguments)
  throws InvalidArgumentsException
  {
    return this.getArguments(this.instantiate(arguments));
  }
}
