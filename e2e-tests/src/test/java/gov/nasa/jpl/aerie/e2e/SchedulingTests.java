package gov.nasa.jpl.aerie.e2e;

import com.microsoft.playwright.Playwright;
import gov.nasa.jpl.aerie.e2e.types.ExternalDataset.ProfileInput;
import gov.nasa.jpl.aerie.e2e.types.ExternalDataset.ProfileInput.ProfileSegmentInput;
import gov.nasa.jpl.aerie.e2e.types.Plan;
import gov.nasa.jpl.aerie.e2e.types.ProfileSegment;
import gov.nasa.jpl.aerie.e2e.types.ValueSchema;
import gov.nasa.jpl.aerie.e2e.utils.GatewayRequests;
import gov.nasa.jpl.aerie.e2e.utils.HasuraRequests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchedulingTests {
  // Requests
  private Playwright playwright;
  private HasuraRequests hasura;

  // Per-Test Data
  private int modelId;
  private int planId;
  private int schedulingSpecId;

  // Cross-Test Constants
  private final String planStartTimestamp = "2023-01-01T00:00:00+00:00";
  private final String planEndTimestamp = "2023-01-02T00:00:00+00:00";
  private final String recurrenceGoalDefinition =
      """
      export default function myGoal() {
        return Goal.ActivityRecurrenceGoal({
          activityTemplate: ActivityTemplates.PeelBanana({peelDirection: 'fromStem'}),
          interval: Temporal.Duration.from({hours:1})
      })}""";
  private final String coexistenceGoalDefinition =
      """
      export default function myGoal() {
        return Goal.CoexistenceGoal({
          forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
          activityTemplate: ActivityTemplates.BiteBanana({biteSize: 1}),
          startsAt:TimingConstraint.singleton(WindowProperty.END)
        })
      }""";
  private final String plantCountGoalDefinition =
      """
      export default () => Goal.CoexistenceGoal({
        forEach: Real.Resource("/plant").lessThan(300),
        activityTemplate: ActivityTemplates.GrowBanana({quantity: 100, growingDuration: Temporal.Duration.from({minutes:1}) }),
        startsAt: TimingConstraint.singleton(WindowProperty.START)
      })""";

  private final String bakeBananaGoalDefinition =
      """
      export default (): Goal =>
          Goal.ActivityRecurrenceGoal({
              activityTemplate: ActivityTemplates.BakeBananaBread({
                  temperature: 325.0,
                  tbSugar: 2,
                  glutenFree: false,
              }),
              interval: Temporal.Duration.from({ hours: 12 }),
          });""";

  @BeforeAll
  void beforeAll() {
    // Setup Requests
    playwright = Playwright.create();
    hasura = new HasuraRequests(playwright);
  }

  @AfterAll
  void afterAll() {
    // Cleanup Requests
    hasura.close();
    playwright.close();
  }

  @BeforeEach
  void beforeEach() throws IOException {
    // Insert the Mission Model
    try (final var gateway = new GatewayRequests(playwright)) {
      modelId = hasura.createMissionModel(
          gateway.uploadJarFile(),
          "Banananation (e2e tests)",
          "aerie_e2e_tests",
          "Scheduling Tests");
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
    // Insert the Plan
    planId = hasura.createPlan(
        modelId,
        "Test Plan - Scheduling Tests",
        "24:00:00",
        planStartTimestamp);

    // Insert Scheduling Spec
    schedulingSpecId = hasura.insertSchedulingSpecification(
        planId,
        hasura.getPlanRevision(planId),
        planStartTimestamp,
        planEndTimestamp,
        JsonValue.EMPTY_JSON_OBJECT,
        false);
  }

  @AfterEach
  void afterEach() throws IOException {
    // Remove Model and Plan
    hasura.deletePlan(planId);
    hasura.deleteMissionModel(modelId);
  }

  private void insertActivities() throws IOException {
    // Duration argument is specified on one but not the other to verify that the scheduler can pick up on effective args
    hasura.insertActivity(planId, "GrowBanana", "1h", JsonValue.EMPTY_JSON_OBJECT);
    hasura.insertActivity(planId, "GrowBanana", "3h", Json.createObjectBuilder()
                                                          .add("growingDuration", 7200000000L) // 2h
                                                          .build());
    hasura.updatePlanRevisionSchedulingSpec(planId);
  }

  //reproduces issue #1165
  @Test
  void twoInARow() throws IOException {
    // Setup: Add Goal
    final int bakeBananaBreadGoalId = hasura.insertSchedulingGoal(
        "BakeBanana Scheduling Test Goal",
        modelId,
        bakeBananaGoalDefinition);
    hasura.createSchedulingSpecGoal(bakeBananaBreadGoalId, schedulingSpecId, 0);
    try {
      // Schedule and get Plan
      hasura.awaitScheduling(schedulingSpecId);
      hasura.updatePlanRevisionSchedulingSpec(planId);
      hasura.awaitScheduling(schedulingSpecId);
      final var plan = hasura.getPlan(planId);
      final var activities = plan.activityDirectives();
      assertEquals(2, activities.size());
      activities.forEach(a->assertEquals("BakeBananaBread", a.type()));
    } finally {
      // Teardown: Delete Goal
      hasura.deleteSchedulingGoal(bakeBananaBreadGoalId);
    }
  }

  @Test
  void getSchedulingDSLTypeScript() throws IOException {
    final var schedulingDslTypes = hasura.getSchedulingDslTypeScript(modelId);
    assertEquals(7, schedulingDslTypes.typescriptFiles().size());
    assertEquals("success", schedulingDslTypes.status());
    assertNull(schedulingDslTypes.reason());
  }

  @Test
  void schedulingRecurrenceGoal() throws IOException {
    // Setup: Add Goal
    final int recurrenceGoalId = hasura.insertSchedulingGoal(
        "Recurrence Scheduling Test Goal",
        modelId,
        recurrenceGoalDefinition);
    hasura.createSchedulingSpecGoal(recurrenceGoalId, schedulingSpecId, 0);
    try {
      // Schedule and get Plan
      hasura.awaitScheduling(schedulingSpecId);
      final var plan = hasura.getPlan(planId);
      final var activities = plan.activityDirectives();

      // Assert that the correct number of PeelBananas were scheduled
      assertEquals(24, activities.size());
      activities.forEach(a->assertEquals("PeelBanana", a.type()));
    } finally {
      // Teardown: Delete Goal
      hasura.deleteSchedulingGoal(recurrenceGoalId);
    }
  }

  @Test
  void schedulingCoexistenceGoal() throws IOException {
    // Setup: Add Goal and Activities
    insertActivities();
    final int coexistenceGoalId = hasura.insertSchedulingGoal(
        "Coexistence Scheduling Test Goal",
        modelId,
        coexistenceGoalDefinition);
    hasura.createSchedulingSpecGoal(coexistenceGoalId, schedulingSpecId, 0);

    try {
      // Schedule and get Plan
      hasura.awaitScheduling(schedulingSpecId);
      final var plan = hasura.getPlan(planId);
      final var activities = plan.activityDirectives();

      assertEquals(4, activities.size());

      // Assert the correct number of each activity type exists
      int growBananaCount = 0;
      int biteBananaCount = 0;
      for (final var activity : activities) {
        switch (activity.type()) {
          case "GrowBanana" -> growBananaCount++;
          case "BiteBanana" -> biteBananaCount++;
          default -> fail("Encountered unexpected activity type in plan: " + activity.type());
        }
      }
      assertEquals(2, growBananaCount);
      assertEquals(2, biteBananaCount);

    } finally {
       // Teardown: Delete Goal
      hasura.deleteSchedulingGoal(coexistenceGoalId);
    }
  }

  @Test
  void schedulingMultipleGoals() throws IOException {
    // Setup: Add Goals
    insertActivities();
    final int recurrenceGoalId = hasura.insertSchedulingGoal(
        "Recurrence Scheduling Test Goal",
        modelId,
        recurrenceGoalDefinition);
    hasura.createSchedulingSpecGoal(recurrenceGoalId, schedulingSpecId, 0);
    final int coexistenceGoalId = hasura.insertSchedulingGoal(
        "Coexistence Scheduling Test Goal",
        modelId,
        coexistenceGoalDefinition);
    hasura.createSchedulingSpecGoal(coexistenceGoalId, schedulingSpecId, 1);
    try {
      // Schedule and get Plan
      hasura.awaitScheduling(schedulingSpecId);
      final var activities = hasura.getPlan(planId).activityDirectives();

      assertEquals(28, activities.size());

      // Assert the correct number of each activity type exists
      int growBananaCount = 0;
      int biteBananaCount = 0;
      int peelBananaCount = 0;
      for (final var activity : activities) {
        switch (activity.type()) {
          case "GrowBanana" -> growBananaCount++;
          case "BiteBanana" -> biteBananaCount++;
          case "PeelBanana" -> peelBananaCount++;
          default -> fail("Encountered unexpected activity type in plan: " + activity.type());
        }
      }
      assertEquals(2, growBananaCount);
      assertEquals(2, biteBananaCount);
      assertEquals(24, peelBananaCount);
    } finally {
      // Teardown: Delete Goals
      hasura.deleteSchedulingGoal(recurrenceGoalId);
      hasura.deleteSchedulingGoal(coexistenceGoalId);
    }
  }

  @Test
  void schedulingPostsSimResults() throws IOException {
    insertActivities();
    final var schedulingResults = hasura.awaitScheduling(schedulingSpecId);
    final int datasetId = schedulingResults.datasetId();
    final var plan = hasura.getPlan(planId);

    final var simResults = hasura.getSimulationDatasetByDatasetId(datasetId);

    // All directive have their simulated activity
    final var planActivities = plan.activityDirectives();
    final var simActivities = simResults.activities();

    assertEquals(2, planActivities.size());
    assertEquals(planActivities.size(), simActivities.size());
    for(int i = 0; i<planActivities.size(); ++i) {
      assertEquals(planActivities.get(i).id(), simActivities.get(i).directiveId());
      assertEquals(planActivities.get(i).startOffset(), simActivities.get(i).startOffset());
    }

    final var profiles = hasura.getProfiles(datasetId);
    // Expect one profile per resource in the Banananation model
    assertEquals(7, profiles.size());
    assertTrue(profiles.keySet().containsAll(List.of(
        "/flag",
        "/flag/conflicted",
        "/peel",
        "/fruit",
        "/plant",
        "/producer",
        "/data/line_count")));

    // Fruit resource is impacted by the two GrowBananas, which each create two segments
    final var fruitSegments = profiles.get("/fruit");
    assertEquals(5, fruitSegments.size());
    assertEquals("00:00:00", fruitSegments.get(0).startOffset()); // plan start
    assertEquals("01:00:00", fruitSegments.get(1).startOffset()); // GB1 start
    assertEquals("02:00:00", fruitSegments.get(2).startOffset()); // GB1 end
    assertEquals("03:00:00", fruitSegments.get(3).startOffset()); // GB2 start
    assertEquals("05:00:00", fruitSegments.get(4).startOffset()); // GB2 end

    final var initialFruit = Json.createObjectBuilder().add("rate", 0.0).add("initial", 4.0).build();
    assertEquals(initialFruit, fruitSegments.get(0).dynamics());

    // Plant resource is impacted by the two GrowBananas, which each create one segment
    final var plantSegments = profiles.get("/plant");
    assertEquals(3, plantSegments.size());
    assertEquals("00:00:00", plantSegments.get(0).startOffset()); // plan start
    assertEquals("02:00:00", plantSegments.get(1).startOffset()); // GB1 end
    assertEquals("05:00:00", plantSegments.get(2).startOffset()); // GB2 end

    final var topics = hasura.getTopicsEvents(datasetId);
    assertEquals(41, topics.size());
    // Assert that the keys to be inspected are included
    assertTrue(topics.containsKey("ActivityType.Input.GrowBanana"));
    assertTrue(topics.containsKey("ActivityType.Output.GrowBanana"));
    for(final var topic : topics.entrySet()) {
      switch (topic.getKey()) {
        case "ActivityType.Input.GrowBanana",
            "ActivityType.Output.GrowBanana" -> assertEquals(2, topic.getValue().events().size());
        // No other topic should have events
        default -> assertTrue(topic.getValue().events().isEmpty());
      }
    }
  }

  @Nested
  class SchedulingWithExistingSimResults {
    @BeforeEach
    public void beforeEach() throws IOException {
      insertActivities();
    }

    /**
     * This tests the check on Plan Revision before loading initial simulation results.
     * In this test, there are only two GrowBanana activities in the latest sim results
     * instead of the actual 3 present in the latest plan revision.
     * A coexistence goal attaching to GrowBanana activities shows that the scheduler did not use the stale sim results.
     */
    @Test
    void outdatedPlanRevision() throws IOException {
      hasura.awaitSimulation(planId);
      // Add Grow Banana
      hasura.insertActivity(planId, "GrowBanana", "5h", JsonObject.EMPTY_JSON_OBJECT);

      // Setup: Add Goal
      final int coexistenceGoalId = hasura.insertSchedulingGoal(
          "Coexistence Scheduling Test Goal",
          modelId,
          coexistenceGoalDefinition);
      hasura.createSchedulingSpecGoal(coexistenceGoalId, schedulingSpecId, 0);

      try {
        hasura.updatePlanRevisionSchedulingSpec(planId);

        // Schedule and get Plan
        hasura.awaitScheduling(schedulingSpecId);
        final var plan = hasura.getPlan(planId);
        final var activities = plan.activityDirectives();

        assertEquals(6, activities.size());

        // Assert the correct number of each activity type exists
        int growBananaCount = 0;
        int biteBananaCount = 0;
        for (final var activity : activities) {
          switch (activity.type()) {
            case "GrowBanana" -> growBananaCount++;
            case "BiteBanana" -> biteBananaCount++;
            default -> fail("Encountered unexpected activity type in plan: " + activity.type());
          }
        }
        assertEquals(3, growBananaCount);
        assertEquals(3, biteBananaCount);
      } finally {
        // Teardown: Delete Goal
        hasura.deleteSchedulingGoal(coexistenceGoalId);
      }
    }

    /**
     * This tests the check on Simulation Config before loading initial simulation results.
     * In this test, the latest results had plant count always >= 400 due to the sim template.
     * However, the latest config has plant count start at 200, less than the 300 goal.
     * If the injected results are picked up, no activities will be added.
     */
    @Test
    void outdatedSimConfig() throws IOException {
      final int templateId = hasura.insertAndAssociateSimTemplate(
          modelId,
          "Scheduling Tests Template",
          Json.createObjectBuilder().add("initialPlantCount", 400).build(),
          hasura.getSimulationId(planId)
      );
      hasura.awaitSimulation(planId);
      hasura.deleteSimTemplate(templateId); // Return to blank sim config args

      final int plantGoal = hasura.insertSchedulingGoal(
          "Scheduling Test: When Plant < 300",
          modelId,
          plantCountGoalDefinition);
      hasura.createSchedulingSpecGoal(plantGoal, schedulingSpecId, 0);

      try {
        hasura.awaitScheduling(schedulingSpecId);
        final var activities = hasura.getPlan(planId).activityDirectives();

        assertEquals(3, activities.size()); // Two Grow Bananas were already in the plan
        activities.forEach(a -> assertEquals("GrowBanana", a.type()));
        activities.sort(Comparator.comparingInt(Plan.ActivityDirective::id));
        assertEquals("00:00:00", activities.get(2).startOffset());
      } finally {
        hasura.deleteSchedulingGoal(plantGoal);
      }
    }

    /**
     * This tests that Scheduling loads the most recent sim results if it can.
     * In this test, simulation results inconsistent with the sim config are manually inserted.
     * If the scheduler correctly loads the injected results, no activities will be added,
     * as plant count will always be > 300.
     */
    @Test
    void injectedResultsLoaded() throws IOException{
      // Insert sim results
      final int datasetId = hasura.insertSimDataset(
          hasura.getSimulationId(planId),
          planStartTimestamp,
          planEndTimestamp,
          "success",
          JsonObject.EMPTY_JSON_OBJECT,
          hasura.getPlanRevision(planId));
      final var plantType = Json.createObjectBuilder()
                                .add("type", "discrete")
                                .add("schema", Json.createObjectBuilder().add("type", "int"))
                                .build();
      hasura.insertProfile(
          datasetId,
          "/plant",
          "24h",
          plantType,
          List.of(new ProfileSegment("0h", false, Json.createValue(400))));

      // Insert Goal
      final int plantGoal = hasura.insertSchedulingGoal(
          "Scheduling Test: When Plant < 300",
          modelId,
          plantCountGoalDefinition);
      hasura.createSchedulingSpecGoal(plantGoal, schedulingSpecId, 0);

      try {
        hasura.awaitScheduling(schedulingSpecId);
        final var activities = hasura.getPlan(planId).activityDirectives();

        assertEquals(2, activities.size()); // Two Grow Bananas were already in the plan
        activities.forEach(a -> assertEquals("GrowBanana", a.type()));
      } finally {
        hasura.deleteSchedulingGoal(plantGoal);
      }
    }

    /**
     * This tests that Scheduling does not load temporal subset sim results.
     * In this test, simulation is only run until the end of the first GrowBanana.
     * If the scheduler correctly doesn't load the subset sim results, two BiteBananas will be added.
     */
    @Test
    void temporalSubsetExcluded() throws IOException {
      hasura.updateSimBounds(planId, planStartTimestamp, "2023-01-01T02:30:00+00:00"); // Between GB1 and GB2
      hasura.awaitSimulation(planId);

      // Setup: Add Goal
      final int coexistenceGoalId = hasura.insertSchedulingGoal(
          "Coexistence Scheduling Test Goal",
          modelId,
          coexistenceGoalDefinition);
      hasura.createSchedulingSpecGoal(coexistenceGoalId, schedulingSpecId, 0);

      try {
        // Schedule and get Plan
        hasura.awaitScheduling(schedulingSpecId);
        final var plan = hasura.getPlan(planId);
        final var activities = plan.activityDirectives();

        assertEquals(4, activities.size());

        // Assert the correct number of each activity type exists
        int growBananaCount = 0;
        int biteBananaCount = 0;
        for (final var activity : activities) {
          switch (activity.type()) {
            case "GrowBanana" -> growBananaCount++;
            case "BiteBanana" -> biteBananaCount++;
            default -> fail("Encountered unexpected activity type in plan: " + activity.type());
          }
        }
        assertEquals(2, growBananaCount);
        assertEquals(2, biteBananaCount);

      } finally {
        // Teardown: Delete Goal
        hasura.deleteSchedulingGoal(coexistenceGoalId);
      }
    }
  }

  @Nested
  class SchedulingDecomposition {
    int longPlanId;
    int cardinalityGoalId;

    @BeforeEach
    void beforeEach() throws IOException {
      // Add Elongated Plan
      longPlanId = hasura.createPlan(
          modelId,
          "Long Test Plan - Scheduling Tests",
          "2136:00:00",
          planStartTimestamp);

      // Insert Scheduling Spec
      schedulingSpecId = hasura.insertSchedulingSpecification(
          longPlanId,
          hasura.getPlanRevision(longPlanId),
          planStartTimestamp,
          "2023-090T00:00:00.000",
          JsonValue.EMPTY_JSON_OBJECT,
          false);

      // Add Goal
      cardinalityGoalId = hasura.insertSchedulingGoal(
          "Cardinality and Decomposition Scheduling Test Goal",
          modelId,
          """
          export default function cardinalityGoalExample() {
            return Goal.CardinalityGoal({
              activityTemplate: ActivityTemplates.parent({ label: "unlabeled"}),
              specification: { duration: Temporal.Duration.from({ seconds: 10 }) },
          });}""");
      hasura.createSchedulingSpecGoal(cardinalityGoalId, schedulingSpecId, 0);
    }

    @AfterEach
    void afterEach() throws IOException {
      hasura.deleteSchedulingGoal(cardinalityGoalId);
      hasura.deletePlan(longPlanId);
    }

    @Test
    void schedulingCardinalityGoal() throws IOException {
      // Schedule and get Plan
      final int datasetId = hasura.awaitScheduling(schedulingSpecId).datasetId();
      final var plan = hasura.getPlan(longPlanId);
      final var activities = plan.activityDirectives();

      assertEquals(1, activities.size());
      final var parentDirective = activities.get(0);
      assertEquals("parent", parentDirective.type());

      // Check Posted Spans
      final var simulatedActivities = hasura.getSimulationDatasetByDatasetId(datasetId).activities();
      assertEquals(7, simulatedActivities.size()); // 1 parent, 2 children, 4 grandchildren

      // Assert Parent Span
      final var parentSpans = simulatedActivities.stream().filter((a -> a.type().equals("parent"))).toList();
      assertEquals(1, parentSpans.size());
      final var parentSpan = parentSpans.get(0);
      assertNull(parentSpan.parentId());
      assertEquals(parentDirective.id(), parentSpan.directiveId());

      // Assert Children Spans
      final var childSpans = simulatedActivities.stream().filter((a -> a.type().equals("child"))).toList();
      assertEquals(2, childSpans.size());
      childSpans.forEach(cs -> {
        assertNull(cs.directiveId());
        assertEquals(parentSpan.spanId(), cs.parentId());
      });

      final var child1 = childSpans.get(0);
      final var child2 = childSpans.get(1);

      // Assert Grandchildren Spans
      final var grandchildSpans = simulatedActivities.stream().filter((a -> a.type().equals("grandchild"))).toList();
      assertEquals(4, grandchildSpans.size());
      grandchildSpans.forEach(gcs -> assertNull(gcs.directiveId()));

      final var gcsFirstChild = grandchildSpans.stream().filter((s -> s.parentId() == child1.spanId())).toList();
      assertEquals(2, gcsFirstChild.size());

      final var gcsSecondChild = grandchildSpans.stream().filter((s -> s.parentId() == child2.spanId())).toList();
      assertEquals(2, gcsSecondChild.size());
    }
  }

  @Nested
  class SchedulingExternalDatasets {
    private int datasetId;
    private int edGoalId;

    @BeforeEach
    void beforeEach() throws IOException {
      // Create External Dataset
      final var myBooleanProfile = new ProfileInput(
          "/my_boolean",
          "discrete",
          ValueSchema.VALUE_SCHEMA_BOOLEAN,
          List.of(
              new ProfileSegmentInput(3600000000L, JsonValue.FALSE),
              new ProfileSegmentInput(3600000000L, JsonValue.TRUE),
              new ProfileSegmentInput(3600000000L, JsonValue.NULL),
              new ProfileSegmentInput(3600000000L, JsonValue.FALSE)));

      datasetId = hasura.insertExternalDataset(
          planId,
          "2023-001T00:00:00.000",
          List.of(myBooleanProfile));

      // Insert Goal
      edGoalId = hasura.insertSchedulingGoal(
          "On my_boolean true",
          modelId,
          """
          export default function myGoal() {
            return Goal.CoexistenceGoal({
              forEach: Discrete.Resource("/my_boolean").equal(true).assignGaps(false),
              activityTemplate: ActivityTemplates.BiteBanana({ biteSize: 1, }),
              startsAt:TimingConstraint.singleton(WindowProperty.END)
            })
          }""");
      // Add the goal
      hasura.createSchedulingSpecGoal(edGoalId, schedulingSpecId, 0);
    }

    @AfterEach
    void afterEach() throws IOException {
      // Cleanup Plan, Goal, External Dataset
      hasura.deleteSchedulingGoal(edGoalId);
      hasura.deleteExternalDataset(planId, datasetId);
    }

    /**
     * Verify that when providing an external plan dataset,
     * the resources in that dataset are included in the scheduling edsl
     */
    @Test
    void schedulingDSLGeneratesExternalTypes() throws IOException {
      final var schedulingDslTypes = hasura.getSchedulingDslTypeScript(modelId, planId);
      assertEquals(7, schedulingDslTypes.typescriptFiles().size());
      assertEquals("success", schedulingDslTypes.status());
      assertNull(schedulingDslTypes.reason());

      final var findFile = schedulingDslTypes.typescriptFiles()
                                             .stream()
                                             .filter(ts -> ts
                                                 .filePath()
                                                 .equals("file:///mission-model-generated-code.ts"))
                                             .toList();
      assertEquals(1, findFile.size());

      // Create a list of the exported resource types, one entry per new line
      final List<String> resourceTypes = Arrays.stream(findFile.get(0)
                                       .content()
                                       .split("export type Resource = \\{\\n")[1]
                                       .split("\\n};")[0] // isolate to export type Resource block
                                       .split("\\n"))
                                       .map(String::strip) // remove whitespace
                                       .toList();

      final var expectedResources = List.of(
          "\"/data/line_count\": number,",
          "\"/flag\": ( | \"A\" | \"B\"),",
          "\"/flag/conflicted\": boolean,",
          "\"/fruit\": {initial: number, rate: number, },",
          "\"/my_boolean\": boolean,",
          "\"/peel\": number,",
          "\"/plant\": number,",
          "\"/producer\": string,"
      );

      assertEquals(expectedResources.size(), resourceTypes.size());
      assertTrue(resourceTypes.containsAll(expectedResources));
    }

    /**
     * Verify that it is possible to use an external resource in a scheduling goal
     */
    @Test
    void useExternalDatasetInGoal() throws IOException {
      hasura.awaitScheduling(schedulingSpecId);

      // Expect one BiteBanana placed at start of `true` window for `/my_boolean` resource
      final var activities = hasura.getPlan(planId).activityDirectives();
      assertEquals(1, activities.size());
      final var activity = activities.get(0);
      assertEquals("BiteBanana", activity.type());
      assertEquals("02:00:00", activity.startOffset());
      assertEquals(Json.createObjectBuilder().add("biteSize", 1).build(), activity.arguments());
    }
  }
}
