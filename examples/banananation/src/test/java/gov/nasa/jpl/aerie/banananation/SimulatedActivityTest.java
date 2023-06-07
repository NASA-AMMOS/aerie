package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SimulatedActivityTest {
  @Test
  public void testRemoveAndAddActivity() {
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
    var simulationResults = driver.simulate(schedule1, startTime, simDuration, startTime, simDuration);
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    System.out.println("fruitProfile = " + fruitProfile);

    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(new HashMap<>(), startTime, simDuration, startTime, simDuration);
    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    System.out.println("fruitProfile = " + fruitProfile);

    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(schedule2, startTime, simDuration, startTime, simDuration);
    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    System.out.println("fruitProfile = " + fruitProfile);

//    assertEquals(1, simulationResults.getSimulatedActivities().size());
//
//    assertEquals(4.0, fruitProfile.get(fruitProfile.size()-1).dynamics().initial);
  }

  @Test
  public void testRemoveActivity() {
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
//    assertEquals(4.0, fruitProfile.get(fruitProfile.size()-1).dynamics().initial);
  }

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

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    simulationResults.getSimulatedActivities().forEach( (id, act) -> {
      assertEquals(1, act.arguments().size());
      assertTrue(act.arguments().containsKey("peelDirection"));
    });

    assertEquals(1, simulationResults.getUnfinishedActivities().size());
    simulationResults.getUnfinishedActivities().forEach( (id, act) -> {
      assertEquals(2, act.arguments().size());
      assertTrue(act.arguments().containsKey("quantity"));
      assertTrue(act.arguments().containsKey("growingDuration"));
    });
  }

  /** This test is a response to not accounting for all Task ExecutionStates
   * when collecting activities into the results object. This indirectly tests that portion
   * of {@link gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine#computeResults(Instant, Duration, Topic) computeResults()}
   *
   * The schedule in this test, results produces Tasks in all three of the states,
   * {@link gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine.ExecutionState.AwaitingChildren AwaitingChildren},
   * {@link gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine.ExecutionState.InProgress InProgress}, and
   * {@link gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine.ExecutionState.Terminated Terminated}.
   */
  @Test
  public void testCollectAllActivitiesInResults() {
    final var schedule = SimulationUtility.buildSchedule(
        Pair.of(
            duration(0, SECONDS),
            new SerializedActivity("PeelBanana", Map.of())),
        Pair.of(
            duration(0, SECONDS),
            new SerializedActivity("GrowBanana", Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", SerializedValue.of(Duration.SECOND.times(3).in(Duration.MICROSECONDS))
            ))),
        Pair.of(
            duration(0, SECONDS),
            new SerializedActivity("DecomposingSpawnParent", Map.of())));

    final var simDuration = duration(2, SECONDS);

    final var simulationResults = SimulationUtility.simulate(schedule, simDuration);

    assertEquals(2, simulationResults.getSimulatedActivities().size());

    var simulatedActivityTypes = new HashSet<String>();
    simulationResults.getSimulatedActivities().forEach( (id, act) -> simulatedActivityTypes.add(act.type()));
    Collection<String> expectedSimulated = new HashSet<>(
        Arrays.asList("PeelBanana", "DecomposingSpawnChild"));

    assertEquals(simulatedActivityTypes, expectedSimulated);

    assertEquals(3, simulationResults.getUnfinishedActivities().size());

    var unfinishedActivityTypes = new HashSet<String>();
    simulationResults.getUnfinishedActivities().forEach( (id, act) -> unfinishedActivityTypes.add(act.type()));

    Collection<String> expectedUnfinished = new HashSet<>(
        Arrays.asList("GrowBanana", "DecomposingSpawnChild", "DecomposingSpawnParent"));
    assertEquals(unfinishedActivityTypes, expectedUnfinished);
  }
}
