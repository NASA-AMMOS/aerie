package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimpleSimulator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.MILLISECONDS;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.duration;

public final class Main {
  public static void main(final String[] args) {
    final var schedule = List.of(
        Pair.of(duration(0, MILLISECONDS), new SerializedActivity("PeelBanana", Map.of("peelDirection", SerializedParameter.of("fromStem")))),
        Pair.of(duration(500, MILLISECONDS), new SerializedActivity("BiteBanana", Map.of("biteSize", SerializedParameter.of(0.5)))),
        Pair.of(duration(1500, MILLISECONDS), new SerializedActivity("BiteBanana", Map.of())));

    final var adaptation = new Banananation();
    final var results = SimpleSimulator.simulateToCompletion(adaptation, schedule, duration(100, MILLISECONDS));

    System.out.println(results.timestamps);
    System.out.println(results.timelines);
    System.out.println(results.constraintViolations);
  }
}
