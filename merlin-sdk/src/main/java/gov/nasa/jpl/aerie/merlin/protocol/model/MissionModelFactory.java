package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;

import java.util.Map;

public interface MissionModelFactory<Config, Model> {
  ConfigurationType<Config> getConfigurationType();
  Map<String, TaskSpecType<Model, ?, ?>> getTaskSpecTypes();
  Model instantiate(Config configuration, Initializer builder);
}
