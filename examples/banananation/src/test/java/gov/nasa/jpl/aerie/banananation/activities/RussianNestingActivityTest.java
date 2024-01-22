package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.banananation.SimulationUtility;
import gov.nasa.jpl.aerie.banananation.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.banananation.generated.activities.ParameterTestActivityMapper;
import gov.nasa.jpl.aerie.banananation.generated.activities.RussianNestingBananaMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.banananation.generated.ActivityActions.call;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MILLISECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MerlinExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RussianNestingActivityTest {
  private final RussianNestingBananaMapper mapper;

  public RussianNestingActivityTest() {
    this.mapper = new RussianNestingBananaMapper();
  }

  @Test
  public void testDefaultSimulationDoesNotThrow() {
    final var schedule = SimulationUtility.buildSchedule(
        Pair.of(
            duration(1100, MILLISECONDS),
            new SerializedActivity("RussianNestingBanana",
                                   Map.of("pickBananaActivityRecord",
                                          SerializedValue.of(Map.of("id", SerializedValue.of(2),
                                                                    "pickBananaActivity", SerializedValue.of(Map.of("quantity", SerializedValue.of(10)))))
                                   , "pickBananaQuantityOverride", SerializedValue.of(0),
                                          "biteBananaActivity", SerializedValue.of(List.of()),
                                          "peelBananaActivity", SerializedValue.of(Map.of("peelDirection", SerializedValue.of("fromStem")))))));

    final var simulationDuration = duration(5, SECONDS);

    final var simulationResults = SimulationUtility.simulate(schedule, simulationDuration);

    System.out.println(simulationResults.discreteProfiles);
    System.out.println(simulationResults.realProfiles);
  }

}
