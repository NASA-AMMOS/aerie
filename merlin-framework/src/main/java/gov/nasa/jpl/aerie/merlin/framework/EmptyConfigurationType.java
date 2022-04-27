package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.UnconstructableException;

import java.util.List;
import java.util.Map;

public final class EmptyConfigurationType implements ConfigurationType<VoidEnum> {
  @Override
  public List<Parameter> getParameters() {
    return List.of();
  }

  @Override
  public List<String> getRequiredParameters() {
    return List.of();
  }

  @Override
  public VoidEnum instantiate(final Map<String, SerializedValue> arguments)
  throws UnconstructableException
  {
    if (!arguments.isEmpty()) {
      // TODO: once `NonexistentArgument` is changed to `NonexistentArguments` this `stream().findAny()` logic won't be needed
      throw new UnconstructableException(new UnconstructableException.Reason.NonexistentArgument(arguments.keySet().stream().findAny().get()));
    }

    return VoidEnum.VOID;
  }

  @Override
  public Map<String, SerializedValue> getArguments(final VoidEnum configuration) {
    return Map.of();
  }

  @Override
  public List<String> getValidationFailures(final VoidEnum configuration) {
    return List.of();
  }
}
