package gov.nasa.jpl.aerie.foomissionmodel;

import gov.nasa.jpl.aerie.foomissionmodel.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.framework.ThreadedTask;
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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTE;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimulationDuplicationTest {
  @BeforeAll
  static void beforeAll() {
    ThreadedTask.CACHE_READS = true;
  }

  private static MissionModel<Mission> makeMissionModel(final MissionModelBuilder builder, final Instant planStart, final Configuration config) {
    final var factory = new GeneratedModelType();
    final var registry = DirectiveTypeRegistry.extract(factory);
    final var model = factory.instantiate(planStart, config, builder);
    return builder.build(model, registry);
  }

  @Test
  void emptyPlanTest() {
    final SimulationResults results = SimulationDriver.simulate(
        missionModel,
        Map.of(),
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR);
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
    assertResultsEqual(expected, results);
  }

  @Test
  void testTrivialDuplicate() {
    final SimulationDriver.SimulationResultsWithCheckpoints results = simulateWithCheckpoints(
        missionModel,
        SimulationDriver.CachedSimulationEngine.empty(), List.of(Duration.of(5, MINUTES)), Map.of()
    );
    final SimulationResults expected = SimulationDriver.simulate(
        missionModel,
        Map.of(),
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        $ -> {});
    assertResultsEqual(expected, results.results());
    final SimulationDriver.SimulationResultsWithCheckpoints newResults = simulateWithCheckpoints(
        missionModel,
        results.checkpoints().get(0), List.of(), Map.of()
    );
    assertResultsEqual(expected, newResults.results());
  }

  @Test
  void testFooDuplicateEmptyPlan() {
    final MissionModel<Mission> missionModel = makeMissionModel(
        new MissionModelBuilder(),
        Instant.EPOCH,
        new Configuration());
    final SimulationDriver.SimulationResultsWithCheckpoints results = simulateWithCheckpoints(
        missionModel,
        List.of(Duration.of(5, MINUTES)),
        Map.of()
    );
    final SimulationResults expected = SimulationDriver.simulate(
        missionModel,
        Map.of(),
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        $ -> {});
    assertResultsEqual(expected, results.results());
  }

  @Test
  void testFooNonEmptyPlan() {
    final MissionModel<Mission> missionModel = makeMissionModel(
        new MissionModelBuilder(),
        Instant.EPOCH,
        new Configuration());
    final Map<ActivityDirectiveId, ActivityDirective> schedule = Map.ofEntries(
        activity(1, MINUTE, "foo", Map.of("z", SerializedValue.of(123))),
        activity(7, MINUTES, "foo", Map.of("z", SerializedValue.of(999)))
    );
    final SimulationDriver.SimulationResultsWithCheckpoints results = simulateWithCheckpoints(
        missionModel,
        List.of(Duration.of(5, MINUTES)),
        schedule
    );
    final SimulationResults expected = SimulationDriver.simulate(
        missionModel,
        schedule,
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        $ -> {});
    assertResultsEqual(expected, results.results());

    assertEquals(Duration.of(5, MINUTES), results.checkpoints().get(0).startOffset());

    final SimulationDriver.SimulationResultsWithCheckpoints results2 = simulateWithCheckpoints(
        missionModel,
        results.checkpoints().get(0),
        List.of(Duration.of(5, MINUTES)),
        schedule
    );

    assertResultsEqual(expected, results2.results());
  }

  @Test
  void testFooNonEmptyPlanMultipleResumes() {
    final MissionModel<Mission> missionModel = makeMissionModel(
        new MissionModelBuilder(),
        Instant.EPOCH,
        new Configuration());
    final Map<ActivityDirectiveId, ActivityDirective> schedule = Map.ofEntries(
        activity(1, MINUTE, "foo", Map.of("z", SerializedValue.of(123))),
        activity(7, MINUTES, "foo", Map.of("z", SerializedValue.of(999)))
    );
    final SimulationDriver.SimulationResultsWithCheckpoints results = simulateWithCheckpoints(
        missionModel,
        List.of(Duration.of(5, MINUTES)),
        schedule
    );
    final SimulationResults expected = SimulationDriver.simulate(
        missionModel,
        schedule,
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        $ -> {});
    assertResultsEqual(expected, results.results());

    assertEquals(Duration.of(5, MINUTES), results.checkpoints().get(0).startOffset());

    final SimulationDriver.SimulationResultsWithCheckpoints results2 = simulateWithCheckpoints(
        missionModel,
        results.checkpoints().get(0),
        List.of(Duration.of(5, MINUTES)),
        schedule
    );

    assertResultsEqual(expected, results2.results());

    final SimulationDriver.SimulationResultsWithCheckpoints results3 = simulateWithCheckpoints(
        missionModel,
        results.checkpoints().get(0),
        List.of(Duration.of(5, MINUTES)),
        schedule
    );

    assertResultsEqual(expected, results3.results());
  }

  private static long nextActivityDirectiveId = 0L;

  private static Pair<ActivityDirectiveId, ActivityDirective> activity(final long quantity, final Duration unit, final String type) {
    return activity(quantity, unit, type, Map.of());
  }
  private static Pair<ActivityDirectiveId, ActivityDirective> activity(final Duration startOffset, final String type) {
    return activity(startOffset, type, Map.of());
  }

  private static Pair<ActivityDirectiveId, ActivityDirective> activity(final long quantity, final Duration unit, final String type, final Map<String, SerializedValue> args) {
    return activity(Duration.of(quantity, unit), type, args);
  }

  private static Pair<ActivityDirectiveId, ActivityDirective> activity(final Duration startOffset, final String type, final Map<String, SerializedValue> args) {
    if (nextActivityDirectiveId > 1) {
      System.out.println();
    }
    return Pair.of(new ActivityDirectiveId(nextActivityDirectiveId++), new ActivityDirective(startOffset, type, args, null, true));
  }


  static void assertResultsEqual(SimulationResults expected, SimulationResults actual) {
    if (expected.equals(actual)) return;
    final var differences = new ArrayList<String>();
    if (!expected.duration.isEqualTo(actual.duration)) {
      differences.add("duration");
    }
    if (!expected.realProfiles.equals(actual.realProfiles)) {
      differences.add("realProfiles");
    }
    if (!expected.discreteProfiles.equals(actual.discreteProfiles)) {
      differences.add("discreteProfiles");
    }
    if (!expected.simulatedActivities.equals(actual.simulatedActivities)) {
      differences.add("simulatedActivities");
    }
    if (!expected.unfinishedActivities.equals(actual.unfinishedActivities)) {
      differences.add("unfinishedActivities");
    }
    if (!expected.startTime.equals(actual.startTime)) {
      differences.add("startTime");
    }
    if (!expected.duration.isEqualTo(actual.duration)) {
      differences.add("duration");
    }
    if (!expected.topics.equals(actual.topics)) {
      differences.add("topics");
    }
    if (!expected.events.equals(actual.events)) {
      differences.add("events");
    }
    if (!differences.isEmpty()) {
      System.out.println();
    }
    System.out.println(differences);
    assertEquals(expected, actual);
  }

  static SimulationDriver.SimulationResultsWithCheckpoints simulateWithCheckpoints(
      final MissionModel<?> missionModel,
      final SimulationDriver.CachedSimulationEngine cachedSimulationEngine,
      final List<Duration> desiredCheckpoints,
      final Map<ActivityDirectiveId, ActivityDirective> schedule
  ) {
    return SimulationDriver.simulateWithCheckpoints(
        missionModel,
        schedule,
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        $ -> {},
        desiredCheckpoints,
        cachedSimulationEngine);
  }

  static SimulationDriver.SimulationResultsWithCheckpoints simulateWithCheckpoints(
      final MissionModel<?> missionModel,
      final List<Duration> desiredCheckpoints,
      final Map<ActivityDirectiveId, ActivityDirective> schedule
  ) {
    final SimulationEngine engine = new SimulationEngine();
    final TemporalEventSource timeline = new TemporalEventSource();
    final LiveCells cells = new LiveCells(timeline, missionModel.getInitialCells());

    // Begin tracking all resources.
    for (final var entry : missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();

      engine.trackResource(name, resource, Duration.ZERO);
    }

    {
      // Start daemon task(s) immediately, before anything else happens.
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
      {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
        final var commit = engine.performJobs(batch.jobs(), cells, Duration.ZERO, Duration.MAX_VALUE);
        timeline.add(commit);
      }
    }

    final var cachedSimulationEngine = new SimulationDriver.CachedSimulationEngine(
        Duration.ZERO,
        List.of(),
        engine,
        cells,
        timeline.points(),
        new Topic<>()
    );

    return SimulationDriver.simulateWithCheckpoints(
        missionModel,
        schedule,
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        $ -> {},
        desiredCheckpoints,
        cachedSimulationEngine);
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

  public static <T> Task<T> oneShotTask(Function<Scheduler, TaskStatus<T>> f) {
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
      new LiveCells(null),
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
}
