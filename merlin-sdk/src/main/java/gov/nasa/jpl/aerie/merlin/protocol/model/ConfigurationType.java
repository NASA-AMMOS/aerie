package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

public interface ConfigurationType<Config> {
  String getName();
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  Config instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableConfigurationException, MissingArgumentsException;

  Map<String, SerializedValue> getArguments(Config configuration);
  List<String> getValidationFailures(Config configuration);

  final class UnconstructableConfigurationException extends Exception {}
}
