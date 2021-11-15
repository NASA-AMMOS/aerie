package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.banananation.ConfigurationValueMapper;
import gov.nasa.jpl.aerie.banananation.generated.GeneratedMissionModelFactory;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.nio.file.Path;

public final class SimulationUtility {

  private static MissionModel<?> makeMissionModel(final MissionModelBuilder builder, final SerializedValue config) {
    final var factory = new GeneratedMissionModelFactory();
    final var model = factory.instantiate(config, builder);
    return builder.build(model, factory.getTaskSpecTypes());
  }


  public static MissionModel<?> getMissionModel(){
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, Path.of("/etc/hosts"));
    final var serializedConfig = new ConfigurationValueMapper().serializeValue(config);
    return makeMissionModel(new MissionModelBuilder(), serializedConfig);
  }

}
