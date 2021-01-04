package gov.nasa.jpl.aerie.fooadaptation;

import gov.nasa.jpl.aerie.fooadaptation.generated.GeneratedAdaptationFactory;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.json.JsonEncoding;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static gov.nasa.jpl.aerie.time.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.time.Duration.SECOND;
import static gov.nasa.jpl.aerie.time.Duration.SECONDS;
import static gov.nasa.jpl.aerie.time.Duration.duration;

public class SimulateMapSchedule {
  public static void main(final String[] args) {
    try {
      simulateWithMapSchedule();
    } catch (final SimulationDriver.TaskSpecInstantiationException ex) {
      ex.printStackTrace();
    }
  }

  private static
  void simulateWithMapSchedule()
  throws SimulationDriver.TaskSpecInstantiationException
  {
    final var schedule = loadSchedule();
    final var startTime = Instant.now();
    final var simulationDuration = duration(5, SECONDS);
    final var samplingPeriod = duration(1, SECOND);

    final var simulationResults = SimulationDriver.simulate(
        new GeneratedAdaptationFactory().instantiate(),
        schedule,
        startTime,
        simulationDuration,
        samplingPeriod);

    simulationResults.timelines.forEach((name, samples) -> System.out.format("%s: %s\n", name, samples));
  }

  private static Map<String, Pair<Duration, SerializedActivity>> loadSchedule() {
    final var schedule = new HashMap<String, Pair<Duration, SerializedActivity>>();

    final var planJson = Json.createReader(SimulateMapSchedule.class.getResourceAsStream("plan.json")).readValue();
    for (final var scheduledActivity : planJson.asJsonArray()) {
      final var deferInMicroseconds = scheduledActivity.asJsonObject().getJsonNumber("defer").longValueExact();
      final var activityType = scheduledActivity.asJsonObject().getString("type");

      final var arguments = new HashMap<String, SerializedValue>();
      for (final var field : scheduledActivity.asJsonObject().getJsonObject("arguments").entrySet()) {
        arguments.put(field.getKey(), JsonEncoding.decode(field.getValue()));
      }

      schedule.put(
          UUID.randomUUID().toString(),
          Pair.of(
              duration(deferInMicroseconds, MICROSECONDS),
              new SerializedActivity(activityType, arguments)));
    }

    return schedule;
  }
}
