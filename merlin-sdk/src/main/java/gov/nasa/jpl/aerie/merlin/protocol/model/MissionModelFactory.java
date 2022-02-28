package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeRegistrar;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;

public interface MissionModelFactory<Registry, Config, Model> {
  Registry buildRegistry(DirectiveTypeRegistrar<Model> registrar);

  ConfigurationType<Config> getConfigurationType();

  Model instantiate(Config configuration, Initializer builder);
}
