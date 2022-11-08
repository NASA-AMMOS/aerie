package gov.nasa.jpl.aerie.foomissionmodel;

import gov.nasa.jpl.aerie.foomissionmodel.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.json.JsonEncoding;
import gov.nasa.jpl.aerie.merlin.framework.RootModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;

public class SimulateMapSchedule {
  public static void main(final String[] args) {
    simulateWithMapSchedule();
  }

  private static MissionModel<RootModel<Mission>>
  makeMissionModel(final MissionModelBuilder builder, final Instant planStart, final Configuration config) {
    final var factory = new GeneratedModelType();
    final var registry = DirectiveTypeRegistry.extract(factory);
    final var model = factory.instantiate(planStart, config, builder);
    return builder.build(model, registry);
  }

  private static
  void simulateWithMapSchedule() {
    final var config = new Configuration();
    final var startTime = Instant.now();
    final var simulationDuration = duration(25, SECONDS);
    final var missionModel = makeMissionModel(new MissionModelBuilder(), Instant.EPOCH, config);

    try {
      final var schedule = loadSchedule();
      final var simulationResults = SimulationDriver.simulate(
          missionModel,
          schedule,
          startTime,
          simulationDuration);

      simulationResults.realProfiles.forEach((name, samples) -> {
        System.out.println(name + ":");
        samples.getRight().forEach(point -> System.out.format("\t%s\t%s\n", point.getKey(), point.getValue()));
      });

      simulationResults.discreteProfiles.forEach((name, samples) -> {
        System.out.println(name + ":");
        samples.getRight().forEach(point -> System.out.format("\t%s\t%s\n", point.getKey(), point.getValue()));
      });

      simulationResults.simulatedActivities.forEach((name, activity) -> {
        System.out.println(name + ": " + activity.start() + " for " + activity.duration());
      });
    } finally {
      missionModel.getModel().close();
    }
  }

  private static Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> loadSchedule() {
    final var schedule = new HashMap<ActivityInstanceId, Pair<Duration, SerializedActivity>>();
    long counter = 0;

    final var planJson = Json.createReader(SimulateMapSchedule.class.getResourceAsStream("plan.json")).readValue();
    for (final var scheduledActivity : planJson.asJsonArray()) {
      final var deferInMicroseconds = scheduledActivity.asJsonObject().getJsonNumber("defer").longValueExact();
      final var activityType = scheduledActivity.asJsonObject().getString("type");

      final var arguments = new HashMap<String, SerializedValue>();
      for (final var field : scheduledActivity.asJsonObject().getJsonObject("arguments").entrySet()) {
        arguments.put(field.getKey(), JsonEncoding.decode(field.getValue()));
      }

      schedule.put(
          new ActivityInstanceId(counter++),
          Pair.of(
              duration(deferInMicroseconds, MICROSECONDS),
              new SerializedActivity(activityType, arguments)));
    }

    return schedule;
  }
}
