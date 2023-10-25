package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.foomissionmodel.Mission;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;

import java.nio.file.Path;
import java.time.Instant;

public final class SimulationUtility {

  private static MissionModel<?> makeMissionModel(final MissionModelBuilder builder, final Configuration config) {
    final var factory = new gov.nasa.jpl.aerie.banananation.generated.GeneratedModelType();
    final var registry = DirectiveTypeRegistry.extract(factory);
    final var model = factory.instantiate(Instant.EPOCH, config, builder);
    return builder.build(model, registry);
  }

  public static MissionModel<Mission>
  getFooMissionModel() {
    final var config = new gov.nasa.jpl.aerie.foomissionmodel.Configuration();
    final var factory = new gov.nasa.jpl.aerie.foomissionmodel.generated.GeneratedModelType();
    final var registry = DirectiveTypeRegistry.extract(factory);
    final var builder = new MissionModelBuilder();
    final var model = factory.instantiate(Instant.EPOCH, config, builder);
    return builder.build(model, registry);
  }

  public static SchedulerModel getFooSchedulerModel(){
    return new gov.nasa.jpl.aerie.foomissionmodel.generated.GeneratedSchedulerModel();
  }

  public static MissionModel<?> getBananaMissionModel(){
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, Path.of("/etc/hosts"), Configuration.DEFAULT_INITIAL_CONDITIONS, false);
    return makeMissionModel(new MissionModelBuilder(), config);
  }

  public static SchedulerModel
  getBananaSchedulerModel(){
    return new gov.nasa.jpl.aerie.banananation.generated.GeneratedSchedulerModel();
  }
}
