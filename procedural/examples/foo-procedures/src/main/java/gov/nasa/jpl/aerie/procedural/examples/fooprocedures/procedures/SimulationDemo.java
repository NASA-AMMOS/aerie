package gov.nasa.jpl.aerie.procedural.examples.fooprocedures.procedures;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.procedural.scheduling.Procedure;
import gov.nasa.jpl.aerie.procedural.scheduling.plan.EditablePlan;
import gov.nasa.jpl.aerie.procedural.scheduling.plan.NewDirective;
import gov.nasa.jpl.aerie.procedural.scheduling.annotations.SchedulingProcedure;
import gov.nasa.jpl.aerie.timeline.CollectOptions;
import gov.nasa.jpl.aerie.timeline.collections.profiles.Real;
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyDirective;
import gov.nasa.jpl.aerie.timeline.payloads.activities.DirectiveStart;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;

@SchedulingProcedure
public record SimulationDemo(int quantity) implements Procedure {
  @Override
  public void run(EditablePlan plan, @NotNull CollectOptions options) {
    final var firstActivityTime = plan.toRelative(Instant.from(DOY_WITHOUT_ZONE_FORMATTER.parse("2024-115T07:00:00")));

    plan.create(new NewDirective(
        new AnyDirective(
            Map.of("biteSize", SerializedValue.of(2))
        ),
        "First activity",
        "BiteBanana",
        new DirectiveStart.Absolute(firstActivityTime)
    ));

    final var simResults = plan.simulate();

    final var lowFruit = simResults.resource("/fruit", Real::deserialize).inspect(
        $ -> System.out.println("low fruit: " + $)
    ).lessThan(3.5).isolateTrue();
    final var bites = simResults.instances("BiteBanana").inspect(
        $ -> System.out.println("bites: " + $)
    );

    final var connections = lowFruit.starts().shift(Duration.MINUTE.negate()).connectTo(bites.ends(), false);

    for (final var connection: connections.collect(options)) {
        assert connection.to != null;
        plan.create(
          new NewDirective(
              new AnyDirective(
                  Map.of(
                      "quantity", SerializedValue.of(1),
                      "growingDuration", SerializedValue.of(Duration.HOUR.dividedBy(Duration.MICROSECOND))
                  )
              ),
              "Second Activity",
              "GrowBanana",
              new DirectiveStart.Anchor(
                  connection.to.directiveId,
                  Duration.minutes(30),
                  DirectiveStart.Anchor.AnchorPoint.End
              )
          )
      );
    }

    plan.commit();
  }

  private static final DateTimeFormatter DOY_WITHOUT_ZONE_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("uuuu-DDD'T'HH:mm:ss")
      .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
      .toFormatter()
      .withZone(ZoneOffset.UTC);
}
