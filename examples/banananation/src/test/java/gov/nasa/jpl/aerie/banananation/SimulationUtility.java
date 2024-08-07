package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.driver.*;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class SimulationUtility {
  public static SimulationResults
  simulate(final Map<ActivityDirectiveId, ActivityDirective> schedule, final Duration simulationDuration) {
    final var dataPath = Path.of(SimulationUtility.class.getResource("data/lorem_ipsum.txt").getPath());
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, dataPath, Configuration.DEFAULT_INITIAL_CONDITIONS);
    final var startTime = Instant.now();
    final var missionModel = gov.nasa.jpl.aerie.stateless.simulation.SimulationUtility.instantiateMissionModel(
        new GeneratedModelType(),
        Instant.EPOCH,
        config);

    return SimulationDriver.simulate(
        missionModel,
        schedule,
        startTime,
        simulationDuration,
        startTime,
        simulationDuration,
        () -> false);
  }

  @SafeVarargs
  public static Map<ActivityDirectiveId, ActivityDirective> buildSchedule(final Pair<Duration, SerializedActivity>... activitySpecs) {
    final var schedule = new HashMap<ActivityDirectiveId, ActivityDirective>();
    long counter = 0;

    for (final var activitySpec : activitySpecs) {
      schedule.put(
          new ActivityDirectiveId(counter++),
          new ActivityDirective(activitySpec.getLeft(), activitySpec.getRight(), null, true));
    }

    return schedule;
  }
}
