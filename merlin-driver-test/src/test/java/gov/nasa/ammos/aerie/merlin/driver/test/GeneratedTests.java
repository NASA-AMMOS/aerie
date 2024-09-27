package gov.nasa.ammos.aerie.merlin.driver.test;

import gov.nasa.ammos.aerie.simulation.protocol.Directive;
import gov.nasa.ammos.aerie.simulation.protocol.DualSchedule;
import gov.nasa.ammos.aerie.simulation.protocol.Schedule;
import gov.nasa.ammos.aerie.simulation.protocol.Simulator;
import gov.nasa.jpl.aerie.merlin.driver.IncrementalSimAdapter;
import gov.nasa.jpl.aerie.merlin.driver.develop.MerlinDriverAdapter;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static gov.nasa.ammos.aerie.merlin.driver.test.Scenario.rightmostNumber;
import static gov.nasa.ammos.aerie.merlin.driver.test.SideBySideTest.call;
import static gov.nasa.ammos.aerie.merlin.driver.test.SideBySideTest.delay;
import static gov.nasa.ammos.aerie.merlin.driver.test.SideBySideTest.spawn;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOUR;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Unit.UNIT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeneratedTests {
  @Test
  void test3() {
    final var model = new TestRegistrar();
    SideBySideTest.Cell[] cells = new SideBySideTest.Cell[10];
    for (int i = 0; i < cells.length; i++) {
      cells[i] = model.cell();
    }
    model.activity("DT1", it -> {
      cells[2].emit("26461");
      cells[2].get();
      delay(SECOND);
      call(() -> {
        cells[2].emit("26461");
        cells[2].get();
        cells[0].emit("7923");
      });
    });
    for (int i = 0; i < cells.length; i++) {
      final var cell = cells[i];
      model.resource("cell" + i, () -> cell.get().toString());
    }
    final var schedule = new DualSchedule();
    schedule.add(duration(0, SECONDS), "DT1");
    schedule.add(duration(0, SECONDS), "DT1");
    schedule.add(duration(1, SECONDS), "DT1").thenDelete();
    schedule.add(duration(3599, SECONDS), "DT1").thenDelete();
    schedule.thenAdd(duration(1, SECONDS), "DT1");

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final var schedule1 = schedule.schedule1();
    final var schedule2 = schedule.schedule2();

    final var incrementalSimulator = (Simulator) new IncrementalSimAdapter(
        model.asModelType(),
        UNIT,
        Instant.EPOCH,
        HOUR);
    final var regularSimulator = new MerlinDriverAdapter<>(model.asModelType(), UNIT, Instant.EPOCH, HOUR);

    {
      System.out.println("Regular simulation 1");
      final var regularProfiles = regularSimulator.simulate(schedule1).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : regularProfiles.entrySet()) {
        expected.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      System.out.println("Incremental simulation 1");
      final var incrementalProfiles = incrementalSimulator.simulate(schedule1).discreteProfiles();

      final var actual = new LinkedHashMap<String, String>();
      for (final var entry : incrementalProfiles.entrySet()) {
        actual.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      assertEquals(expected, actual);
    }

    {
      System.out.println("Regular simulation 2");
      final var regularProfiles = regularSimulator.simulate(schedule2).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : regularProfiles.entrySet()) {
        expected.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      System.out.println("Incremental simulation 2");
      final var incrementalProfiles = incrementalSimulator.simulate(schedule2).discreteProfiles();

      final var actual = new LinkedHashMap<String, String>();
      for (final var entry : incrementalProfiles.entrySet()) {
        actual.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      assertEquals(expected, actual);
    }


  }


  @Test
  void test2() {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final var model = new TestRegistrar();
    SideBySideTest.Cell[] cells = new SideBySideTest.Cell[2];
    for (int i = 0; i < cells.length; i++) {
      cells[i] = model.cell();
    }
    model.activity("DT1", it -> {
      cells[0].get();
      cells[0].emit("51");
    });
    for (int i = 0; i < cells.length; i++) {
      final var cell = cells[i];
      model.resource("cell" + i, () -> cell.get().toString());
    }
    Schedule schedule1 = Schedule.build(
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1415, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1112, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(2122, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1492, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(487, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(206, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1606, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3594, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(2304, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(336, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3551, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1012, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1097, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(6, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(556, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(278, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(86, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(138, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(823, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1866, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3175, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1927, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3595, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3123, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(5, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(192, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(37, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3461, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(757, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(2944, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1558, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(796, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(2663, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(892, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(135, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(53, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(16, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(438, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(24, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1717, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3536, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3598, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1552, SECONDS), new Directive("DT1", Map.of())));
    Schedule schedule2 = Schedule.build(
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1415, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1112, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(2122, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(0, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1492, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(487, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(206, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1606, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3594, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(2304, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(336, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3551, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(2945, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(758, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3462, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(38, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(193, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(6, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3124, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3596, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1928, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(3176, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1867, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(824, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(139, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(87, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(279, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(557, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(7, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1098, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(2, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1013, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())),
        Pair.of(duration(1, SECONDS), new Directive("DT1", Map.of())));

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final var incrementalSimulator = (Simulator) new IncrementalSimAdapter(
        model.asModelType(),
        UNIT,
        Instant.EPOCH,
        HOUR);
    final var regularSimulator = new MerlinDriverAdapter<>(model.asModelType(), UNIT, Instant.EPOCH, HOUR);

    {
      System.out.println("Regular simulation 1");
      final var regularProfiles = regularSimulator.simulate(schedule1).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : regularProfiles.entrySet()) {
        expected.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      System.out.println("Incremental simulation 1");
      final var incrementalProfiles = incrementalSimulator.simulate(schedule1).discreteProfiles();

      final var actual = new LinkedHashMap<String, String>();
      for (final var entry : incrementalProfiles.entrySet()) {
        actual.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      assertEquals(expected, actual);
    }

    {
      System.out.println("Regular simulation 2");
      final var regularProfiles = regularSimulator.simulate(schedule2).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : regularProfiles.entrySet()) {
        expected.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      System.out.println("Incremental simulation 2");
      final var incrementalProfiles = incrementalSimulator.simulate(schedule2).discreteProfiles();

      final var actual = new LinkedHashMap<String, String>();
      for (final var entry : incrementalProfiles.entrySet()) {
        actual.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      assertEquals(expected, actual);
    }
  }


  @Test
  void test1() {
    final var model = new TestRegistrar();
    final var cells = new SideBySideTest.Cell[1];

    for (int i = 0; i < cells.length; i++) {
      cells[i] = model.cell();
    }

    for (int i = 0; i < cells.length; i++) {
      final var cell = cells[i];
      model.resource("cell" + i, () -> cell.get().toString());
    }

//    model.activity("entrypoint", $ -> {
//
//    });
    model.activity("DT1", it -> {
      call(() -> {
      });
    });
    model.activity("DT2", it -> {
      if (rightmostNumber(cells[0].get().toString()) < 0) {
        cells[0].emit("0");
      }
    });
    model.activity("DT3", it -> {
      cells[0].emit("1");
    });

    final var incrementalSimulator = (Simulator) new IncrementalSimAdapter(
        model.asModelType(),
        UNIT,
        Instant.EPOCH,
        HOUR);
    final var regularSimulator = new MerlinDriverAdapter<>(model.asModelType(), UNIT, Instant.EPOCH, HOUR);

    Schedule schedule1 = Schedule.empty();
    {
      for (final var directiveType : List.of("DT1", "DT2", "DT3")) {
        schedule1 = schedule1.plus(Schedule.build(Pair.of(SECOND, new Directive(directiveType, Map.of()))));
      }
      System.out.println("Regular simulation 1");
      final var regularProfiles = regularSimulator.simulate(schedule1).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : regularProfiles.entrySet()) {
        expected.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      System.out.println("Incremental simulation 1");
      final var incrementalProfiles = incrementalSimulator.simulate(schedule1).discreteProfiles();

      final var actual = new LinkedHashMap<String, String>();
      for (final var entry : incrementalProfiles.entrySet()) {
        actual.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      assertEquals(expected, actual);
    }

    {
      Schedule schedule2 = schedule1;
      for (final var entry : schedule1.entries()) {
        schedule2 = schedule2.setStartTime(entry.id(), entry.startTime().plus(SECOND));
      }
      System.out.println("Regular simulation 2");
      final var regularProfiles = regularSimulator.simulate(schedule2).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : regularProfiles.entrySet()) {
        expected.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      System.out.println("Incremental simulation 2");
      final var incrementalProfiles = incrementalSimulator.simulate(schedule2).discreteProfiles();

      final var actual = new LinkedHashMap<String, String>();
      for (final var entry : incrementalProfiles.entrySet()) {
        actual.put(entry.getKey(), entry.getValue().segments().getLast().dynamics().asString().get());
      }

      assertEquals(expected, actual);
    }
  }
}
