package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SimulatedActivityTest {
  @Test
  public void testUnspecifiedArgInSimulatedActivity() {
    final var schedule = SimulationUtility.buildSchedule(
        Pair.of(
            duration(0, SECONDS),
            new SerializedActivity("PeelBanana", Map.of())),
        Pair.of(
            duration(0, SECONDS),
            new SerializedActivity("GrowBanana", Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", SerializedValue.of(Duration.SECOND.times(2).in(Duration.MICROSECONDS))
            ))));

    final var simDuration = duration(1, SECOND);

    final var simulationResults = SimulationUtility.simulate(schedule, simDuration);

    assertEquals(1, simulationResults.simulatedActivities.size());
    simulationResults.simulatedActivities.forEach( (id, act) -> {
        assertEquals(1, act.arguments().size());
        assertTrue(act.arguments().containsKey("peelDirection"));
    });

    assertEquals(1, simulationResults.unfinishedActivities.size());
    simulationResults.unfinishedActivities.forEach( (id, act) -> {
      assertEquals(2, act.arguments().size());
      assertTrue(act.arguments().containsKey("quantity"));
      assertTrue(act.arguments().containsKey("growingDuration"));
    });
  }
}
