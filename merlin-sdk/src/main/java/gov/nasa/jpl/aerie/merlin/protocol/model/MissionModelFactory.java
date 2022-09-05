package gov.nasa.jpl.aerie.merlin.protocol.model;

import java.time.Instant;
import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeRegistrar;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;

public interface MissionModelFactory<Config, Model> {
  void buildRegistry(DirectiveTypeRegistrar<Model> registrar);

  ConfigurationType<Config> getConfigurationType();

  Model instantiate(Instant planStart, Config configuration, Initializer builder);
}
