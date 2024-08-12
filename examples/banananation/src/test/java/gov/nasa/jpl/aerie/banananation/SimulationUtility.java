package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.driver.*;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class SimulationUtility {
  private static MissionModel<?> makeMissionModel(
      final MissionModelBuilder builder,
      final Instant planStart,
      final Configuration config)
  {
    final var factory = new GeneratedModelType();
    final var registry = DirectiveTypeRegistry.extract(factory);
    // TODO: [AERIE-1516] Teardown the model to release any system resources (e.g. threads).
    final var model = factory.instantiate(planStart, config, builder);
    return builder.build(model, registry);
  }

  public static <Model> SimulationDriver<Model>
  getDriver(final Duration simulationDuration)
  {
    return getDriver(simulationDuration, false);
  }

  public static <Model> SimulationDriver<Model>
  getDriver(final Duration simulationDuration, boolean runDaemons)
  {
    final var dataPath = Path.of(SimulationUtility.class.getResource("data/lorem_ipsum.txt").getPath());
    final var config = new Configuration(
        Configuration.DEFAULT_PLANT_COUNT,
        Configuration.DEFAULT_PRODUCER,
        dataPath,
        Configuration.DEFAULT_INITIAL_CONDITIONS,
        runDaemons);
    final var missionModel = makeMissionModel(new MissionModelBuilder(), Instant.EPOCH, config);

    var driver = new SimulationDriver(
        missionModel,
        simulationDuration);
    return driver;
  }

  public static SimulationResultsInterface
  simulate(final Map<ActivityDirectiveId, ActivityDirective> schedule, final Duration simulationDuration) {
    return simulate(schedule, simulationDuration, false);
  }

  public static SimulationResultsInterface
  simulate(final Map<ActivityDirectiveId, ActivityDirective> schedule, final Duration simulationDuration, boolean runDaemons) {
    final var dataPath = Path.of(SimulationUtility.class.getResource("data/lorem_ipsum.txt").getPath());
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, dataPath, Configuration.DEFAULT_INITIAL_CONDITIONS, runDaemons);
    final var startTime = Instant.now();
    final var missionModel = makeMissionModel(new MissionModelBuilder(), Instant.EPOCH, config);

    return SimulationDriver.simulate(
        missionModel,
        schedule,
        startTime,
        simulationDuration,
        startTime,
        simulationDuration,
        () -> false);
  }

  private static long _counter = 0;

  @SafeVarargs
  public static Map<ActivityDirectiveId, ActivityDirective> buildSchedule(final Pair<Duration, SerializedActivity>... activitySpecs) {
    final var schedule = new TreeMap<ActivityDirectiveId, ActivityDirective>();

    for (final var activitySpec : activitySpecs) {
      schedule.put(
          new ActivityDirectiveId(_counter++),
          new ActivityDirective(activitySpec.getLeft(), activitySpec.getRight(), null, true));
    }

    return schedule;
  }
}
