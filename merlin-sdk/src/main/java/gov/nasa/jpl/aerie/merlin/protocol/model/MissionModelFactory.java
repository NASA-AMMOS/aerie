package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.Optional;

public interface MissionModelFactory<Model> {
  Optional<ConfigurationType<?>> getConfigurationType();
  Map<String, TaskSpecType<Model, ?, ?>> getTaskSpecTypes();
  Model instantiate(SerializedValue configuration, Initializer builder) throws MissionModelInstantiationException;

  final class MissionModelInstantiationException extends RuntimeException {
    public MissionModelInstantiationException(final Throwable cause) {
      super(cause);
    }
  }
}
