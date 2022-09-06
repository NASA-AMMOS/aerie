package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;

import java.time.Instant;
import java.util.Map;

public interface MissionModelFactory<Config, Model> {
  Map<String, ? extends DirectiveType<Model, ?, ?>> getDirectiveTypes();

  ConfigurationType<Config> getConfigurationType();

  Model instantiate(Instant planStart, Config configuration, Initializer builder);
}
