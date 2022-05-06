package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchedulingIntegrationTests {

  private record MissionModelDescription(String name, Map<String, SerializedValue> config, Path libPath) {}

  private static final MissionModelDescription BANANANATION = new MissionModelDescription(
      "bananantion",
      Map.of("initialDataPath", SerializedValue.of("/etc/hosts")),
      Path.of(System.getenv("AERIE_ROOT"), "examples", "banananation", "build", "libs")
  );

  private MockMerlinService merlinService;
  private SchedulingDSLCompilationService schedulingDSLCompiler;

  @BeforeAll
  void setup() throws IOException {
    this.merlinService = new MockMerlinService();
    this.schedulingDSLCompiler = new SchedulingDSLCompilationService(new TypescriptCodeGenerationService(this.merlinService));
  }

  @Test
  void testEmptyPlanEmptySpecification() {
    final var results = runScheduler(BANANANATION, List.of(), List.of());
    assertEquals(Map.of(), results.scheduleResults.goalResults());
  }

  @Test
  void testEmptyPlanSimpleRecurrenceGoal() {
    final var results = runScheduler(
        BANANANATION,
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
    final var results = runScheduler(
        BANANANATION,
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
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(new MockMerlinService.PlannedActivityInstance(
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
            Duration.ZERO)),
        List.of("""
          export default () => Goal.CoexistenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            forEach: WindowSet.during(ActivityTypes.GrowBanana),
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

  @Test
  void testStateCoexistenceGoal() {
    // Initial plant count is 200 in default configuration
    // GrowBanana adds 100
    // PickBanana removes 100
    // Between the end of the GrowBanana, and the beginning of the PickBanana, the StateConstraint is satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(new MockMerlinService.PlannedActivityInstance(
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(100),
                "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
            Duration.of(2, Duration.HOURS)),
                new MockMerlinService.PlannedActivityInstance(
                    "PickBanana",
                    Map.of("quantity", SerializedValue.of(100)),
                    Duration.of(4, Duration.HOURS))),
        List.of("""
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: WindowSet.gt(Resources["/plant"], 201.0)
                 })
               }"""));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(growBanana.startTime().plus(growBananaDuration)));
    assertTrue(peelBanana.startTime().noLongerThan(pickBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testLessThan() {
    // Initial plant count is 200 in default configuration
    // GrowBanana adds 100
    // PickBanana removes 100
    // Between the end of the GrowBanana, and the beginning of the PickBanana, the StateConstraint is satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                    "GrowBanana",
                    Map.of(
                        "quantity", SerializedValue.of(100),
                        "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                    Duration.of(4, Duration.HOURS))),
        List.of("""
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: WindowSet.lt(Resources["/plant"], 199.0)
                 })
               }"""));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(pickBanana.startTime()));
    assertTrue(peelBanana.startTime().noLongerThan(growBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testEqualTo_satsified() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 100
    // GrowBanana adds 100
    // Between the end of the PickBanana, and the beginning of the GrowBanana, the StateConstraint is satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                Duration.of(4, Duration.HOURS))),
        List.of("""
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: WindowSet.eq(Resources["/plant"], 100.0)
                 })
               }"""));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(pickBanana.startTime()));
    assertTrue(peelBanana.startTime().noLongerThan(growBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testEqualTo_neverSatisfied() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 99
    // GrowBanana adds 100
    // The StateConstraint is never satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(99)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                Duration.of(4, Duration.HOURS))),
        List.of("""
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: WindowSet.eq(Resources["/plant"], 100.0)
                 })
               }"""));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(2, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    assertEquals(Duration.of(2, Duration.HOURS), pickBanana.startTime());
    assertEquals(Duration.of(4, Duration.HOURS), growBanana.startTime());
  }

  @Test
  void testNotEqualTo_satisfied() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 100
    // GrowBanana adds 100
    // The StateConstraint is satisfied between the two
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                Duration.of(4, Duration.HOURS))),
        List.of("""
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: WindowSet.neq(Resources["/plant"], 200.0)
                 })
               }"""));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(pickBanana.startTime()));
    assertTrue(peelBanana.startTime().noLongerThan(growBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
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

  private static MockMerlinService.MissionModelInfo getMissionModelInfo(final MissionModelDescription desc) {
    final var jarFile = getLatestJarFile(desc.libPath());
    try {
      return new MockMerlinService.MissionModelInfo(
          desc.libPath(),
          Path.of(jarFile.getName()),
          desc.name(),
          loadMissionModelTypesFromJar(jarFile.getAbsolutePath(), desc.config()),
          desc.config());
    } catch (MissionModelLoader.MissionModelLoadException e) {
      fail(e);
      throw new IllegalStateException("unreachable due to fail() call");
    }
  }

  private static File getLatestJarFile(final Path fooMissionModelLibPath) {
    final var files = fooMissionModelLibPath.toFile().listFiles(pathname -> pathname.getName().endsWith(".jar"));
    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
    return files[0];
  }

  private SchedulingRunResults runScheduler(
      final MissionModelDescription desc,
      final List<MockMerlinService.PlannedActivityInstance> plannedActivities,
      final Iterable<String> goals
  ) {
    this.merlinService.setMissionModel(getMissionModelInfo(desc));
    this.merlinService.setInitialPlan(plannedActivities);
    final var planId = new PlanId(1L);
    final var goalsByPriority = new ArrayList<GoalRecord>();
    var goalId = 0L;
    for (final var goal : goals) {
      final var goalResult = schedulingDSLCompiler.compileSchedulingGoalDSL(planId, goal);
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
    final var agent = new SynchronousSchedulerAgent(
        specificationService,
        this.merlinService,
        desc.libPath(),
        Path.of(""),
        PlanOutputMode.UpdateInputPlanWithNewActivities);
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

  static MissionModelService.MissionModelTypes loadMissionModelTypesFromJar(
      final String jarPath,
      final Map<String, SerializedValue> configuration)
  throws MissionModelLoader.MissionModelLoadException
  {
    final var missionModel = MissionModelLoader.loadMissionModel(
        SerializedValue.of(configuration),
        Path.of(jarPath),
        "",
        "");
    final Map<String, ? extends TaskSpecType<?, ?, ?>> taskSpecTypes = missionModel.getDirectiveTypes().taskSpecTypes();
    final var activityTypes = new ArrayList<MissionModelService.ActivityType>();
    for (final var entry : taskSpecTypes.entrySet()) {
      final var activityTypeName = entry.getKey();
      final var taskSpecType = entry.getValue();
      activityTypes.add(new MissionModelService.ActivityType(
          activityTypeName,
          taskSpecType
              .getParameters()
              .stream()
              .collect(Collectors.toMap(Parameter::name, Parameter::schema))));
    }

    final var resourceTypes = new ArrayList<MissionModelService.ResourceType>();
    for (final var entry : missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      resourceTypes.add(new MissionModelService.ResourceType(name, resource.getSchema()));
    }

    return new MissionModelService.MissionModelTypes(activityTypes, resourceTypes);
  }
}
