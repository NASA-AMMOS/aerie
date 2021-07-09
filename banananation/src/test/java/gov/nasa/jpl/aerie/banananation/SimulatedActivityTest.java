package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.protocol.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.Duration.duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SimulatedActivityTest {
  @Test
  public void testUnspecifiedArgInSimulatedActivity() throws SimulationDriver.TaskSpecInstantiationException {
    final var schedule = SimulationUtility.buildSchedule(
        Pair.of(
            duration(0, SECONDS),
            new SerializedActivity("PeelBanana", Map.of())));

    final var simDuration = duration(1, SECOND);

    final var simulationResults = SimulationUtility.simulate(schedule, simDuration);

    assertEquals(1, simulationResults.simulatedActivities.size());
    simulationResults.simulatedActivities.forEach( (id, act) -> {
        assertEquals(1, act.parameters.size());
        assertTrue(act.parameters.containsKey("peelDirection"));
    });
  }
}
