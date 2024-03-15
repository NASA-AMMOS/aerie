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
        missionModel,
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
        CheckpointSimulationDriver.CachedSimulationEngine.empty(missionModel),
        List.of(Duration.of(5, MINUTES)),
        store);
    final SimulationResults expected = SimulationDriver.simulate(
        missionModel,
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
    return CheckpointSimulationDriver.computeResults(CheckpointSimulationDriver.simulateWithCheckpoints(
        missionModel,
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
        mockConfiguration()
        ));
  }

  private static final Topic<Object> delayedActivityDirectiveInputTopic = new Topic<>();
  private static final Topic<Object> delayedActivityDirectiveOutputTopic = new Topic<>();

  private static final InputType<Object> testModelInputType = new InputType<>() {
    @Override
    public List<Parameter> getParameters() {
      return List.of();
    }

    @Override
    public List<String> getRequiredParameters() {
      return List.of();
    }

    @Override
    public Object instantiate(final Map arguments) {
      return new Object();
    }

    @Override
    public Map<String, SerializedValue> getArguments(final Object value) {
      return Map.of();
    }

    @Override
    public List<ValidationNotice> getValidationFailures(final Object value) {
      return List.of();
    }
  };

  private static final OutputType<Object> testModelOutputType = new OutputType<>() {
    @Override
    public ValueSchema getSchema() {
      return ValueSchema.ofStruct(Map.of());
    }

    @Override
    public SerializedValue serialize(final Object value) {
      return SerializedValue.of(Map.of());
    }
  };

  /* package-private*/ static final DirectiveType<Object, Object, Object> delayedActivityDirective = new DirectiveType<>() {
    @Override
    public InputType<Object> getInputType() {
      return testModelInputType;
    }

    @Override
    public OutputType<Object> getOutputType() {
      return testModelOutputType;
    }

    @Override
    public TaskFactory<Object> getTaskFactory(final Object o, final Object o2) {
      return executor -> oneShotTask($ -> {
        $.emit(this, delayedActivityDirectiveInputTopic);
        return TaskStatus.delayed(Duration.MINUTE, oneShotTask($$ -> {
          $$.emit(Unit.UNIT, delayedActivityDirectiveOutputTopic);
          return TaskStatus.completed(Unit.UNIT);
        }));
      });
    }
  };

  private static final Topic<Object> decomposingActivityDirectiveInputTopic = new Topic<>();
  private static final Topic<Object> decomposingActivityDirectiveOutputTopic = new Topic<>();
  /* package-private */  static final DirectiveType<Object, Object, Object> decomposingActivityDirective = new DirectiveType<>() {
    @Override
    public InputType<Object> getInputType() {
      return testModelInputType;
    }

    @Override
    public OutputType<Object> getOutputType() {
      return testModelOutputType;
    }

    @Override
    public TaskFactory<Object> getTaskFactory(final Object o, final Object o2) {
      return executor -> oneShotTask(scheduler -> {
        scheduler.emit(this, decomposingActivityDirectiveInputTopic);
        return TaskStatus.delayed(
            Duration.ZERO,
            oneShotTask($ -> {
              try {
                $.spawn(delayedActivityDirective.getTaskFactory(null, null));
              } catch (final InstantiationException ex) {
                throw new Error("Unexpected state: activity instantiation of DelayedActivityDirective failed with: %s".formatted(
                    ex.toString()));
              }
              return TaskStatus.delayed(Duration.of(120, Duration.SECOND), oneShotTask($$ -> {
                try {
                  $$.spawn(delayedActivityDirective.getTaskFactory(null, null));
                } catch (final InstantiationException ex) {
                  throw new Error(
                      "Unexpected state: activity instantiation of DelayedActivityDirective failed with: %s".formatted(
                          ex.toString()));
                }
                $$.emit(Unit.UNIT, decomposingActivityDirectiveOutputTopic);
                return TaskStatus.completed(Unit.UNIT);
              }));
            }));
      });
    }
  };

  static MissionModel<?> missionModel = new MissionModel<>(
      new Object(),
      new LiveCells(new CausalEventSource()),
      Map.of(),
      List.of(
          new MissionModel.SerializableTopic<>(
              "ActivityType.Input.DelayActivityDirective",
              delayedActivityDirectiveInputTopic,
              testModelOutputType),
          new MissionModel.SerializableTopic<>(
              "ActivityType.Output.DelayActivityDirective",
              delayedActivityDirectiveOutputTopic,
              testModelOutputType),
          new MissionModel.SerializableTopic<>(
              "ActivityType.Input.DecomposingActivityDirective",
              decomposingActivityDirectiveInputTopic,
              testModelOutputType),
          new MissionModel.SerializableTopic<>(
              "ActivityType.Output.DecomposingActivityDirective",
              decomposingActivityDirectiveOutputTopic,
              testModelOutputType)),
      List.of(),
      DirectiveTypeRegistry.extract(
          new ModelType<>() {

            @Override
            public Map<String, ? extends DirectiveType<Object, ?, ?>> getDirectiveTypes() {
              return Map.of(
                  "DelayActivityDirective",
                  delayedActivityDirective,
                  "DecomposingActivityDirective",
                  decomposingActivityDirective);
            }

            @Override
            public InputType<Object> getConfigurationType() {
              return testModelInputType;
            }

            @Override
            public Object instantiate(
                final Instant planStart,
                final Object configuration,
                final Initializer builder)
            {
              return new Object();
            }
          }
      )
  );

  private static <T> Task<T> oneShotTask(Function<Scheduler, TaskStatus<T>> f) {
    return new Task<>() {
      @Override
      public TaskStatus<T> step(final Scheduler scheduler) {
        return f.apply(scheduler);
      }

      @Override
      public Task<T> duplicate(Executor executor) {
        return this;
      }
    };
  }
}
