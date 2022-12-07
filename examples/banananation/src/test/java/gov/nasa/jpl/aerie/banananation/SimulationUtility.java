package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.driver.ActionTree;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class SimulationUtility {
  private static MissionModel<?> makeMissionModel(final MissionModelBuilder builder, final Instant planStart, final Configuration config) {
    final var factory = new GeneratedModelType();
    final var registry = DirectiveTypeRegistry.extract(factory);
    // TODO: [AERIE-1516] Teardown the model to release any system resources (e.g. threads).
    final var model = factory.instantiate(planStart, config, builder);
    return builder.build(model, registry);
  }

  public static SimulationResults
  simulate(final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule, final Duration simulationDuration) {
    final var dataPath = Path.of(SimulationUtility.class.getResource("data/lorem_ipsum.txt").getPath());
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, dataPath);
    final var startTime = Instant.now();
    final var missionModel = makeMissionModel(new MissionModelBuilder(), Instant.EPOCH, config);

    final ActionTree plan;
    try {
      plan = ActionTree.from(simulationDuration, missionModel, schedule);
    } catch (final InstantiationException ex) {
      throw new RuntimeException(ex);
    }

    return SimulationDriver.simulate(missionModel, plan, startTime, simulationDuration);
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
