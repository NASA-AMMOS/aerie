package gov.nasa.jpl.aerie.scheduler.worker.services;

import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.scheduler.server.services.MerlinServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGInterval;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.scheduler.worker.services.SimulationResultsComparisonUtils.assertEqualsSimResultsClipper;
import static org.junit.jupiter.api.Assertions.fail;

public class ClipperSimulationDuplicationTest {
  @BeforeAll
  static void beforeAll() {
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
        "eurc_30_oct.jar",
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

    final File jsonInputFile = new File(path);
    final InputStream is = new FileInputStream(jsonInputFile);
    final JsonReader reader = Json.createReader(is);
    final JsonObject empObj = reader.readObject();
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
      final Integer anchorId = jsonActivity.isNull("anchor_id") ? null : jsonActivity.getInt("anchor_id");
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
      final PGInterval asInterval = new PGInterval(pgInterval);
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
