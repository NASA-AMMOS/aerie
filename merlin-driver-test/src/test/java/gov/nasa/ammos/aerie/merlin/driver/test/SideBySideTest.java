package gov.nasa.ammos.aerie.merlin.driver.test;

import gov.nasa.ammos.aerie.simulation.protocol.Directive;
import gov.nasa.ammos.aerie.simulation.protocol.Results;
import gov.nasa.ammos.aerie.simulation.protocol.Schedule;
import gov.nasa.ammos.aerie.simulation.protocol.Simulator;
import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.banananation.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.driver.develop.MerlinDriverAdapter;
import gov.nasa.jpl.aerie.merlin.driver.IncrementalSimAdapter;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static gov.nasa.ammos.aerie.merlin.driver.test.Scenario.rightmostNumber;
import static gov.nasa.ammos.aerie.merlin.driver.test.TestRegistrar.schedulerOfQuerier;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MILLISECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Unit.UNIT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SideBySideTest {
  private static final ModelType<Configuration, Mission> MODEL = new GeneratedModelType();
  private static final Configuration CONFIG = new Configuration(
      Configuration.DEFAULT_PLANT_COUNT,
      Configuration.DEFAULT_PRODUCER,
      Path.of("/etc/hosts"),
      Configuration.DEFAULT_INITIAL_CONDITIONS,
      false);
  private static final Instant START = Instant.EPOCH;
  private static final Duration PLAN_DURATION = duration(10, SECONDS);
  private TestRegistrar model;

  @BeforeEach
  void setup() {
    model = new TestRegistrar();

//    final var builder = new MissionModelBuilder();
//    MISSION_MODEL = builder.build(MODEL.instantiate(START, CONFIG, builder), DirectiveTypeRegistry.extract(MODEL));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testSideBySide() {
    final var incrementalSimulator = (Simulator) new IncrementalSimAdapter(MODEL, CONFIG, START, PLAN_DURATION);
    final var regularSimulator = new MerlinDriverAdapter<>(MODEL, CONFIG, START, PLAN_DURATION);

    final var schedule1 = Schedule.build(Pair.of(duration(1, SECOND), new Directive("BiteBanana", Map.of())));
    final var schedule2 = Schedule.build(
        Pair.of(duration(1, SECOND), new Directive("BiteBanana", Map.of())),
        Pair.of(duration(2, SECOND), new Directive("BiteBanana", Map.of())));

    final var regResult1 = regularSimulator.simulate(schedule1);
    final Results incResult1 = incrementalSimulator.simulate(schedule1);

    assertEquals(regResult1, incResult1);

    final var regResult2 = regularSimulator.simulate(schedule2);
    final var incResult2 = incrementalSimulator.simulate(schedule2);

    assertEquals(regResult2, incResult2);
  }

  public record Cell(Topic<String> topic, Topic<LinearDynamicsEffect> linearTopic, boolean isLinear) {
    public static Cell of()
    {
      return new Cell(new Topic<>(), new Topic<>(), false);
    }

    public static Cell ofLinear()
    {
      return new Cell(new Topic<>(), new Topic<>(), true);
    }

    public void emit(String event) {
      TestContext.get().scheduler().emit(event, this.topic);
    }

    public void emit(int number) {
      this.emit(String.valueOf(number));
    }

    public void setRate(final double newRate) {
      TestContext.get().scheduler().emit(new LinearDynamicsEffect(newRate, null), this.linearTopic);
    }

    public void setInitialValue(final double newInitialValue) {
      TestContext.get().scheduler().emit(new LinearDynamicsEffect(null, newInitialValue), this.linearTopic);
    }

    public double getLinear() {
      final var context = TestContext.get();
      final var scheduler = context.scheduler();
      final var cellId = context.cells().get(this);
      final MutableObject<LinearDynamics> state = (MutableObject<LinearDynamics>) scheduler.get(Objects.requireNonNull(cellId));
      return state.getValue().initialValue;
    }

    public double getRate() {
      final var context = TestContext.get();
      final var scheduler = context.scheduler();
      final var cellId = context.cells().get(this);
      final MutableObject<LinearDynamics> state = (MutableObject<LinearDynamics>) scheduler.get(Objects.requireNonNull(cellId));
      return state.getValue().rate;
    }

    public int getRightmostNumber() {
      return rightmostNumber(this.get().toString());
    }

    public int getNum() {
      for (final var entry : this.get().timeline.reversed()) {
        if (!(entry instanceof TimePoint.Commit e)) continue;
        final int num = rightmostNumber(e.toString());
        if (num != -1) return num;
      }
      return 0;
    }

    @SuppressWarnings("unchecked")
    public History get() {
      final var context = TestContext.get();
      final var scheduler = context.scheduler();
      final var cellId = context.cells().get(this);
      final MutableObject<History> state = (MutableObject<History>) scheduler.get(Objects.requireNonNull(cellId));
      return state.getValue();
    }
  }

  @Test
  void testAlternateInlineMissionModel() {
    final var cell1 = model.cell();
    final var cell2 = model.cell();

    model.activity("abc", $ -> {
      assertGraphEquals("", cell1.get());
      assertGraphEquals("", cell2.get());

      cell1.emit("3");

      assertGraphEquals("3", cell1.get());
      assertGraphEquals("", cell2.get());

      cell2.emit("4");

      assertGraphEquals("3", cell1.get());
      assertGraphEquals("4", cell2.get());

      cell1.emit("1");
      cell2.emit("2");

      assertGraphEquals("3; 1", cell1.get());
      assertGraphEquals("4; 2", cell2.get());
    });

    model.resource("cell1", cell1::get);

    incrementalSimTestCase(
        model.asModelType(),
        Unit.UNIT,
        START,
        duration(10, SECONDS),
        Schedule.build(Pair.of(duration(1, SECOND), new Directive("abc", Map.of()))),
//            Pair.of(duration(2, SECOND), new Directive("abc", Map.of())),
//            Pair.of(duration(3, SECOND), new Directive("abc", Map.of()))),
        Schedule.empty());
  }

  @Test
  void testDelay() {
    final var model = new TestRegistrar();
    final var cell1 = model.cell();
    final var cell = model.cell();

    model.activity("abc", $ -> {
//      assertGraphEquals("", cell1.get());
//      assertGraphEquals("", cell.get());
//
//      cell1.emit("a");
//
//      assertGraphEquals("a", cell1.get());
//      assertGraphEquals("", cell.get());
//
      delay(duration(2, SECOND));
//
//      assertGraphEquals("a", cell1.get());
//      assertGraphEquals("", cell.get());
//
//      delay(duration(2, SECOND));
//
//      assertGraphEquals("a", cell1.get());
//      assertGraphEquals("x", cell.get());
//
//      cell1.emit("b");
//
//      assertGraphEquals("a; b", cell1.get());
//      assertGraphEquals("x", cell.get());
//
//      delay(duration(2, SECOND));
//
//      assertGraphEquals("a; b", cell1.get());
//      assertGraphEquals("x", cell.get());
    });

//    model.activity("def", $ -> {
////      assertGraphEquals("a", cell1.get());
//      assertGraphEquals("", cell.get());
//
//      delay(duration(2, SECOND));
//
////      assertGraphEquals("a", cell1.get());
//      assertGraphEquals("", cell.get());
//
//      cell.emit("x");
//
////      assertGraphEquals("a", cell1.get());
//      assertGraphEquals("x", cell.get());
//
//      delay(duration(2, SECOND));
//
////      assertGraphEquals("a; b", cell1.get());
//      assertGraphEquals("x", cell.get());
//    });

    model.activity("def", $ -> {
      cell.emit("x");
      assertGraphEquals("x", cell.get());
      delay(duration(2, SECOND));
      assertGraphEquals("x", cell.get());
      cell.emit("y");
      assertGraphEquals("x; y", cell.get());
      delay(duration(2, SECOND));
      assertGraphEquals("x; y", cell.get());
    });

    incrementalSimTestCase(
        model.asModelType(),
        Unit.UNIT,
        START,
        duration(10, SECONDS),
        Schedule.build(
            Pair.of(duration(2, SECOND), new Directive("def", Map.of()))),
        Schedule.build(
            Pair.of(duration(1, SECOND).plus(duration(500, MILLISECONDS)), new Directive("def", Map.of()))));
  }

  private static void assertGraphEquals(String expected, History actual) {
    assertEquals(expected, actual.toString());
  }

  public static void delay(Duration duration) {
    TestContext.get().threadedTask().thread().delay(duration);
  }

  public static void spawn(Runnable task) {
    final TestContext.CellMap cells = TestContext.get().cells();
    TestContext.get().scheduler().spawn(InSpan.Fresh, x -> ThreadedTask.of(x, cells, () -> {
      task.run();
      return UNIT;
    }));
  }

  public static void call(Runnable task) {
    final TestContext.CellMap cells = TestContext.get().cells();
    TestContext.get().threadedTask().thread().call(InSpan.Fresh, x -> ThreadedTask.of(x, cells, () -> {
      task.run();
      return UNIT;
    }));
  }

  public static void waitUntil(Function<Duration, Optional<Duration>> condition) {
    final var cells = TestContext.get().cells();
    TestContext.get().threadedTask().thread().waitUntil((now, atLatest) -> {
      TestContext.set(new TestContext.Context(cells, schedulerOfQuerier(now), null));
      try {
        return condition.apply(atLatest);
      } finally {
        TestContext.clear();
      }
    });
  }

  public static void waitUntil(Supplier<Boolean> condition) {
    waitUntil($ -> condition.get() ? Optional.of(ZERO) : Optional.empty());
  }

  public static <Config, Model> void incrementalSimTestCase(
      ModelType<Config, Model> modelType,
      Config config,
      Instant startTime,
      Duration duration,
      Schedule... schedules)
  {
    final var incrementalSimulator = (Simulator) new IncrementalSimAdapter(modelType, config, startTime, duration);
    final var regularSimulator = new MerlinDriverAdapter<>(modelType, config, startTime, duration);
    for (final var schedule : schedules) {
      System.out.println("Running regular simulator");
      final var results = regularSimulator.simulate(schedule);
      System.out.println("Running incremental simulator");
      final Results incrementalResultsWithCache = incrementalSimulator.simulate(schedule);
//      assertEquals(results, incrementalResultsWithCache); // TODO use a more nuanced equality check
    }
  }

  private sealed interface TimePoint {
    record Commit(EventGraph<String> graph) implements TimePoint {}

    record Delay(Duration duration) implements TimePoint {}
  }

  public record LinearDynamics(double rate, double initialValue) {}
  public record LinearDynamicsEffect(Double newRate, Double newValue) {
    static LinearDynamicsEffect empty() {
      return new LinearDynamicsEffect(null, null);
    }
    boolean isEmpty() {
      return newRate == null && newValue == null;
    }
  }

  public static class History {
    final ArrayList<TimePoint> timeline = new ArrayList<>();

    public static History empty() {
      return new History();
    }

    public static History sequentially(History prefix, History suffix) {
      if (suffix.timeline.isEmpty()) return prefix;
      if (prefix.timeline.isEmpty()) return suffix;
      if (prefix.timeline.getLast() instanceof TimePoint.Delay p
          && suffix.timeline.getFirst() instanceof TimePoint.Delay s) {
        final var result = new History();
        result.timeline.addAll(prefix.timeline);
        result.timeline.removeLast();
        result.timeline.add(new TimePoint.Delay(p.duration.plus(s.duration)));
        for (int i = 1; i < suffix.timeline.size(); i++) { // Skip the first item
          final var it = suffix.timeline.get(i);
          result.timeline.add(it);
        }
        return result;
      } else if (prefix.timeline.getLast() instanceof TimePoint.Commit p
                 && suffix.timeline.getFirst() instanceof TimePoint.Commit s) {
        final var result = new History();
        result.timeline.addAll(prefix.timeline);
        result.timeline.removeLast();
        result.timeline.add(new TimePoint.Commit(EventGraph.sequentially(p.graph, s.graph)));
        for (int i = 1; i < suffix.timeline.size(); i++) { // Skip the first item
          final var it = suffix.timeline.get(i);
          result.timeline.add(it);
        }
        return result;
      } else {
        final var result = new History();
        result.timeline.addAll(prefix.timeline);
        result.timeline.addAll(suffix.timeline);
        return result;
      }
    }

    public static History concurrently(History left, History right) {
      if (left.timeline.isEmpty()) return right;
      if (right.timeline.isEmpty()) return left;
      if (left.timeline.size() == 1 && right.timeline.size() == 1) {
        if (left.timeline.getFirst() instanceof TimePoint.Commit l
            && right.timeline.getFirst() instanceof TimePoint.Commit r) {
          final var res = new History();
          res.timeline.add(new TimePoint.Commit(rebalance((EventGraph.Concurrently<String>) EventGraph.concurrently(r.graph, l.graph))));
          return res;
        } else {
          throw new IllegalArgumentException("Cannot concurrently compose delays and commits: " + left + " | " + right);
        }
      } else {
        throw new IllegalArgumentException("Cannot concurrently compose non unit-length histories: "
                                           + left
                                           + " | "
                                           + right);
      }
    }

    static <T> EventGraph.Concurrently<T> rebalance(EventGraph.Concurrently<T> graph) {
      final List<EventGraph<T>> sorted = expandConcurrently(graph);
      sorted.sort(Comparator.comparing(EventGraph::toString));
      var res = EventGraph.<T>empty();
      for (final var item : sorted.reversed()) {
        res = EventGraph.concurrently(item, res);
      }
      return (EventGraph.Concurrently<T>) res;
    }

    static <T> List<EventGraph<T>> expandConcurrently(EventGraph.Concurrently<T> graph) {
      final var res = new ArrayList<EventGraph<T>>();
      if (graph.left() instanceof EventGraph.Concurrently<T> l) {
        res.addAll(expandConcurrently(l));
      } else {
        res.add(graph.left());
      }
      if (graph.right() instanceof EventGraph.Concurrently<T> r) {
        res.addAll(expandConcurrently(r));
      } else {
        res.add(graph.right());
      }
      return res;
    }

    public static History atom(String s) {
      final var res = new History();
      res.timeline.add(new TimePoint.Commit(EventGraph.atom(s)));
      return res;
    }

    public static History atom(Duration duration) {
      final var res = new History();
      res.timeline.add(new TimePoint.Delay(duration));
      return res;
    }

    @Override
    public boolean equals(final Object object) {
      if (this == object) return true;
      if (object == null || getClass() != object.getClass()) return false;

      History history = (History) object;
      return history.toString().equals(this.toString());
    }

    @Override
    public int hashCode() {
      return timeline.hashCode();
    }

    public String toString() {
      final var res = new StringBuilder();
      var first = true;
      for (final var entry : timeline) {
        if (!first) {
          res.append(", ");
        }
        switch (entry) {
          case TimePoint.Commit e -> {
            res.append(e.graph.toString());
          }
          case TimePoint.Delay e -> {
            res.append("delay(");
            res.append(e.duration.in(SECONDS));
            res.append(")");
          }
        }
        first = false;
      }
      return res.toString();
    }
  }

  public static CellId<MutableObject<LinearDynamics>> allocateLinear(final Initializer builder, final Topic<LinearDynamicsEffect> topic) {
    return builder.allocate(
        new MutableObject<>(new LinearDynamics(0, 0)),
        new CellType<>() {
          @Override
          public EffectTrait<LinearDynamicsEffect> getEffectType() {
            return new EffectTrait<>() {
              @Override
              public LinearDynamicsEffect empty() {
                return LinearDynamicsEffect.empty();
              }

              @Override
              public LinearDynamicsEffect sequentially(
                  final LinearDynamicsEffect prefix,
                  final LinearDynamicsEffect suffix)
              {
                if (suffix.isEmpty()) {
                  return prefix;
                } else {
                  return suffix;
                }
              }

              @Override
              public LinearDynamicsEffect concurrently(
                  final LinearDynamicsEffect left,
                  final LinearDynamicsEffect right)
              {
                if (left.isEmpty()) return right;
                if (right.isEmpty()) return left;
                throw new IllegalArgumentException("Concurrent composition of non-empty linear effects: "
                                                   + left
                                                   + " | "
                                                   + right);
              }
            };
          }

          @Override
          public MutableObject<LinearDynamics> duplicate(final MutableObject<LinearDynamics> mutableObject) {
            return new MutableObject<>(mutableObject.getValue());
          }

          @Override
          public void apply(final MutableObject<LinearDynamics> mutableObject, final LinearDynamicsEffect o) {
            final LinearDynamics currentDynamics = mutableObject.getValue();
            mutableObject.setValue(new LinearDynamics(o.newRate == null ? currentDynamics.rate : o.newRate, o.newValue == null ? currentDynamics.initialValue : o.newValue));
          }

          @Override
          public void step(final MutableObject<LinearDynamics> mutableObject, final Duration duration) {
            final LinearDynamics currentDynamics = mutableObject.getValue();
            mutableObject.setValue(
                new LinearDynamics(
                    currentDynamics.rate,
                    currentDynamics.initialValue + (duration.ratioOver(SECONDS) * currentDynamics.rate)));
          }
        },
        $ -> $,
        topic);
  }

  public static CellId<MutableObject<History>> allocate(final Initializer builder, final Topic<String> topic)
  {
    return builder.allocate(
        new MutableObject<>(History.empty()),
        new CellType<>() {
          @Override
          public EffectTrait<History> getEffectType() {
            return new EffectTrait<>() {
              @Override
              public History empty() {
                return History.empty();
              }

              @Override
              public History sequentially(final History prefix, final History suffix) {
                return History.sequentially(prefix, suffix);
              }

              @Override
              public History concurrently(final History left, final History right) {
                return History.concurrently(left, right);
              }
            };
          }

          @Override
          public MutableObject<History> duplicate(final MutableObject<History> mutableObject) {
            return new MutableObject<>(mutableObject.getValue());
          }

          @Override
          public void apply(final MutableObject<History> mutableObject, final History o) {
            mutableObject.setValue(History.sequentially(mutableObject.getValue(), o));
          }

          @Override
          public void step(final MutableObject<History> mutableObject, final Duration duration) {
            mutableObject.setValue(History.sequentially(
                mutableObject.getValue(),
                History.atom(duration)));
          }
        },
        (String atom) -> {
          return History.atom(atom);
        },
        topic);
  }
}

