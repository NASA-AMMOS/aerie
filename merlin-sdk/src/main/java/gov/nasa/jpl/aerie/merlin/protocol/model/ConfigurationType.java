package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.UnconstructableException;

import java.util.List;
import java.util.Map;

public interface ConfigurationType<Config> {
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  Config instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableException;

  Map<String, SerializedValue> getArguments(Config configuration);
  List<String> getValidationFailures(Config configuration);
}
