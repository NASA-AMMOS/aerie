package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimpleSimulator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.MILLISECONDS;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.duration;

public final class Main {
  public static void main(final String[] args) throws SimpleSimulator.InvalidSerializedActivityException {
    final var schedule = List.of(
        Pair.of(duration(0, MILLISECONDS), new SerializedActivity("PeelBanana", Map.of("peelDirection", SerializedValue.of("fromStem")))),
        Pair.of(duration(500, MILLISECONDS), new SerializedActivity("BiteBanana", Map.of("biteSize", SerializedValue.of(0.5)))),
        Pair.of(duration(1500, MILLISECONDS), new SerializedActivity("BiteBanana", Map.of())));

    final var adaptation = new Banananation();
    final var results = SimpleSimulator.simulateToCompletion(adaptation, schedule, Instant.MIN, duration(100, MILLISECONDS));

    System.out.println(results.timestamps);
    System.out.println(results.timelines);
    System.out.println(results.constraintViolations);
  }
}
