package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public interface MissionModelFactory<Model> {
  ConfigurationType<?> getConfigurationType();
  Map<String, TaskSpecType<Model, ?, ?>> getTaskSpecTypes();
  Model instantiate(SerializedValue configuration, Initializer builder) throws MissionModelInstantiationException;

  final class MissionModelInstantiationException extends RuntimeException {
    public MissionModelInstantiationException(final Throwable cause) {
      super(cause);
    }
  }
}
