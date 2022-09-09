package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;

import java.util.List;
import java.util.Map;

public interface InputType<T> {
  List<Parameter> getParameters();

  List<String> getRequiredParameters();

  T instantiate(Map<String, SerializedValue> arguments)
      throws InstantiationException;

  Map<String, SerializedValue> getArguments(T value);

  List<ValidationNotice> getValidationFailures(T value);

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
