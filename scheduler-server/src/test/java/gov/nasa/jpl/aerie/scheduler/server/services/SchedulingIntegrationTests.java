package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.server.config.PlanOutputMode;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchedulingIntegrationTests {

  public static final TypescriptCodeGenerationService.MissionModelTypes MISSION_MODEL_TYPES =
      new TypescriptCodeGenerationService.MissionModelTypes(
          List.of(
              new TypescriptCodeGenerationService.ActivityType(
                  "PeelBanana",
                  Map.of(
                      "peelDirection",
                      ValueSchema.ofVariant(List.of(
                          new ValueSchema.Variant(
                              "fromTip", "fromTip"
                          ),
                          new ValueSchema.Variant(
                              "fromStem",
                              "fromStem"))
                      )
                  )
              ),
              new TypescriptCodeGenerationService.ActivityType(
                  "GrowBanana",
                  Map.of(
                      "growingDuration",
                      ValueSchema.REAL,
                      "quantity",
                      ValueSchema.REAL
                  )
              ),
              new TypescriptCodeGenerationService.ActivityType(
                  "BiteBanana",
                  Map.of(
                      "biteSize",
                      ValueSchema.REAL
                  )
              )
          ),
          null
      );

  private MockMerlinService merlinService;
  private Path banananationLibPath;
  private SchedulingDSLCompilationService schedulingDSLCompiler;

  @BeforeAll
  void setup() throws IOException {
    banananationLibPath = Path.of(System.getenv("AERIE_ROOT"), "examples", "banananation", "build", "libs");
    final var files = banananationLibPath.toFile().listFiles(pathname -> pathname.getName().endsWith(".jar"));
    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
    final var banananationJarFile = files[0];

    this.merlinService = new MockMerlinService(Path.of(banananationJarFile.getName()), "some-model-name");
    this.schedulingDSLCompiler = new SchedulingDSLCompilationService(new TypescriptCodeGenerationService(this.merlinService));
  }

  @Test
  void testEmptyPlanEmptySpecification() {
    final var results = runSchedulerOnBanananation(List.of(), List.of());
    assertEquals(Map.of(), results.scheduleResults.goalResults());
  }

  @Test
  void testEmptyPlanSimpleRecurrenceGoal() {
    final var results = runSchedulerOnBanananation(
        List.of(),
        List.of("""
          export default () => Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({
              peelDirection: "fromStem",
            }),
            interval: 24 * 60 * 60 * 1000 * 1000 // one day in microseconds
          })
          """));
    assertEquals(1, results.scheduleResults.goalResults().size());
    final var goalResult = results.scheduleResults.goalResults().get(new GoalId(0L));
    assertTrue(goalResult.satisfied());
    assertEquals(4, goalResult.createdActivities().size());
    for (final var activity : goalResult.createdActivities()) {
      assertNotNull(activity);
    }
    for (final var activity : goalResult.satisfyingActivities()) {
      assertNotNull(activity);
    }
    final var updatedPlan = results.updatedPlan();
    for (final var activity : updatedPlan) {
      final var arguments = activity.args();
      assertEquals("PeelBanana", activity.type());
      assertEquals(SerializedValue.of("fromStem"), arguments.get("peelDirection"));
    }
  }

  @Test
  void testSingleActivityPlanSimpleRecurrenceGoal() {
    final var results = runSchedulerOnBanananation(
        List.of(new MockMerlinService.PlannedActivityInstance("BiteBanana", Map.of("biteSize", SerializedValue.of(1)), Duration.ZERO)),
        List.of("""
          export default () => Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            interval: 24 * 60 * 60 * 1000 * 1000 // one day in microseconds
          })
          """));

    assertEquals(1, results.scheduleResults.goalResults().size());
    final var goalResult = results.scheduleResults.goalResults().get(new GoalId(0L));

    assertTrue(goalResult.satisfied());
    assertEquals(4, goalResult.createdActivities().size());
    for (final var activity : goalResult.createdActivities()) {
      assertNotNull(activity);
    }
    for (final var activity : goalResult.satisfyingActivities()) {
      assertNotNull(activity);
    }

    final var activitiesByType = partitionByActivityType(results.updatedPlan());
    final var biteBananas = activitiesByType.get("BiteBanana");
    assertEquals(1, biteBananas.size());

    final var biteBanana = biteBananas.iterator().next();
    assertEquals(SerializedValue.of(1), biteBanana.args().get("biteSize"));

    final var peelBananas = activitiesByType.get("PeelBanana");
    assertEquals(4, peelBananas.size());

    for (final var peelBanana : peelBananas) {
      assertEquals(SerializedValue.of("fromStem"), peelBanana.args().get("peelDirection"));
    }
  }

  @Test
  void testSingleActivityPlanSimpleCoexistenceGoal() {
    // TODO Coexistence goal's "forEach" doesn't work on activities with zero Duration
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runSchedulerOnBanananation(
        List.of(new MockMerlinService.PlannedActivityInstance(
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
            Duration.ZERO)),
        List.of("""
          export default () => Goal.CoexistenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            forEach: ActivityTypes.GrowBanana,
          })
          """));

    assertEquals(1, results.scheduleResults.goalResults().size());
    final var goalResult = results.scheduleResults.goalResults().get(new GoalId(0L));

    assertTrue(goalResult.satisfied());
    assertEquals(1, goalResult.createdActivities().size());
    for (final var activity : goalResult.createdActivities()) {
      assertNotNull(activity);
    }
    for (final var activity : goalResult.satisfyingActivities()) {
      assertNotNull(activity);
    }

    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var peelBananas = planByActivityType.get("PeelBanana");
    final var growBananas = planByActivityType.get("GrowBanana");
    assertEquals(1, peelBananas.size());
    assertEquals(1, growBananas.size());
    final var peelBanana = peelBananas.iterator().next();
    final var growBanana = growBananas.iterator().next();

    assertEquals(SerializedValue.of("fromStem"), peelBanana.args().get("peelDirection"));
    assertEquals(SerializedValue.of(1), growBanana.args().get("quantity"));

    assertEquals(growBanana.startTime().plus(growBananaDuration), peelBanana.startTime());
  }

  private static Map<String, Collection<MockMerlinService.PlannedActivityInstance>>
  partitionByActivityType(final Iterable<MockMerlinService.PlannedActivityInstance> activities) {
    final var result = new HashMap<String, Collection<MockMerlinService.PlannedActivityInstance>>();
    for (final var activity : activities) {
      result
          .computeIfAbsent(activity.type(), key -> new ArrayList<>())
          .add(activity);
    }
    return result;
  }

  private SchedulingRunResults runSchedulerOnBanananation(List<MockMerlinService.PlannedActivityInstance> plannedActivities, final Iterable<String> goals)
  {
    this.merlinService.setInitialPlan(plannedActivities);
    final var planId = new PlanId(1L);
    final var goalsByPriority = new ArrayList<GoalRecord>();
    var goalId = 0L;
    for (final var goal : goals) {
      final var goalResult = schedulingDSLCompiler.compileSchedulingGoalDSL(planId, goal, "");
      if (goalResult instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Success s) {
        goalsByPriority.add(new GoalRecord(new GoalId(goalId++), s.goalSpecifier()));
      } else if (goalResult instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error e) {
        fail(e.toString());
      }
    }
    final var specificationService = new MockSpecificationService(Map.of(new SpecificationId(1L), new Specification(
        planId,
        1L,
        goalsByPriority,
        Timestamp.fromString("2021-001T00:00:00"),
        Timestamp.fromString("2021-005T00:00:00"),
        Map.of())));
    final var agent = new SynchronousSchedulerAgent(specificationService, this.merlinService, this.banananationLibPath, Path.of(""), PlanOutputMode.UpdateInputPlanWithNewActivities);
    // Scheduling Goals -> Scheduling Specification
    final var writer = new MockResultsProtocolWriter();
    agent.schedule(new ScheduleRequest(new SpecificationId(1L), $ -> RevisionData.MatchResult.success()), writer);
    assertEquals(1, writer.results.size());
    final var result = writer.results.get(0);
    if (result instanceof MockResultsProtocolWriter.Result.Failure e) {
      System.err.println(e.reason());
      fail(e.reason());
    }
    return new SchedulingRunResults(((MockResultsProtocolWriter.Result.Success) result).results(), this.merlinService.updatedPlan);
  }

  record SchedulingRunResults(ScheduleResults scheduleResults, Collection<MockMerlinService.PlannedActivityInstance> updatedPlan) {}
}
