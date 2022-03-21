package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;

import java.nio.file.Path;

public final class SimulationUtility {

  private static MissionModel<?> makeMissionModel(final MissionModelBuilder builder, final Configuration config) {
    final var factory = new gov.nasa.jpl.aerie.banananation.generated.GeneratedMissionModelFactory();
    final var model = factory.instantiate(config, builder);
    return builder.build(model, factory.getConfigurationType(), factory.getTaskSpecTypes());
  }

  public static MissionModel<?>
  getFooMissionModel() {
    final var conf = new gov.nasa.jpl.aerie.foomissionmodel.Configuration();
    final var factory = new gov.nasa.jpl.aerie.foomissionmodel.generated.GeneratedMissionModelFactory();
    final var builder = new MissionModelBuilder();
    final var model = factory.instantiate(conf, builder);
    return builder.build(model, factory.getConfigurationType(), factory.getTaskSpecTypes());
  }


  public static MissionModel<?> getBananaMissionModel(){
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, Path.of("/etc/hosts"));
    return makeMissionModel(new MissionModelBuilder(), config);
  }

  public static SchedulerModel getBananaSchedulerModel(){
    return new gov.nasa.jpl.aerie.banananation.generated.GeneratedSchedulerModel();
  }

}
