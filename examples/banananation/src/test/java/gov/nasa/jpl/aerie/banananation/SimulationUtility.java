package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.generated.GeneratedMissionModelFactory;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class SimulationUtility {
  private static MissionModel<?> makeMissionModel(final MissionModelBuilder builder, final Configuration config) {
    final var factory = new GeneratedMissionModelFactory();
    final var registry = DirectiveTypeRegistry.extract(factory);
    // TODO: [AERIE-1516] Teardown the model to release any system resources (e.g. threads).
    final var model = factory.instantiate(registry.registry(), config, builder);
    return builder.build(model, factory.getConfigurationType(), registry);
  }

  public static SimulationResults
  simulate(final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule, final Duration simulationDuration) {
    final var dataPath = Path.of(SimulationUtility.class.getResource("data/lorem_ipsum.txt").getPath());
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, dataPath);
    final var missionModel = makeMissionModel(new MissionModelBuilder(), config);
    final var startTime = Instant.now();

    return SimulationDriver.simulate(
        missionModel,
        schedule,
        startTime,
        simulationDuration);
  }

  @SafeVarargs
  public static Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> buildSchedule(final Pair<Duration, SerializedActivity>... activitySpecs) {
    final var schedule = new HashMap<ActivityInstanceId, Pair<Duration, SerializedActivity>>();
    long counter = 0;

    for (final var activitySpec : activitySpecs) {
      schedule.put(
          new ActivityInstanceId(counter++),
          activitySpec);
    }

    return schedule;
  }
}
