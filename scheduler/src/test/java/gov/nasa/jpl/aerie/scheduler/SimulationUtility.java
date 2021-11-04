package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.banananation.ConfigurationValueMapper;
import gov.nasa.jpl.aerie.banananation.generated.GeneratedAdaptationFactory;
import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import gov.nasa.jpl.aerie.merlin.driver.AdaptationBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.nio.file.Path;

public final class SimulationUtility {

  private static <$Schema> Adaptation<$Schema, ?> makeAdaptation(final AdaptationBuilder<$Schema> builder, final SerializedValue config) {
    final var factory = new GeneratedAdaptationFactory();
    final var model = factory.instantiate(config, builder);
    return builder.build(model, factory.getTaskSpecTypes());
  }


  public static Adaptation<?, ?> getAdaptation(){
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, Path.of("/etc/hosts"));
    final var serializedConfig = new ConfigurationValueMapper().serializeValue(config);
    Adaptation adaptation = makeAdaptation(new AdaptationBuilder<>(), serializedConfig);
    return adaptation;
  }

}
