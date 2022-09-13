package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;
import java.util.Map;

public interface InputType<T> {
  List<Parameter> getParameters();

  List<String> getRequiredParameters();

  T instantiate(Map<String, SerializedValue> arguments)
      throws InstantiationException;

  Map<String, SerializedValue> getArguments(T value);

  List<ValidationNotice> getValidationFailures(T value);

  record Parameter(String name, ValueSchema schema) {}

  record ValidationNotice(List<String> subjects, String message) { }

  /**
   * This method must behave as though implemented as:
   * {@snippet :
   * return this.getValidationFailures(this.instantiate(arguments));
   * }
   */
  default List<ValidationNotice> validateArguments(final Map<String, SerializedValue> arguments)
  throws InstantiationException
  {
    return this.getValidationFailures(this.instantiate(arguments));
  }

  /**
   * This method must behave as though implemented as:
   * {@snippet :
   * return this.getArguments(this.instantiate(arguments));
   * }
   */
  default Map<String, SerializedValue> getEffectiveArguments(final Map<String, SerializedValue> arguments)
  throws InstantiationException
  {
    return this.getArguments(this.instantiate(arguments));
  }
}
