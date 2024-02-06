package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.CheckpointSimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationEngineConfiguration;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SlabList;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.CausalEventSource;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.SimulationUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InMemoryCachedEngineStoreTest {
  SimulationEngineConfiguration simulationEngineConfiguration;
  MissionModelId missionModelId;

  @BeforeEach
  void beforeEach(){
    this.missionModelId = new MissionModelId(1);
    this.simulationEngineConfiguration = new SimulationEngineConfiguration(Map.of(), Instant.EPOCH, this.missionModelId);
  }

  public static CheckpointSimulationDriver.CachedSimulationEngine getCachedEngine1(){
    return new CheckpointSimulationDriver.CachedSimulationEngine(
        Duration.SECOND,
        Map.of(
            new ActivityDirectiveId(1), new ActivityDirective(Duration.HOUR, "ActivityType1", Map.of(), null, true),
            new ActivityDirectiveId(2), new ActivityDirective(Duration.HOUR, "ActivityType2", Map.of(), null, true)
        ),
        new SimulationEngine(),
        new LiveCells(new CausalEventSource()),
        new SlabList<>(),
        null,
        SimulationUtility.getFooMissionModel()
    );
  }

  public static CheckpointSimulationDriver.CachedSimulationEngine getCachedEngine2(){
    return new CheckpointSimulationDriver.CachedSimulationEngine(
        Duration.SECOND,
        Map.of(
            new ActivityDirectiveId(3), new ActivityDirective(Duration.HOUR, "ActivityType3", Map.of(), null, true),
            new ActivityDirectiveId(4), new ActivityDirective(Duration.HOUR, "ActivityType4", Map.of(), null, true)
        ),
        new SimulationEngine(),
        new LiveCells(new CausalEventSource()),
        new SlabList<>(),
        null,
        SimulationUtility.getFooMissionModel()
    );
  }

  public static CheckpointSimulationDriver.CachedSimulationEngine getCachedEngine3(){
    return new CheckpointSimulationDriver.CachedSimulationEngine(
        Duration.SECOND,
        Map.of(
            new ActivityDirectiveId(5), new ActivityDirective(Duration.HOUR, "ActivityType5", Map.of(), null, true),
            new ActivityDirectiveId(6), new ActivityDirective(Duration.HOUR, "ActivityType6", Map.of(), null, true)
        ),
        new SimulationEngine(),
        new LiveCells(new CausalEventSource()),
        new SlabList<>(),
        null,
        SimulationUtility.getFooMissionModel()
    );
  }

  @Test
  public void duplicateTest(){
    final var store = new InMemoryCachedEngineStore(2);
    store.save(CheckpointSimulationDriver.CachedSimulationEngine.empty(SimulationUtility.getFooMissionModel()), this.simulationEngineConfiguration);
    store.save(CheckpointSimulationDriver.CachedSimulationEngine.empty(SimulationUtility.getFooMissionModel()), this.simulationEngineConfiguration);
    store.save(CheckpointSimulationDriver.CachedSimulationEngine.empty(SimulationUtility.getFooMissionModel()), this.simulationEngineConfiguration);
    assertEquals(1, store.getCachedEngines(this.simulationEngineConfiguration).size());
  }

  @Test
  public void order(){
    final var store = new InMemoryCachedEngineStore(2);
    final var cachedEngine1 = getCachedEngine1();
    final var cachedEngine2 = getCachedEngine2();
    final var cachedEngine3 = getCachedEngine3();
    store.save(cachedEngine1, this.simulationEngineConfiguration);
    store.save(cachedEngine2, this.simulationEngineConfiguration);
    final var cachedBeforeRegister = store.getCachedEngines(this.simulationEngineConfiguration);
    //engines have 0 used, so they are ordered in descending creation date
    assertEquals(cachedBeforeRegister.get(0).activityDirectives(), cachedEngine1.activityDirectives());
    assertEquals(cachedBeforeRegister.get(1).activityDirectives(), cachedEngine2.activityDirectives());
    //engine1 has been used so it goes first in the list
    store.registerUsed(cachedEngine2);
    final var cachedAfterRegister = store.getCachedEngines(this.simulationEngineConfiguration);
    assertEquals(cachedAfterRegister.get(0).activityDirectives(), cachedEngine2.activityDirectives());
    assertEquals(cachedAfterRegister.get(1).activityDirectives(), cachedEngine1.activityDirectives());
    store.save(cachedEngine3, this.simulationEngineConfiguration);
    //to store cachedEngine3, we had to remove the last element of the list, engine 1 and the order is still most recently used
    final var cachedAfterRemoveLast = store.getCachedEngines(this.simulationEngineConfiguration);
    assertEquals(cachedAfterRemoveLast.get(0).activityDirectives(), cachedEngine2.activityDirectives());
    assertEquals(cachedAfterRemoveLast.get(1).activityDirectives(), cachedEngine3.activityDirectives());
    System.out.println();
  }
}
