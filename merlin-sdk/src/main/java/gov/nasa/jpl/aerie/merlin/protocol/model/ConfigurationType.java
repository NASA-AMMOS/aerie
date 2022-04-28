package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

public interface ConfigurationType<Config> {
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  Config instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableConfigurationException, MissingArgumentsException;

  Map<String, SerializedValue> getArguments(Config configuration);
  List<String> getValidationFailures(Config configuration);

  final class UnconstructableConfigurationException extends Exception {
    public UnconstructableConfigurationException() {
      super();
    }

    public UnconstructableConfigurationException(final String message) {
      super(message);
    }

    public static UnconstructableConfigurationException unconstructableArgument(final String argumentName, final String failure) {
      return new UnconstructableConfigurationException("Unconstructable argument \"%s\": %s".formatted(argumentName, failure));
    }

    public static UnconstructableConfigurationException nonexistentArgument(final String argumentName) {
      return new UnconstructableConfigurationException("Nonexistent argument \"%s\"".formatted(argumentName));
    }
  }
}
