package gov.nasa.ammos.aerie.merlin.driver.test.property;

import gov.nasa.ammos.aerie.merlin.driver.test.framework.Cell;
import gov.nasa.ammos.aerie.merlin.driver.test.framework.TestRegistrar;
import gov.nasa.ammos.aerie.simulation.protocol.Directive;
import gov.nasa.ammos.aerie.simulation.protocol.DualSchedule;
import gov.nasa.ammos.aerie.simulation.protocol.Schedule;
import gov.nasa.ammos.aerie.simulation.protocol.Simulator;
import gov.nasa.jpl.aerie.merlin.driver.IncrementalSimAdapter;
import gov.nasa.jpl.aerie.merlin.driver.develop.MerlinDriverAdapter;
import gov.nasa.jpl.aerie.merlin.driver.retracing.RetracingDriverAdapter;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static gov.nasa.ammos.aerie.merlin.driver.test.framework.ModelActions.call;
import static gov.nasa.ammos.aerie.merlin.driver.test.framework.ModelActions.delay;
import static gov.nasa.ammos.aerie.merlin.driver.test.framework.ModelActions.spawn;
import static gov.nasa.ammos.aerie.merlin.driver.test.property.IncrementalSimPropertyTests.assertLastSegmentsEqual;
import static gov.nasa.ammos.aerie.merlin.driver.test.property.Scenario.rightmostNumber;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOUR;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Unit.UNIT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeneratedTests {
  static final Simulator.Factory INCREMENTAL_SIMULATOR = IncrementalSimAdapter::new;
  static final Simulator.Factory REGULAR_SIMULATOR = MerlinDriverAdapter::new;
  static final Simulator.Factory RETRACING_SIMULATOR = RetracingDriverAdapter::new;

  @Test
  void test7() {
    final var model = new TestRegistrar();
    Cell[] cells = new Cell[4];
    for (int i = 0; i < cells.length; i++) {
      cells[i] = model.cell();
    }
    model.activity("DT1", it -> {
       cells[0].emit(1);
       delay(ZERO);
       cells[0].get();
     });
    model.activity("DT2", it -> {
      cells[0].emit(2);
    });
    for (int i = 0; i < cells.length; i++) {
      final var cell = cells[i];
      model.resource("cell" + i, () -> cell.get().toString());
    }
    final var schedule = new DualSchedule();
    schedule.add(duration(10, SECONDS), "DT1");
    schedule.thenAdd(duration(10, SECONDS), "DT2");

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final var schedule1 = schedule.schedule1();
    final var schedule2 = schedule.schedule2();

    final var referenceSimulator = REGULAR_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);
    final var testSimulator = INCREMENTAL_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);

    {
      System.out.println("Reference simulation 1");
      final var referenceProfiles = referenceSimulator.simulate(schedule1).discreteProfiles();


      System.out.println("Test simulation 1");
      final var testProfiles = testSimulator.simulate(schedule1).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, testProfiles);
    }

    {
      System.out.println("Reference simulation 2");
      final var referenceProfiles = referenceSimulator.simulate(schedule2).discreteProfiles();


      System.out.println("Test simulation 2");
      final var testProfiles = testSimulator.simulate(schedule2).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, testProfiles);
    }
  }

  @Test
  void test6() {
    final var model = new TestRegistrar();
    Cell[] cells = new Cell[1];
    for (int i = 0; i < cells.length; i++) {
      cells[i] = model.cell();
    }
    model.activity("DT2", it -> {
      cells[0].emit("1");
      cells[0].get();
      cells[0].emit("2");
      cells[0].emit("3");
      cells[0].emit("4");
      delay(SECOND);
      cells[0].emit("5");
    } );
    for (int i = 0; i < cells.length; i++) {
      final var cell = cells[i];
      model.resource("cell" + i, () -> cell.get().toString());
    }
    final var schedule = new DualSchedule();
    schedule.add(duration(0, SECONDS), "DT2");

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final var schedule1 = schedule.schedule1();
    final var schedule2 = schedule.schedule2();

    final var referenceSimulator = REGULAR_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);
    final var simulatorUnderTest = INCREMENTAL_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);

    {
      System.out.println("Reference simulation 1");
      final var referenceProfiles = referenceSimulator.simulate(schedule1).discreteProfiles();


      System.out.println("Test simulation 1");
      final var testProfiles = simulatorUnderTest.simulate(schedule1).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, testProfiles);
    }

    {
      System.out.println("Reference simulation 2");
      final var referenceProfiles = referenceSimulator.simulate(schedule2).discreteProfiles();


      System.out.println("Test simulation 2");
      final var testProfiles = simulatorUnderTest.simulate(schedule2).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, testProfiles);
    }
  }

  @Test
  void test5() {
    final var model = new TestRegistrar();
    Cell[] cells = new Cell[2];
    for (int i = 0; i < cells.length; i++) {
      cells[i] = model.cell();
    }
    model.activity("DT1", it -> {
      // t = 0
      cells[0].emit("1");
      delay(SECOND.times(10));
      // t = 10
      cells[0].emit("2");
      delay(SECOND.times(10));
      // t = 20
      cells[0].emit("3");
      // t = 30
      delay(SECOND.times(10));
    });
    model.activity("DT2", it -> {
     if (rightmostNumber(cells[0].get().toString()) == 1) {
       cells[1].emit("foo");
     } else {
       cells[1].emit("bar");
     }
    });
    model.resource("cell0", () -> cells[0].get().toString());

    final var referenceSimulator = REGULAR_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);
    final var testSimulator = INCREMENTAL_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);

    System.out.println("Schedule 1");
    {
      var schedule = Schedule.empty();
      schedule = schedule.plus(duration(0, SECONDS), "DT1");
      schedule = schedule.plus(duration(5, SECONDS), "DT2");
      schedule = schedule.plus(duration(15, SECONDS), "DT2");

      System.out.println("Regular simulation");
      final var referenceProfiles = referenceSimulator.simulate(schedule).discreteProfiles();

      System.out.println("Test simulation");
      final var testProfiles = testSimulator.simulate(schedule).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, testProfiles);
    }

    System.out.println("Schedule 2");
    {
      var schedule = Schedule.empty();
      schedule = schedule.plus(duration(0, SECONDS), "DT1");
      schedule = schedule.plus(duration(5, SECONDS), "DT2");
      schedule = schedule.plus(duration(15, SECONDS), "DT2");
      System.out.println("Regular simulation");
      final var referenceProfiles = referenceSimulator.simulate(schedule).discreteProfiles();

      System.out.println("Test simulation");
      final var testProfiles = testSimulator.simulate(schedule).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, testProfiles);
    }
  }


  @Test
  void test3() {
    final var model = new TestRegistrar();
    Cell[] cells = new Cell[1];
    for (int i = 0; i < cells.length; i++) {
      cells[i] = model.cell();
    }
    model.activity("DT1", it -> {
      cells[0].emit("517");
      delay(SECOND);
    } );
    model.resource("cell0", () -> cells[0].get().toString());
    final var schedule = new DualSchedule();
    schedule.add(duration(0, SECONDS), "DT1").thenDelete();
    schedule.thenAdd(duration(0, SECONDS), "DT1");
    schedule.thenAdd(duration(0, SECONDS), "DT1");

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final var schedule1 = schedule.schedule1();
    final var schedule2 = schedule.schedule2();

    final var incrementalSimulator = (Simulator) INCREMENTAL_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);
    final var referenceSimulator = REGULAR_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);
    final var testSimulator = INCREMENTAL_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);

    {
      System.out.println("Reference simulation 1");
      final var referenceProfiles = referenceSimulator.simulate(schedule1).discreteProfiles();


      System.out.println("Test simulation 1");
      final var testProfiles = testSimulator.simulate(schedule1).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, testProfiles);


      System.out.println("Incremental simulation 1");
      final var incrementalProfiles = incrementalSimulator.simulate(schedule1).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, incrementalProfiles);
    }

    {
      System.out.println("Reference simulation 2");
      final var referenceProfiles = referenceSimulator.simulate(schedule2).discreteProfiles();


      System.out.println("Test simulation 2");
      final var testProfiles = testSimulator.simulate(schedule2).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, testProfiles);


      System.out.println("Incremental simulation 2");
      final var incrementalProfiles = incrementalSimulator.simulate(schedule2).discreteProfiles();
      assertLastSegmentsEqual(referenceProfiles, incrementalProfiles);
    }
  }

  @Test
  void test2() {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final var model = new TestRegistrar();
    Cell[] cells = new Cell[2];
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

    final var incrementalSimulator = (Simulator) INCREMENTAL_SIMULATOR.create(
        model.asModelType(),
        UNIT,
        Instant.EPOCH,
        HOUR);
    final var referenceSimulator = REGULAR_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);

    {
      System.out.println("Reference simulation 1");
      final var referenceProfiles = referenceSimulator.simulate(schedule1).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : referenceProfiles.entrySet()) {
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
      System.out.println("Reference simulation 2");
      final var referenceProfiles = referenceSimulator.simulate(schedule2).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : referenceProfiles.entrySet()) {
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
    final var cells = new Cell[1];

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

    final var incrementalSimulator = (Simulator) INCREMENTAL_SIMULATOR.create(
        model.asModelType(),
        UNIT,
        Instant.EPOCH,
        HOUR);
    final var referenceSimulator = REGULAR_SIMULATOR.create(model.asModelType(), UNIT, Instant.EPOCH, HOUR);

    Schedule schedule1 = Schedule.empty();
    {
      for (final var directiveType : List.of("DT1", "DT2", "DT3")) {
        schedule1 = schedule1.plus(Schedule.build(Pair.of(SECOND, new Directive(directiveType, Map.of()))));
      }
      System.out.println("Reference simulation 1");
      final var referenceProfiles = referenceSimulator.simulate(schedule1).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : referenceProfiles.entrySet()) {
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
      System.out.println("Reference simulation 2");
      final var referenceProfiles = referenceSimulator.simulate(schedule2).discreteProfiles();

      final var expected = new LinkedHashMap<String, String>();
      for (final var entry : referenceProfiles.entrySet()) {
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
