package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeRegistrar;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;

import java.util.Map;

public interface MissionModelFactory<Registry, Config, Model> {
  // It is intended that `buildRegistry` replace `getTaskSpecTypes`.
  Registry buildRegistry(DirectiveTypeRegistrar<Model> registrar);

  ConfigurationType<Config> getConfigurationType();
  Map<String, TaskSpecType<Model, ?, ?>> getTaskSpecTypes();
  Model instantiate(Config configuration, Initializer builder);
}
