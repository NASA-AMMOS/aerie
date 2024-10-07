package gov.nasa.ammos.aerie.merlin.driver.test;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.model.OutputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TestRegistrar {
  List<Pair<String, Consumer<String>>> activities = new ArrayList<>();
  List<SideBySideTest.Cell> cells = new ArrayList<>();
  List<Runnable> daemons = new ArrayList<>();
  List<Pair<String, Supplier<?>>> resources = new ArrayList<>();

  public <T> SideBySideTest.Cell cell() {
    final SideBySideTest.Cell cell = SideBySideTest.Cell.of();
    cells.add(cell);
    return cell;
  }

  public <T> SideBySideTest.Cell linearCell() {
    final SideBySideTest.Cell cell = SideBySideTest.Cell.ofLinear();
    cells.add(cell);
    return cell;
  }

//  public <T> SideBySideTest.Cell<T, T> cell(T initialValue) {
//    return this.cell(initialValue, List::getLast);
//  }

//  public <T> SideBySideTest.Cell<T, T> cell(T initialValue, Function<List<T>, T> apply) {
//    final var cell = SideBySideTest.Cell.of(initialValue, apply);
//    cells.add(cell);
//    return cell;
//  }

  public void activity(String name, Consumer<String> effectModel) {
    this.activities.add(Pair.of(name, effectModel));
  }

  public void daemon(Runnable runnable) {
    this.daemons.add(runnable);
  }

  public <T> void resource(String name, Supplier<T> supplier) {
    this.resources.add(Pair.of(name, supplier));
  }

  public ModelType<Unit, TestContext.CellMap> asModelType() {
    final var directives = new HashMap<String, DirectiveType<TestContext.CellMap, Map<String, SerializedValue>, Unit>>();
    final var inputTopics = new HashMap<String, Topic<String>>();
    final var outputTopics = new HashMap<String, Topic<Unit>>();

    for (final var activity : activities) {
      final Topic<String> inputTopic = new Topic<>();
      final Topic<Unit> outputTopic = new Topic<>();
      inputTopics.put(activity.getLeft(), inputTopic);
      outputTopics.put(activity.getLeft(), outputTopic);
      directives.put(activity.getKey(), new DirectiveType<>() {

        @Override
        public InputType<Map<String, SerializedValue>> getInputType() {
          return Stubs.PASS_THROUGH_INPUT_TYPE;
        }

        @Override
        public OutputType<Unit> getOutputType() {
          return Stubs.UNIT_OUTPUT_TYPE;
        }

        @Override
        public TaskFactory<Unit> getTaskFactory(final TestContext.CellMap cellMap, final Map<String, SerializedValue> args) {
          return executor -> ThreadedTask.of(executor, cellMap, () -> {
            final SerializedValue value = args.get("value");
            final String input = value == null ? "" : value.asString().get();
            TestContext.get().scheduler().startActivity(input, inputTopic);
            activity.getValue().accept(input);
            TestContext.get().scheduler().endActivity(Unit.UNIT, outputTopic);
            return Unit.UNIT;
          });
        }
      });
    }


    return new ModelType<>() {
      @Override
      public Map<String, ? extends DirectiveType<TestContext.CellMap, ?, ?>> getDirectiveTypes() {
        return directives;
      }

      @Override
      public InputType<Unit> getConfigurationType() {
        return Stubs.UNIT_INPUT_TYPE;
      }

      @Override
      public TestContext.CellMap instantiate(
          final Instant planStart,
          final Unit configuration,
          final Initializer builder)
      {
        for (final var directive : directives.entrySet()) {
          builder.topic(
              "ActivityType.Input." + directive.getKey(),
              inputTopics.get(directive.getKey()),
              Stubs.STRING_OUTPUT_TYPE);
          builder.topic(
              "ActivityType.Output." + directive.getKey(),
              outputTopics.get(directive.getKey()),
              Stubs.UNIT_OUTPUT_TYPE);
        }
        final var cellMap = new TestContext.CellMap();
        for (final var cell : cells) {
          if (cell.isLinear()) {
            cellMap.put(cell, SideBySideTest.allocateLinear(builder, cell.linearTopic()));
          } else {
            cellMap.put(cell, SideBySideTest.allocate(builder, cell.topic()));
          }
        }
        for (final var daemon : daemons) {
          builder.daemon(executor -> ThreadedTask.of(executor, cellMap, () -> {daemon.run(); return Unit.UNIT;}));
        }
        for (final var resource : resources) {
          builder.resource(resource.getLeft(), new Resource<Object>() {
            @Override
            public String getType() {
              return "discrete";
            }

            @Override
            public OutputType<Object> getOutputType() {
              return new OutputType<>() {
                @Override
                public ValueSchema getSchema() {
                  return ValueSchema.ofStruct(Map.of());
                }

                @Override
                public SerializedValue serialize(final Object value) {
                  return SerializedValue.of(value.toString());
                }
              };
            }

            @Override
            public Object getDynamics(final Querier querier) {
              TestContext.set(new TestContext.Context(cellMap, schedulerOfQuerier(querier), null));
              try {
                return resource.getRight().get();
              } finally {
                TestContext.clear();
              }
            }
          });
        }
        return cellMap;
      }
    };
  }

  public static Scheduler schedulerOfQuerier(Querier querier) {
    return new Scheduler() {
      @Override
      public <State> State get(final CellId<State> cellId) {
        return querier.getState(cellId);
      }

      @Override
      public <Event> void emit(final Event event, final Topic<Event> topic) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void spawn(final InSpan taskSpan, final TaskFactory<?> task) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <T> void startActivity(final T activity, final Topic<T> inputTopic) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <T> void endActivity(final T result, final Topic<T> outputTopic) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <ActivityDirectiveId> void startDirective(
          final ActivityDirectiveId activityDirectiveId,
          final Topic<ActivityDirectiveId> activityTopic)
      {
        throw new UnsupportedOperationException();
      }
    };
  }
}
