package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.generated.GeneratedAdaptationFactory;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static gov.nasa.jpl.aerie.time.Duration.MILLISECOND;
import static gov.nasa.jpl.aerie.time.Duration.MILLISECONDS;
import static gov.nasa.jpl.aerie.time.Duration.SECOND;
import static gov.nasa.jpl.aerie.time.Duration.SECONDS;
import static gov.nasa.jpl.aerie.time.Duration.duration;

public final class Main {
  public static void main(final String[] args) throws SimulationDriver.TaskSpecInstantiationException {
    final var adaptation = new GeneratedAdaptationFactory().instantiate();
    final var schedule = Map.of(
        UUID.randomUUID().toString(), Pair.of(
            duration(0, MILLISECONDS),
            new SerializedActivity("PeelBanana", Map.of("peelDirection", SerializedValue.of("fromStem")))),
        UUID.randomUUID().toString(), Pair.of(
            duration(300, MILLISECONDS),
            new SerializedActivity("BiteBanana", Map.of("biteSize", SerializedValue.of(1.5)))),
        UUID.randomUUID().toString(), Pair.of(
            duration(700, MILLISECONDS),
            new SerializedActivity("BiteBanana", Map.of("biteSize", SerializedValue.of(0.5)))),
        UUID.randomUUID().toString(), Pair.of(
            duration(1100, MILLISECONDS),
            new SerializedActivity("BiteBanana", Map.of("biteSize", SerializedValue.of(2.0)))),
        UUID.randomUUID().toString(), Pair.of(
            duration(1500, MILLISECONDS),
            new SerializedActivity("BiteBanana", Map.of()))
    );

    final var startTime = Instant.now();
    final var simulationDuration = duration(5, SECONDS);
    final var samplingPeriod = duration(100, MILLISECOND);

    final var simulationResults = SimulationDriver.simulate(
        adaptation,
        schedule,
        startTime,
        simulationDuration,
        samplingPeriod);

    System.out.println(simulationResults.timestamps);
    System.out.println(simulationResults.timelines);
    System.out.println(simulationResults.constraintViolations);
  }
}
