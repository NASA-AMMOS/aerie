package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.List;
import java.util.Map;

public final class EmptyConfigurationType implements ConfigurationType<Unit> {
  @Override
  public List<Parameter> getParameters() {
    return List.of();
  }

  @Override
  public List<String> getRequiredParameters() {
    return List.of();
  }

  @Override
  public Unit instantiate(final Map<String, SerializedValue> arguments)
  throws UnconstructableConfigurationException, MissingArgumentsException
  {
    if (!arguments.isEmpty()) throw new UnconstructableConfigurationException();

    return Unit.UNIT;
  }

  @Override
  public Map<String, SerializedValue> getArguments(final Unit configuration) {
    return Map.of();
  }

  @Override
  public List<String> getValidationFailures(final Unit configuration) {
    return List.of();
  }
}
