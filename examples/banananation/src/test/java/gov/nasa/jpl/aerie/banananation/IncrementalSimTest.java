package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.IntStream;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IncrementalSimTest {
  private static boolean debug = false;
  @Test
  public void testRemoveAndAddActivity() {
    if (debug) System.out.println("testRemoveAndAddActivity()");
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

    // Add PeelBanana at time = 5
    var simulationResults = driver.simulate(schedule1, startTime, simDuration, startTime, simDuration);
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruitProfile = " + fruitProfile);

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    assertEquals(2, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(Duration.of(5, SECONDS), fruitProfile.get(0).extent());
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);

    // Remove PeelBanana (back to empty schedule)
    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(new HashMap<>(), startTime, simDuration, startTime, simDuration);
    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruitProfile = " + fruitProfile);

    assertEquals(0, simulationResults.getSimulatedActivities().size());
    assertEquals(1, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);

    // Add PeelBanana at time = 3
    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(schedule2, startTime, simDuration, startTime, simDuration);
    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruitProfile = " + fruitProfile);

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    assertEquals(2, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(Duration.of(3, SECONDS), fruitProfile.get(0).extent());
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);
  }

  @Test
  public void testRemoveActivity() {
    if (debug) System.out.println("testRemoveActivity()");

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
    assertEquals(4.0, fruitProfile.get(fruitProfile.size()-1).dynamics().initial);
  }

  @Test
  public void testMoveActivityLater() {
    if (debug) System.out.println("testMoveActivityLater()");

    final var schedule1 = SimulationUtility.buildSchedule(
        Pair.of(
            duration(3, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );
    final var schedule2 = SimulationUtility.buildSchedule(
        Pair.of(
            duration(5, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    var simulationResults = driver.simulate(schedule1, startTime, simDuration, startTime, simDuration);
    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(schedule2, startTime, simDuration, startTime, simDuration);

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    assertEquals(3.0, fruitProfile.get(fruitProfile.size()-1).dynamics().initial);
  }

  @Test
  public void testMoveActivityPastAnother() {
    if (debug) System.out.println("testMoveActivityLater()");

    final var schedule = SimulationUtility.buildSchedule(
        Pair.of(
            duration(3, SECONDS),
            new SerializedActivity("PeelBanana", Map.of())),
        Pair.of(
            duration(5, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    var simulationResults = driver.simulate(schedule, startTime, simDuration, startTime, simDuration);

    final Map.Entry<ActivityDirectiveId, ActivityDirective> firstEntry = schedule.entrySet().iterator().next();
    final ActivityDirective directive1 = firstEntry.getValue();
    final ActivityDirectiveId key1 = firstEntry.getKey();
    assertEquals(Duration.of(3, SECONDS), directive1.startOffset());
    schedule.put(key1, new ActivityDirective(Duration.of(7, SECONDS), directive1.serializedActivity(), directive1.anchorId(), directive1.anchoredToStart()));

    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(schedule, startTime, simDuration, startTime, simDuration);

    assertEquals(2, simulationResults.getSimulatedActivities().size());
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    assertEquals(3, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);
    assertEquals(2.0, fruitProfile.get(2).dynamics().initial);
  }

  @Test
  public void testZeroDurationEventAtStart() {
    if (debug) System.out.println("testZeroDurationEventAtStart()");

    final var schedule1 = SimulationUtility.buildSchedule(
        Pair.of(
            duration(0, SECONDS),
            new SerializedActivity("PeelBanana", Map.of())),
        Pair.of(
            duration(5, SECONDS),
            new SerializedActivity("GrowBanana", Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", SerializedValue.of(Duration.SECOND.times(2).in(Duration.MICROSECONDS)))))
    );

    final var schedule2 = SimulationUtility.buildSchedule(
        Pair.of(
            duration(8, SECONDS),
            new SerializedActivity("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    var simulationResults = driver.simulate(schedule1, startTime, simDuration, startTime, simDuration);

    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruit profile = " + fruitProfile);

    driver.initSimulation(simDuration);
    simulationResults = driver.simulate(schedule2, startTime, simDuration, startTime, simDuration);

    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruit profile = " + fruitProfile);

    assertEquals(3, simulationResults.getSimulatedActivities().size());
    assertEquals(4, fruitProfile.size());
    assertEquals(3.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);
    assertEquals(4.0, fruitProfile.get(2).dynamics().initial);
    assertEquals(3.0, fruitProfile.get(3).dynamics().initial);
  }

  final static String INIT_SIM = "Initial Simulation";
  final static String COMP_RESULTS = "Compute Results";
  final static String SERIALIZE_RESULTS = "Serialize Results";
  final static String INC_SIM = "Incremental Simulation";
  final static String COMP_INC_RESULTS = "Compute Incremental Results";
  final static String SERIALIZE_INC_RESULTS = "Serialize Combined Results";

  final static String[] labels = new String[] { INIT_SIM, COMP_RESULTS, SERIALIZE_RESULTS,
                                                INC_SIM, COMP_INC_RESULTS, SERIALIZE_INC_RESULTS };

  final static String[] incSimLabels = new String[] { INC_SIM, COMP_INC_RESULTS, SERIALIZE_INC_RESULTS };


  @Test
  public void testPerformanceOfOneEditToScaledPlan() {
    if (debug) System.out.println("testPerformanceOfOneEditToScaledPlan()");

    int scaleFactor = 1000;

    final List<Integer> sizes = IntStream.rangeClosed(1, 20).boxed().map(i -> i * scaleFactor).toList();
    System.out.println("Numbers of activities to test: " + sizes);

    long spread = 5;
    Duration unit = SECONDS;

    final SerializedActivity biteBanana = new SerializedActivity("BiteBanana", Map.of());

    final SerializedActivity peelBanana = new SerializedActivity("PeelBanana", Map.of());

    final SerializedActivity changeProducerChiquita = new SerializedActivity("ChangeProducer", Map.of("producer", SerializedValue.of("Chiquita")));

    final SerializedActivity changeProducerDole = new SerializedActivity("ChangeProducer", Map.of("producer", SerializedValue.of("Dole")));

    //HashMap<String, HashMap<String, List<Double>>> stats = new HashMap<>();

    var testTimer = new Timer("testPerformanceOfOneEditToScaledPlan", false);

    // test each case
    for (int numActs : sizes) {

      var scaleTimer = new Timer("test " + numActs, false);

      // generate numActs activities
      Pair<Duration, SerializedActivity>[] pairs = new Pair[numActs];
      for (int i = 0; i < numActs; ++i) {
        pairs[i] = Pair.of(duration(spread * (i + 1), unit),
                           changeProducerChiquita);
        ++i;
        pairs[i] = Pair.of(duration(spread * (i + 1), unit),
                           changeProducerDole);
      }
      final Map<ActivityDirectiveId, ActivityDirective> schedule = SimulationUtility.buildSchedule(pairs);

      final var startTime = Instant.now();
      final var simDuration = duration(spread * (numActs + 2), SECOND);

      var timer = new Timer(INIT_SIM + " " + numActs, false);
      final var driver = SimulationUtility.getDriver(simDuration);
      driver.simulate(schedule, startTime, simDuration, startTime, simDuration, false);
      timer.stop(false);

      timer = new Timer(COMP_RESULTS + " " + numActs, false);
      var simulationResults = driver.computeResults(startTime, simDuration);
      timer.stop(false);
      timer = new Timer(SERIALIZE_RESULTS + " " + numActs, false);
      String results = simulationResults.toString();
      timer.stop(false);

      // Modify a directive in the schedule
      final Optional<ActivityDirectiveId> d0 = schedule.keySet().stream().findFirst();
      long middleDirectiveNum = d0.get().id() + schedule.size() / 2;
      ActivityDirectiveId directiveId = new ActivityDirectiveId(middleDirectiveNum);  // get middle activity
      final ActivityDirective directive = schedule.get(directiveId);
      schedule.put(directiveId, new ActivityDirective(directive.startOffset().plus(1, unit),
                                                      directive.serializedActivity(), directive.anchorId(),
                                                      directive.anchoredToStart()));

      timer = new Timer(INC_SIM + " " + numActs, false);
      driver.initSimulation(simDuration);
      simulationResults = driver.diffAndSimulate(schedule, startTime, simDuration, startTime, simDuration);
      timer.stop(false);

      timer = new Timer(COMP_INC_RESULTS + " " + numActs, false);
      simulationResults = driver.computeResults(startTime, simDuration);
      timer.stop(false);
      timer = new Timer(SERIALIZE_INC_RESULTS + " " + numActs, false);
      results = simulationResults.toString();  // The results are not combined until they forced to be
      timer.stop(false);

      scaleTimer.stop(false);
    }

    testTimer.stop(false);

    //Timer.logStats();
    // Write out stats
    final ConcurrentSkipListMap<String, ConcurrentSkipListMap<Timer.StatType, Long>>
        mm = Timer.getStats();
    ArrayList<String> header = new ArrayList<>();
    header.add("Number of Activities");
    for (int i = 0; i < labels.length; ++i) {
      header.add(labels[i] + " (duration)");
      header.add(labels[i] + " (cpu time)");
    }
    System.out.println(String.join(", ", header));
    for (int numActs : sizes) {
      ArrayList<String> row = new ArrayList<>();
      row.add("" + numActs);
      for (int i = 0; i < labels.length; ++i) {
        ConcurrentSkipListMap<Timer.StatType, Long> statMap = mm.get(labels[i] + " " + numActs);
        row.add("" + Timer.formatDuration(statMap.get(Timer.StatType.wallClockTime)));
        row.add("" + Timer.formatDuration(statMap.get(Timer.StatType.cpuTime)));
      }
      System.out.println(String.join(", ", row));
    }
  }


  @Test
  public void testPerformanceOfRepeatedSimsToScaledPlan() {
    if (debug) System.out.println("testPerformanceOfRepeatedSimsToScaledPlan()");

    int scaleFactor = 10;
    int numEdits = 50;

    final List<Integer> sizes = IntStream.rangeClosed(1, 5).boxed().map(i -> i * scaleFactor).toList();
    System.out.println("Numbers of activities to test: " + sizes);

    long spread = 5;
    Duration unit = SECONDS;

    final SerializedActivity biteBanana = new SerializedActivity("BiteBanana", Map.of());
    final SerializedActivity peelBanana = new SerializedActivity("PeelBanana", Map.of());
    final SerializedActivity changeProducerChiquita = new SerializedActivity("ChangeProducer", Map.of("producer", SerializedValue.of("Chiquita")));
    final SerializedActivity changeProducerDole = new SerializedActivity("ChangeProducer", Map.of("producer", SerializedValue.of("Dole")));
    final SerializedActivity[] serializedActivities = new SerializedActivity[] {changeProducerChiquita, changeProducerDole, peelBanana, biteBanana};


    var testTimer = new Timer("testPerformanceOfOneEditToScaledPlan", false);

    // test each case
    for (int numActs : sizes) {

      var scaleTimer = new Timer("test " + numActs, false);

      // generate numActs activities
      Pair<Duration, SerializedActivity>[] pairs = new Pair[numActs];
      for (int i = 0; i < numActs; ++i) {
        pairs[i] = Pair.of(duration(spread * (i + 1), unit),
                           serializedActivities[i % serializedActivities.length]);
      }
      final Map<ActivityDirectiveId, ActivityDirective> schedule = SimulationUtility.buildSchedule(pairs);
      long initialId = schedule.keySet().stream().findFirst().get().id();

      final var startTime = Instant.now();
      final var simDuration = duration(spread * (numActs + 2), SECOND);

      var timer = new Timer(INIT_SIM + " " + numActs, false);
      final var driver = SimulationUtility.getDriver(simDuration);
      driver.simulate(schedule, startTime, simDuration, startTime, simDuration, false);
      timer.stop(false);

      timer = new Timer(COMP_RESULTS + " " + numActs, false);
      var simulationResults = driver.computeResults(startTime, simDuration);
      timer.stop(false);
      timer = new Timer(SERIALIZE_RESULTS + " " + numActs, false);
      String results = simulationResults.toString();
      timer.stop(false);

      var random = new Random(3);

      for (int j=0; j < numEdits; ++j) {

        // Modify a directive in the schedule
        long directiveNumber = initialId + random.nextInt(numActs);
        ActivityDirectiveId directiveId = new ActivityDirectiveId(directiveNumber);  // get random activity
        final ActivityDirective directive = schedule.get(directiveId);
        Duration newOffset = directive.startOffset().plus(1, unit);
        if (newOffset.noShorterThan(simDuration)) newOffset = simDuration.minus(1, unit);
        schedule.put(directiveId, new ActivityDirective(newOffset,
                                                        directive.serializedActivity(), directive.anchorId(),
                                                        directive.anchoredToStart()));

        timer = new Timer(INC_SIM + " " + numActs + " " + j, false);
        driver.initSimulation(simDuration);
        simulationResults = driver.diffAndSimulate(schedule, startTime, simDuration, startTime, simDuration);
        timer.stop(false);

        timer = new Timer(COMP_INC_RESULTS + " " + numActs + " " + j, false);
        simulationResults = driver.computeResults(startTime, simDuration);
        timer.stop(false);
        timer = new Timer(SERIALIZE_INC_RESULTS + " " + numActs + " " + j, false);
        results = simulationResults.toString();  // The results are not combined until they forced to be
        timer.stop(false);
      }
      scaleTimer.stop(false);
    }

    testTimer.stop(false);

    //Timer.logStats();
    // Write out stats
    final ConcurrentSkipListMap<String, ConcurrentSkipListMap<Timer.StatType, Long>>
        mm = Timer.getStats();
    ArrayList<String> header = new ArrayList<>();
    header.add("Number of Activities");
    header.add("Number of Incremental Simulations");
    for (int i = 0; i < incSimLabels.length; ++i) {
      header.add(incSimLabels[i] + " (duration)");
      header.add(incSimLabels[i] + " (cpu time)");
    }
    System.out.println(String.join(", ", header));
    for (int numActs : sizes) {
      for (int j = 0; j < numEdits; ++j) {
        ArrayList<String> row = new ArrayList<>();
        row.add("" + numActs);
        row.add("" + j);
        for (int i = 0; i < incSimLabels.length; ++i) {
          ConcurrentSkipListMap<Timer.StatType, Long> statMap = mm.get(incSimLabels[i] + " " + numActs + " " + j);
          row.add("" + Timer.formatDuration(statMap.get(Timer.StatType.wallClockTime)));
          row.add("" + Timer.formatDuration(statMap.get(Timer.StatType.cpuTime)));
        }
        System.out.println(String.join(", ", row));
      }
    }
  }
}
