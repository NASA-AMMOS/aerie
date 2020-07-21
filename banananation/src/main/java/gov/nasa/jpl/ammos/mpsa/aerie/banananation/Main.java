package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimpleSimulator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public final class Main {
  public static void main(final String[] args) {
    final var schedule = List.of(
        Pair.of(Duration.of(0, TimeUnit.MILLISECONDS), new SerializedActivity("PeelBanana", Map.of("peelDirection", SerializedParameter.of("fromStem")))),
        Pair.of(Duration.of(500, TimeUnit.MILLISECONDS), new SerializedActivity("BiteBanana", Map.of("biteSize", SerializedParameter.of(0.5)))),
        Pair.of(Duration.of(1500, TimeUnit.MILLISECONDS), new SerializedActivity("BiteBanana", Map.of())));

    final var adaptation = new Banananation();
    final var results = SimpleSimulator.simulateToCompletion(adaptation, schedule, Duration.of(100, TimeUnit.MILLISECONDS));

    System.out.println(results.timestamps);
    System.out.println(results.timelines);
    System.out.println(results.constraintViolations);
  }
}
