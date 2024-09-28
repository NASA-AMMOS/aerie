package gov.nasa.ammos.aerie.merlin.driver.test;

import com.squareup.javapoet.CodeBlock;
import gov.nasa.ammos.aerie.simulation.protocol.Directive;
import gov.nasa.ammos.aerie.simulation.protocol.DualSchedule;
import gov.nasa.ammos.aerie.simulation.protocol.Schedule;
import gov.nasa.ammos.aerie.simulation.protocol.Simulator;
import gov.nasa.jpl.aerie.merlin.driver.IncrementalSimAdapter;
import gov.nasa.jpl.aerie.merlin.driver.develop.MerlinDriverAdapter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static gov.nasa.ammos.aerie.merlin.driver.test.Scenario.directiveType;
import static gov.nasa.ammos.aerie.merlin.driver.test.Scenario.effectModels;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IncrementalSimPropertyTests {
  private static final Simulator.Factory REGULAR_SIM_FACTORY = MerlinDriverAdapter::new;
  private static final Simulator.Factory INCREMENTAL_SIM_FACTORY = IncrementalSimAdapter::new;

  @Property
  @Label("Incremental re-simulation should be consistent with regular simulation")
  public void incrementalSimulationMatchesRegularSimulation(@ForAll("scenarios") Scenario scenario) {
    final var incrementalSimulator = INCREMENTAL_SIM_FACTORY.create(
        scenario.model().asModelType(),
        Unit.UNIT,
        scenario.startTime(),
        scenario.duration());
    final var regularSimulator = REGULAR_SIM_FACTORY.create(
        scenario.model().asModelType(),
        Unit.UNIT,
        scenario.startTime(),
        scenario.duration());

    regularSimulator.simulate(scenario.schedule().schedule1());
    incrementalSimulator.simulate(scenario.schedule().schedule1());

    final var regularProfiles = regularSimulator.simulate(scenario.schedule().schedule2()).discreteProfiles();

    final var expected = new LinkedHashMap<String, String>();
    for (final var entry : regularProfiles.entrySet()) {
      expected.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
    }

    MutableBoolean cancelSim = new MutableBoolean(false);

    final var incrementalProfiles = incrementalSimulator.simulate(scenario.schedule().schedule2(), cancelSim::getValue).getDiscreteProfiles();

    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        cancelSim.setTrue();
      }
    }, 30 * 1000);

    final var actual = new LinkedHashMap<String, String>();
    for (final var entry : incrementalProfiles.entrySet()) {
      actual.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
    }

    if (!expected.equals(actual)) {
      scenario.resetTraces();
      regularSimulator.simulate(scenario.schedule().schedule2());
      scenario.shrinkToTraces();
      assertEquals(expected, actual);
    }
  }

  @Provide("scenarios")
  static Arbitrary<Scenario> scenarios() {
    return scenario(Arbitraries.integers());
  }

  static Arbitrary<DualSchedule> schedules(int numDirectiveTypes) {
    return Arbitraries.integers().flatMap(size -> Arbitraries
        .integers()
        .list()
        .ofSize(Math.floorMod(size, 100))
        .map($ -> $.stream().map(it -> duration(Math.floorMod(it, 3600), SECONDS)).toList())).map(startOffsets -> {
      // For each activity type
      DualSchedule schedule = new DualSchedule();

//      Schedule schedule1 = Schedule.empty();

      for (int i = 0; i < startOffsets.size(); i++) {
        final var startOffset = startOffsets.get(i);
        final var name = "DT" + ((i % numDirectiveTypes) + 1);
        schedule.add(startOffset, name);
//        schedule1 = schedule1.plus(Schedule.build(Pair.of(startOffset, new Directive(name, Map.of()))));
      }

      // Generate random edits to that schedule
//      Schedule schedule2 = schedule1;

      // Deletes
      int numDeletes = schedule.schedule1().size() / 4;
      for (int i = 0; i < numDeletes; i++) {
//        schedule2 = schedule2.delete(schedule2.entries().getLast().id());
        schedule.thenDelete(schedule.schedule2().entries().getLast().id());
      }
      // Select number of deletes (must be less than or equal to the number of activities in the schedule)
      // Select which activities to delete

      // Updates
      // Select number of updates (must be less than or equal to the number of activities in the schedule)
      // Select which activities to update
      // Select time delta
      // TODO change parameters

      int numUpdates = schedule.schedule2().entries().size() / 2;
      for (int i = 0; i < numUpdates; i++) {
        final var entry = schedule.schedule2().entries().get(schedule.schedule2().entries().size() - i - 1);
//        schedule2 = schedule2.setStartTime(entry.id(), entry.startTime().plus(SECOND));
      }

      // Additions
      // Select number of additions
      // For each addition, select type
      int numAdditions = schedule.schedule1().entries().size() / 5;
      for (int i = 0; i < numAdditions; i++) {
//        schedule2 = schedule2.plus(Schedule.build(Pair.of(SECOND, new Directive("DT1", Map.of()))));
        schedule.thenAdd(SECOND, "DT1");
      }

//      schedule1.entries().sort(Comparator.comparing(Schedule.ScheduleEntry::startOffset));
//      schedule2.entries().sort(Comparator.comparing(Schedule.ScheduleEntry::startOffset));
      return schedule;
    });
  }

  static Arbitrary<Scenario> scenario(Arbitrary<Integer> integers) {
    return Arbitraries
        .lazyOf(() -> integers.tuple3().flatMap(ints -> {
          final var numCells = 1 + Math.floorMod(ints.get1(), 10);
          final var numDirectiveTypes = 1 + Math.floorMod(ints.get2(), 4);

          return
              directiveTypes(numDirectiveTypes, integers).flatMap(directiveTypes -> schedules(numDirectiveTypes).map(schedules -> {
                final var model = new TestRegistrar();
                SideBySideTest.Cell[] cells = new SideBySideTest.Cell[numCells];
                for (int i = 0; i < cells.length; i++) {
                  cells[i] = model.cell();
                }

                Map<String, Trace.Owner> tracers = new LinkedHashMap<>();
                for (final var directiveType : directiveTypes.directiveTypes()) {
                  tracers.put(directiveType.name(), new Trace.TraceImpl());
                  model.activity(directiveType.name(), $ -> {
                    Scenario.interpret(
                        directiveType.effectModel(),
                        cells,
                        tracers.get(directiveType.name()));
                  });
                }

                for (int i = 0; i < cells.length; i++) {
                  final var cell = cells[i];
                  model.resource("cell" + i, () -> cell.get().toString());
                }

                // Generate a random schedule
                // TODO compute dependencies
                return new Scenario(
                    cells,
                    directiveTypes.directiveTypes(),
                    tracers,
                    model,
                    Instant.EPOCH,
                    Duration.HOUR,
                    schedules);
              }));
        }));
  }

  static Arbitrary<Scenario.DirectiveTypes> directiveTypes(int numDirectiveTypes, Arbitrary<Integer> integers) {
    return directiveType(integers).list().ofSize(numDirectiveTypes).map($ -> {
      final List<Scenario.DirectiveType> res = new ArrayList<>();
      for (int i = 0; i < $.size(); i++) {
        final var dt = $.get(i);
        res.add(new Scenario.DirectiveType("DT" + (i + 1), dt.parameters(), dt.effectModel()));
      }
      return new Scenario.DirectiveTypes(res);
    });
  }

  @Provide("effectModel")
  static Arbitrary<Scenario.EffectModel> effectModel() {
    return effectModels(Arbitraries.integers());
  }

  static CodeBlock printEffectModel(Scenario.EffectModel effectModel, int numCells) {
    final var builder = CodeBlock.builder();
    for (final var step : effectModel.steps()) {
      switch (step) {
        case Scenario.Step.CallDirective s -> {
          builder.addStatement("callDirective()");
        }
        case Scenario.Step.CallTask s -> {
          builder.beginControlFlow("call(() ->");
          builder.add(printEffectModel(s.task(), numCells));
          builder.endControlFlow(")");
        }
        case Scenario.Step.Delay s -> {
          builder.addStatement("delay(SECOND)");
        }
        case Scenario.Step.Emit s -> {
          builder.addStatement("$L.emit($S)", "cells[" + Math.floorMod(s.topic(), numCells) + "]", s.value());
        }
        case Scenario.Step.Read s -> {
          if (s.branch().left().steps().isEmpty() && s.branch().right().steps().isEmpty()) {
            builder.addStatement("$L.get()", "cells[" + Math.floorMod(s.topic(), numCells) + "]");
          } else if (!s.branch().left().steps().isEmpty()) {
            builder.beginControlFlow(
                "if (rightmostNumber($L.get().toString()) < $L)",
                "cells[" + Math.floorMod(s.topic(), numCells) + "]",
                s.branch().threshold());
            builder.add(printEffectModel(s.branch().left(), numCells));

            if (s.branch().right().steps().isEmpty()) {
              builder.endControlFlow();
            } else {
              builder.nextControlFlow("else");
              builder.add(printEffectModel(s.branch().right(), numCells));
              builder.endControlFlow();
            }
          } else {
            builder.beginControlFlow(
                "if (rightmostNumber($L.get().toString()) >= $L)",
                "cells[" + Math.floorMod(s.topic(), numCells) + "]",
                s.branch().threshold());
            builder.add(printEffectModel(s.branch().right(), numCells));
            builder.endControlFlow();
          }
        }
        case Scenario.Step.SpawnDirective s -> {
          builder.addStatement("spawnDirective()");
        }
        case Scenario.Step.SpawnTask s -> {
          builder.beginControlFlow("spawn(() ->");
          builder.add(printEffectModel(s.task(), numCells));
          builder.endControlFlow(")");
        }
        case Scenario.Step.WaitUntil s -> {
          builder.addStatement("waitUntil()");
        }
      }
    }
    return builder.build();
  }
}
