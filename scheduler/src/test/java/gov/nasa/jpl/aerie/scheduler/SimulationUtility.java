package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.banananation.ConfigurationValueMapper;
import gov.nasa.jpl.aerie.banananation.generated.GeneratedAdaptationFactory;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.nio.file.Path;

public final class SimulationUtility {

  private static <$Schema> MissionModel<$Schema, ?> makeAdaptation(final MissionModelBuilder<$Schema> builder, final SerializedValue config) {
    final var factory = new GeneratedAdaptationFactory();
    final var model = factory.instantiate(config, builder);
    return builder.build(model, factory.getTaskSpecTypes());
  }


  public static MissionModel<?, ?> getAdaptation(){
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, Path.of("/etc/hosts"));
    final var serializedConfig = new ConfigurationValueMapper().serializeValue(config);
    MissionModel adaptation = makeAdaptation(new MissionModelBuilder<>(), serializedConfig);
    return adaptation;
  }

}
