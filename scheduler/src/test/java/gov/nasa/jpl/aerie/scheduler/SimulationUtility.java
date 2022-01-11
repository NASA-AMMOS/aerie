package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.banananation.ConfigurationValueMapper;
import gov.nasa.jpl.aerie.foomissionmodel.mappers.FooValueMappers;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.nio.file.Path;

public final class SimulationUtility {

  private static MissionModel<?> makeMissionModel(final MissionModelBuilder builder, final SerializedValue config) {
    final var factory = new gov.nasa.jpl.aerie.banananation.generated.GeneratedMissionModelFactory();
    final var model = factory.instantiate(config, builder);
    return builder.build(model, factory.getTaskSpecTypes());
  }

  public static MissionModel<?>
  getFooMissionModel() {
    var conf = new gov.nasa.jpl.aerie.foomissionmodel.Configuration();
    final var serializedConfig = FooValueMappers.configuration().serializeValue(conf);
    final var factory = new gov.nasa.jpl.aerie.foomissionmodel.generated.GeneratedMissionModelFactory();
    final var builder = new MissionModelBuilder();
    final var model = factory.instantiate(serializedConfig, builder);
    return builder.build(model, factory.getTaskSpecTypes());
  }


  public static MissionModel<?> getBananaMissionModel(){
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, Path.of("/etc/hosts"));
    final var serializedConfig = new ConfigurationValueMapper().serializeValue(config);
    return makeMissionModel(new MissionModelBuilder(), serializedConfig);
  }

}
