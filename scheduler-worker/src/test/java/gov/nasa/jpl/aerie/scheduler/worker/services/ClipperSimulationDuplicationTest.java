package gov.nasa.jpl.aerie.scheduler.worker.services;

import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.framework.ThreadedTask;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.scheduler.server.services.MerlinServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGInterval;

import javax.json.Json;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTES;
import static gov.nasa.jpl.aerie.scheduler.worker.services.ClipperSimulationComparisonUtils.assertEqualsSimResultsClipper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClipperSimulationDuplicationTest {
  final static Duration YEAR = Duration.HOUR.times(24 * 7 * 52);
  final static Duration WEEK = Duration.HOUR.times(24 * 7);
  final static Duration DURATIONSIM = YEAR.dividedBy(2);

  @BeforeAll
  static void beforeAll() {
    ThreadedTask.CACHE_READS = true;
  }

  @Test
  void testDuplicate() throws MissionModelLoader.MissionModelLoadException, IOException,
                              InstantiationException, MerlinServiceException, NoSuchPlanException, URISyntaxException
  {
    final var jsonPlan = loadClipperModelAndPlan();

    final var oneYearSimulationWithCheckpoints = SimulationDriver.simulateWithCheckpoints(
        jsonPlan.missionModel,
        jsonPlan.plan(),
        jsonPlan.planningHorizon.start,
        DURATIONSIM,
        jsonPlan.planningHorizon.start,
        DURATIONSIM,
        $ -> {},
        SimulationDriver.CachedSimulationEngine.empty(jsonPlan.missionModel()),
        SimulationDriver.periodicCheckpoints(WEEK, DURATIONSIM, WEEK));

    for(final var checkpoint : oneYearSimulationWithCheckpoints.checkpoints()){
      System.out.println("Starting simulation from checkpoint at " + checkpoint.startOffset());
      final SimulationDriver.SimulationResultsWithCheckpoints newResults = SimulationDriver.simulateWithCheckpoints(
          jsonPlan.missionModel,
          jsonPlan.plan(),
          jsonPlan.planningHorizon.start,
          DURATIONSIM,
          jsonPlan.planningHorizon.start,
          DURATIONSIM,
          $ -> {},
          checkpoint,
          SimulationDriver.desiredCheckpoints(List.of()));
      System.out.println("Comparing results for checkpoint at " + checkpoint.startOffset());
      assertEqualsSimResultsClipper(oneYearSimulationWithCheckpoints.results(), newResults.results(), jsonPlan.plan());
    }
  }

  @Test
  void testNondeterministricSim()
  throws MissionModelLoader.MissionModelLoadException, IOException, InstantiationException, MerlinServiceException,
         NoSuchPlanException, URISyntaxException
  {
    final var nbIterations = 100;
    final var YEAR = Duration.HOUR.times(24 * 7 * 52);
    final var durationSim = YEAR.dividedBy(12);
    final var jsonPlan = loadClipperModelAndPlan();
    SimulationResults first = null;
    for(var i = 0; i < nbIterations; i++){
      final SimulationResults itResults = SimulationDriver.simulate(
          jsonPlan.missionModel,
          jsonPlan.plan,
          jsonPlan.planningHorizon.start,
          durationSim,
          jsonPlan.planningHorizon.start,
          durationSim,
          $ -> {});
      if(first == null){
        first = itResults;
        continue;
      }
      assertEqualsSimResultsClipper(first, itResults, jsonPlan.plan);
    }
  }

  @Test
  void testBestCachedEngine1()
  throws MissionModelLoader.MissionModelLoadException, FileNotFoundException, InstantiationException
  {
    final var YEAR = Duration.HOUR.times(24 * 7 * 52);
    final var durationSim = YEAR.dividedBy(2);
    final var jsonPlan = loadClipperModelAndPlan();
    final var oneYearSimulationWithCheckpoints = SimulationDriver.simulateWithCheckpoints(
        jsonPlan.missionModel,
        jsonPlan.plan(),
        jsonPlan.planningHorizon.start,
        durationSim,
        jsonPlan.planningHorizon.start,
        durationSim,
        $ -> {},
        SimulationDriver.CachedSimulationEngine.empty(jsonPlan.missionModel()),
        SimulationDriver.periodicCheckpoints(WEEK, durationSim, WEEK));

    final var best = SimulationDriver.bestCachedEngine(Map.of(), oneYearSimulationWithCheckpoints.checkpoints());
    final var ts = new TreeSet<Duration>();
    jsonPlan.plan.values().forEach(a -> ts.add(a.startOffset()));
    assertTrue(best.isPresent());
    assertTrue(best.get().startOffset().shorterThan(ts.first()));
  }

  @Test
  void testRemoveActivities()
  throws MissionModelLoader.MissionModelLoadException, IOException, InstantiationException, MerlinServiceException,
         NoSuchPlanException, URISyntaxException
  {
    final var nbIterations = 100;
    final var YEAR = Duration.HOUR.times(24 * 7 * 52);
    final var WEEK = Duration.HOUR.times(24 * 7);
    final var durationSim = YEAR.dividedBy(2);
    final var jsonPlan = loadClipperModelAndPlan();
    final var oneYearSimulationWithCheckpoints = SimulationDriver.simulateWithCheckpoints(
        jsonPlan.missionModel,
        jsonPlan.plan(),
        jsonPlan.planningHorizon.start,
        durationSim,
        jsonPlan.planningHorizon.start,
        durationSim,
        $ -> {},
        SimulationDriver.CachedSimulationEngine.empty(jsonPlan.missionModel()),
        SimulationDriver.periodicCheckpoints(WEEK, durationSim, WEEK));
    final var generator = new Random(17121950);
    for(var i = 0; i < nbIterations && i < oneYearSimulationWithCheckpoints.checkpoints().size(); i++){
      System.out.println("Iteration " + i);
      final var checkpoint = oneYearSimulationWithCheckpoints.checkpoints().get(i);
      final var slashedPlan = slashPlan(jsonPlan.plan(), checkpoint.startOffset(), 0.2, generator, durationSim);
      final var resultsWithSlashedPlan = SimulationDriver.simulateWithCheckpoints(
          jsonPlan.missionModel,
          slashedPlan,
          jsonPlan.planningHorizon.start,
          DURATIONSIM,
          jsonPlan.planningHorizon.start,
          DURATIONSIM,
          $ -> {},
          checkpoint,
          SimulationDriver.desiredCheckpoints(List.of()));
      //check that it is similar
      final SimulationResults expected = SimulationDriver.simulate(
          jsonPlan.missionModel,
          slashedPlan,
          jsonPlan.planningHorizon.start,
          durationSim,
          jsonPlan.planningHorizon.start,
          durationSim,
          $ -> {});
      assertEqualsSimResultsClipper(expected, resultsWithSlashedPlan.results(), slashedPlan);
    }
  }
  public Map<ActivityDirectiveId, ActivityDirective> slashPlan(
      final Map<ActivityDirectiveId, ActivityDirective> plan,
      final Duration start,
      final double proportion,
      final Random numberGenerator,
      final Duration endHorizon){
    assertTrue(proportion >= 0 && proportion <= 1);
    final var tm = new TreeMap<ActivityDirectiveId, ActivityDirective>(Comparator.comparingLong(ActivityDirectiveId::id));
    tm.putAll(plan);

    final Map<ActivityDirectiveId, ActivityDirective> amputatedPlan = new HashMap<>();
    for(final var element : tm.entrySet()){
      if(element.getValue().startOffset().longerThan(endHorizon)){
        continue;
      }
      if(element.getValue().startOffset().longerThan(start)){
        final var shouldRemove = numberGenerator.nextFloat() < proportion;
        if(!shouldRemove){
          amputatedPlan.put(element.getKey(), element.getValue());
        }
      } else {
        amputatedPlan.put(element.getKey(), element.getValue());
      }
    }
    return amputatedPlan;
  }

  @Test
  void testStartFromCheckpoint()
  throws MissionModelLoader.MissionModelLoadException, FileNotFoundException, InstantiationException
  {
    final var jsonPlan = loadClipperModelAndPlan();
    final SimulationDriver.SimulationResultsWithCheckpoints results = simulateWithCheckpoints(
        SimulationDriver.CachedSimulationEngine.empty(jsonPlan.missionModel()),
        List.of(Duration.of(5, MINUTES)),
        jsonPlan.missionModel());
    final SimulationResults expected = SimulationDriver.simulate(
        jsonPlan.missionModel,
        Map.of(),
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        $ -> {});
    assertEquals(expected, results.results());
    final SimulationDriver.SimulationResultsWithCheckpoints newResults = simulateWithCheckpoints(results.checkpoints().get(0), List.of(), jsonPlan.missionModel());
    assertEquals(expected, newResults.results());
  }

  static SimulationDriver.SimulationResultsWithCheckpoints simulateWithCheckpoints(
      final SimulationDriver.CachedSimulationEngine cachedEngine,
      final List<Duration> desiredCheckpoints,
      final MissionModel<?> missionModel
  ) {
    return SimulationDriver.simulateWithCheckpoints(
        missionModel,
        Map.of(),
        Instant.EPOCH,
        Duration.HOUR,
        Instant.EPOCH,
        Duration.HOUR,
        $ -> {},
        cachedEngine,
        SimulationDriver.desiredCheckpoints(desiredCheckpoints));
  }

  public record PlanningHorizon(Instant start, Instant end, Duration aerieStart, Duration aerieEnd){
    public static Duration durationBetween(Instant start, Instant end){
      return Duration.of(ChronoUnit.MICROS.between(start, end), Duration.MICROSECONDS);
    }
  }
  record MissionModelDescription(String name, Map<String, SerializedValue> config, Path libPath, Instant start) {}

  public record JsonPlan(PlanningHorizon planningHorizon, Map<ActivityDirectiveId, ActivityDirective> plan, MissionModel<?> missionModel){}

  public JsonPlan loadClipperModelAndPlan()
  throws MissionModelLoader.MissionModelLoadException, FileNotFoundException,
         InstantiationException
  {
    final MissionModelDescription
        CLIPPER = new MissionModelDescription(
        "eurc-duplicatable-branch.jar",
        Map.of(),
        Path.of(System.getenv("AERIE_ROOT"), "scheduler-worker", "src", "test", "resources"),
        null
    );
    final var initialPlan = loadPlanFromJson(
        Path.of(System.getenv("AERIE_ROOT"), "scheduler-worker", "src", "test", "resources",
                "cruise-rap-dev-10-30.json").toString(),
        CLIPPER);
    return initialPlan;
  }

  public JsonPlan loadPlanFromJson(final String path, final MissionModelDescription missionModelDescription)
  throws MissionModelLoader.MissionModelLoadException, InstantiationException,
         FileNotFoundException
  {

    final var jsonInputFile = new File(path);
    final var is = new FileInputStream(jsonInputFile);
    final var reader = Json.createReader(is);
    final var empObj = reader.readObject();
    final var planningHorizonStart = Instant.parse(empObj.getString("start_time"));
    final var planningHorizonEnd = Instant.parse(empObj.getString("end_time"));
    final var planningHorizon = new PlanningHorizon(
        planningHorizonStart,
        planningHorizonEnd,
        Duration.ZERO,
        PlanningHorizon.durationBetween(planningHorizonStart, planningHorizonEnd));
    final var activityDirectives = empObj.getJsonArray("activities");

    final var missionModel = MissionModelLoader.loadMissionModel(
        planningHorizonStart,
        SerializedValue.of(missionModelDescription.config()),
        missionModelDescription.libPath().resolve(missionModelDescription.name()),
        "",
        "");

    final var plan = new HashMap<ActivityDirectiveId, ActivityDirective>();
    for (int i = 0; i < activityDirectives.size(); i++) {
      final var jsonActivity = activityDirectives.getJsonObject(i);
      final var type = activityDirectives.getJsonObject(i).getString("type");
      final var start = jsonActivity.getString("start_offset");
      final var anchorId = jsonActivity.isNull("anchor_id") ? null : jsonActivity.getInt("anchor_id");
      final boolean anchoredToStart = jsonActivity.getBoolean("anchored_to_start");
      final var arguments = jsonActivity.getJsonObject("arguments");
      final var deserializedArguments = BasicParsers
          .mapP(serializedValueP)
          .parse(arguments)
          .getSuccessOrThrow();
      final var effectiveArguments = missionModel.getDirectiveTypes().directiveTypes().get(type).getInputType()
                                                 .getEffectiveArguments(deserializedArguments);
      final var merlinActivity = new ActivityDirective(
          durationFromPGInterval(start),
          type,
          effectiveArguments,
          (anchorId != null) ? new ActivityDirectiveId(anchorId) : null,
          anchoredToStart);
      final var actPK = new ActivityDirectiveId(jsonActivity.getJsonNumber("id").longValue());
      plan.put(actPK, merlinActivity);
    }
    return new JsonPlan(planningHorizon, plan, missionModel);
  }

  public static Duration durationFromPGInterval(final String pgInterval) {
    try {
      final var asInterval = new PGInterval(pgInterval);
      if(asInterval.getYears() != 0 ||
         asInterval.getMonths() != 0) throw new RuntimeException("Years or months found in a pginterval");
      final var asDuration = java.time.Duration.ofDays(asInterval.getDays())
                                               .plusHours(asInterval.getHours())
                                               .plusMinutes(asInterval.getMinutes())
                                               .plusSeconds(asInterval.getWholeSeconds())
                                               .plusNanos(asInterval.getMicroSeconds()*1000);
      return Duration.of(asDuration.toNanos()/1000, MICROSECONDS);
    }catch(SQLException e){
      throw new RuntimeException(e);
    }
  }

}
