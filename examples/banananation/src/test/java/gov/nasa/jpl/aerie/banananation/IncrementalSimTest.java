package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IncrementalSimTest {
  private static boolean debug = false;
  @Test
  public void testRemoveAndAddActivity() {
    if (debug) System.out.println("testRemoveAndAddActivity()");
    final var schedule1 = SimulationUtility.buildSchedule(
        Pair.of(
            duration(5, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );
    final var schedule2 = SimulationUtility.buildSchedule(
        Pair.of(
            duration(3, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();

    // Add PeelBanana at time = 5
    var simulationResults = driver.simulate(schedule1, startTime, simDuration, startTime, simDuration);
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruitProfile = " + fruitProfile);

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    assertEquals(2, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(Duration.of(5, SECONDS), fruitProfile.get(0).extent());
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);

    // Remove PeelBanana (back to empty schedule)
    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(new HashMap<>(), startTime, simDuration, startTime, simDuration);
    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruitProfile = " + fruitProfile);

    assertEquals(0, simulationResults.getSimulatedActivities().size());
    assertEquals(1, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);

    // Add PeelBanana at time = 3
    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(schedule2, startTime, simDuration, startTime, simDuration);
    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruitProfile = " + fruitProfile);

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    assertEquals(2, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(Duration.of(3, SECONDS), fruitProfile.get(0).extent());
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);
  }

  @Test
  public void testRemoveActivity() {
    if (debug) System.out.println("testRemoveActivity()");

    final var schedule = SimulationUtility.buildSchedule(
        Pair.of(
            duration(5, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    var simulationResults = driver.simulate(schedule, startTime, simDuration, startTime, simDuration);
    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(new HashMap<>(), startTime, simDuration, startTime, simDuration);

    assertEquals(0, simulationResults.getSimulatedActivities().size());

    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    assertEquals(4.0, fruitProfile.get(fruitProfile.size()-1).dynamics().initial);
  }

  @Test
  public void testMoveActivityLater() {
    if (debug) System.out.println("testMoveActivityLater()");

    final var schedule1 = SimulationUtility.buildSchedule(
        Pair.of(
            duration(3, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );
    final var schedule2 = SimulationUtility.buildSchedule(
        Pair.of(
            duration(5, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    var simulationResults = driver.simulate(schedule1, startTime, simDuration, startTime, simDuration);
    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(schedule2, startTime, simDuration, startTime, simDuration);

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    assertEquals(3.0, fruitProfile.get(fruitProfile.size()-1).dynamics().initial);
  }

  @Test
  public void testMoveActivityPastAnother() {
    if (debug) System.out.println("testMoveActivityLater()");

    final var schedule = SimulationUtility.buildSchedule(
        Pair.of(
            duration(3, SECONDS),
            new SerializedActivity("PeelBanana", Map.of())),
        Pair.of(
            duration(5, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    var simulationResults = driver.simulate(schedule, startTime, simDuration, startTime, simDuration);

    final Map.Entry<ActivityDirectiveId, ActivityDirective> firstEntry = schedule.entrySet().iterator().next();
    final ActivityDirective directive1 = firstEntry.getValue();
    final ActivityDirectiveId key1 = firstEntry.getKey();
    assertEquals(Duration.of(3, SECONDS), directive1.startOffset());
    schedule.put(key1, new ActivityDirective(Duration.of(7, SECONDS), directive1.serializedActivity(), directive1.anchorId(), directive1.anchoredToStart()));

    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(schedule, startTime, simDuration, startTime, simDuration);

    assertEquals(2, simulationResults.getSimulatedActivities().size());
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    assertEquals(3, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);
    assertEquals(2.0, fruitProfile.get(2).dynamics().initial);
  }

}
