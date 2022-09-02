package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.InvalidArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

public interface ConfigurationType<Config> {
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  Config instantiate(Map<String, SerializedValue> arguments)
  throws InvalidArgumentsException;

  Map<String, SerializedValue> getArguments(Config configuration);
  List<String> getValidationFailures(Config configuration);

  final class UnconstructableConfigurationException extends Exception {
    public UnconstructableConfigurationException() {
      super();
    }

    public UnconstructableConfigurationException(final String message) {
      super(message);
    }
  }

  /**
   * This method must behave as though implemented as:
   * {@snippet :
   * return this.getValidationFailures(this.instantiate(arguments));
   * }
   */
  default List<String> validateArguments(final Map<String, SerializedValue> arguments)
  throws InvalidArgumentsException
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
  throws InvalidArgumentsException
  {
    return this.getArguments(this.instantiate(arguments));
  }
}
