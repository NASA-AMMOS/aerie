package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.timeline.CausalEventSource;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.model.OutputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimulationDuplicationTest {
  CachedEngineStore store;
  final private class InfiniteCapacityEngineStore implements CachedEngineStore{
    private Map<SimulationEngineConfiguration, List<CheckpointSimulationDriver.CachedSimulationEngine>> store = new HashMap<>();
    @Override
    public void save(
        final CheckpointSimulationDriver.CachedSimulationEngine cachedSimulationEngine,
        final SimulationEngineConfiguration configuration)
    {
      store.computeIfAbsent(configuration, conf -> new ArrayList<>());
      store.get(configuration).add(cachedSimulationEngine);
    }

    @Override
    public List<CheckpointSimulationDriver.CachedSimulationEngine> getCachedEngines(final SimulationEngineConfiguration configuration) {
      return store.get(configuration);
    }
  }

  public static SimulationEngineConfiguration mockConfiguration(){
    return new SimulationEngineConfiguration(
        Map.of(),
        Instant.EPOCH,
        new MissionModelId(0)
    );
  }

  @BeforeEach
  void beforeEach(){
    this.store = new InfiniteCapacityEngineStore();
  }

  @Test
  void emptyPlanTest() {
    final SimulationResults results = SimulationDriver.simulate(
        TestMissionModel.missionModel(),
        Map.of(),
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        () -> false);
    final List<Triple<Integer, String, ValueSchema>> standardTopics = List.of(
        Triple.of(
            0,
            "ActivityType.Input.DelayActivityDirective",
            ValueSchema.ofStruct(Map.of())),
        Triple.of(
            1,
            "ActivityType.Output.DelayActivityDirective",
            ValueSchema.ofStruct(Map.of())),
        Triple.of(
            2,
            "ActivityType.Input.DecomposingActivityDirective",
            ValueSchema.ofStruct(Map.of())),
        Triple.of(
            3,
            "ActivityType.Output.DecomposingActivityDirective",
            ValueSchema.ofStruct(Map.of())));
    final SimulationResults expected = new SimulationResults(
        Map.of(),
        Map.of(),
        Map.of(),
        Map.of(),
        Instant.EPOCH,
        Duration.HOUR,
        standardTopics,
        new TreeMap<>());
    assertEquals(expected, results);
  }

  @Test
  void testDuplicate() {
    final var results = simulateWithCheckpoints(
        CheckpointSimulationDriver.CachedSimulationEngine.empty(TestMissionModel.missionModel()),
        List.of(Duration.of(5, MINUTES)),
        store);
    final SimulationResults expected = SimulationDriver.simulate(
        TestMissionModel.missionModel(),
        Map.of(),
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        () -> false,
        $ -> {});
    assertEquals(expected, results);
    final var newResults = simulateWithCheckpoints(store.getCachedEngines(mockConfiguration()).get(0), List.of(), store);
    assertEquals(expected, newResults);
  }

  static SimulationResults simulateWithCheckpoints(
      final CheckpointSimulationDriver.CachedSimulationEngine cachedEngine,
      final List<Duration> desiredCheckpoints,
      final CachedEngineStore engineStore
  ) {
    return SimulationResultsComputerInputs.computeResults(CheckpointSimulationDriver.simulateWithCheckpoints(
        TestMissionModel.missionModel(),
        Map.of(),
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        $ -> {},
        () -> false,
        cachedEngine,
        CheckpointSimulationDriver.desiredCheckpoints(desiredCheckpoints),
        CheckpointSimulationDriver.noCondition(),
        engineStore,
        mockConfiguration(),
        true
        ));
  }
}
