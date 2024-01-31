package gov.nasa.jpl.aerie.e2e;

import com.microsoft.playwright.Playwright;
import gov.nasa.jpl.aerie.e2e.types.ConstraintError;
import gov.nasa.jpl.aerie.e2e.types.ExternalDataset.ProfileInput;
import gov.nasa.jpl.aerie.e2e.types.ExternalDataset.ProfileInput.ProfileSegmentInput;
import gov.nasa.jpl.aerie.e2e.types.ValueSchema;
import gov.nasa.jpl.aerie.e2e.utils.GatewayRequests;
import gov.nasa.jpl.aerie.e2e.utils.HasuraRequests;
import org.junit.jupiter.api.*;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConstraintsTests {
  // Requests
  private Playwright playwright;
  private HasuraRequests hasura;

  // Per-Test Data
  private int modelId;
  private int planId;
  private int activityId;
  private int constraintId;

  private final String constraintName = "fruit_equal_peel";
  private final String constraintDefinition =
      "export default (): Constraint => Real.Resource(\"/fruit\").equal(Real.Resource(\"/peel\"))";

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
  void beforeEach() throws IOException, InterruptedException {
    // Insert the Mission Model
    try (final var gateway = new GatewayRequests(playwright)) {
      modelId = hasura.createMissionModel(
          gateway.uploadJarFile(),
          "Banananation (e2e tests)",
          "aerie_e2e_tests",
          "Constraints Tests");
    }
    // Insert the Plan
    planId = hasura.createPlan(
        modelId,
        "Test Plan - Constraints Tests",
        "1212h",
        "2021-01-01T00:00:00Z");
    //Insert the Activity
    activityId = hasura.insertActivity(
        planId,
        "BiteBanana",
        "1h",
        Json.createObjectBuilder().add("biteSize", 1).build());
    // Insert the Constraint
    constraintId = hasura.insertPlanConstraint(
        constraintName,
        planId,
        constraintDefinition,
        "");
  }

  @AfterEach
  void afterEach() throws IOException {
    // Remove Model, Plan, and Constraint
    hasura.deleteConstraint(constraintId);
    hasura.deletePlan(planId);
    hasura.deleteMissionModel(modelId);
  }

  @Test
  void constraintsFailNoSimData() {
    final var exception = assertThrows(RuntimeException.class, () -> hasura.checkConstraints(planId));
    final var message = exception.getMessage().split("\"message\":\"")[1].split("\"}]")[0];
    // Hasura strips the cause message ("Assumption falsified -- mission model for existing plan does not exist")
    // from the error it returns
    if (!message.equals("input mismatch exception")) {
      throw exception;
    }
  }

  @Test
  void constraintsSucceedOneViolation() throws IOException {
    hasura.awaitSimulation(planId);
    final var constraintsResponses = hasura.checkConstraints(planId);
    assertEquals(1, constraintsResponses.size());

    // Check the Response
    final var constraintResponse = constraintsResponses.get(0);
    assertTrue(constraintResponse.success());
    assertEquals(constraintId, constraintResponse.constraintId());
    assertEquals(constraintName, constraintResponse.constraintName());
    assertEquals("plan", constraintResponse.type());
    // Check the Result
    assertTrue(constraintResponse.result().isPresent());
    final var constraintResult = constraintResponse.result().get();
    // Resources
    final var resources = constraintResult.resourceIds();
    assertEquals(2, resources.size());
    assertTrue(resources.containsAll(List.of("/peel", "/fruit")));

    // Violation
    assertEquals(1, constraintResult.violations().size());
    final var violation = constraintResult.violations().get(0);
    assertEquals(1, violation.windows().size());

    final long activityOffset = 60 * 60 * 1000000L; // 1h in micros
    final long planDuration = 1212 * 60 * 60 * 1000000L; // 1212h in micros

    assertEquals(activityOffset, violation.windows().get(0).start());
    assertEquals(planDuration, violation.windows().get(0).end());
    // Gaps
    assertTrue(constraintResult.gaps().isEmpty());
  }

  @Test
  void constraintsSucceedAgainstVariantWithUnits() throws IOException {
    hasura.deleteActivity(planId, activityId);
    hasura.deleteConstraint(constraintId);
    var fruitConstraintName = "fruit_equals_3";
    var fruitConstraintId = hasura.insertPlanConstraint(
        fruitConstraintName,
        planId,
        "export default (): Constraint => Real.Resource(\"/fruit\").equal(3)",
        "");
    hasura.insertActivity(
        planId,
        "PeelBanana",
        "1h",
        Json.createObjectBuilder().add("peelDirection", "fromStem").build());

    hasura.awaitSimulation(planId);
    final var constraintsResponses = hasura.checkConstraints(planId);
    assertEquals(1, constraintsResponses.size());

    // Check the Response
    final var constraintResponse = constraintsResponses.get(0);
    assertTrue(constraintResponse.success());
    assertEquals(fruitConstraintId, constraintResponse.constraintId());
    assertEquals(fruitConstraintName, constraintResponse.constraintName());
    assertEquals("plan", constraintResponse.type());
    // Check the Result
    assertTrue(constraintResponse.result().isPresent());
    final var constraintResult = constraintResponse.result().get();
    // Resources
    final var resources = constraintResult.resourceIds();
    assertEquals(1, resources.size());
    assertTrue(resources.contains("/fruit"));

    // Violation
    assertEquals(1, constraintResult.violations().size());
    final var violation = constraintResult.violations().get(0);
    assertEquals(1, violation.windows().size());

    final long activityOffset = 60 * 60 * 1000000L; // 1h in micros

    assertEquals(0, violation.windows().get(0).start());
    assertEquals(activityOffset, violation.windows().get(0).end());
    // Gaps
    assertTrue(constraintResult.gaps().isEmpty());
  }

  @Test
  void constraintsSucceedNoViolations() throws IOException {
    // Delete activity to avoid violation
    hasura.deleteActivity(planId, activityId);
    hasura.awaitSimulation(planId);
    final var constraintResponses = hasura.checkConstraints(planId);

    assertEquals(1, constraintResponses.size());
    assertTrue(constraintResponses.get(0).success());
    assertEquals(constraintId, constraintResponses.get(0).constraintId());
    assertEquals(constraintName, constraintResponses.get(0).constraintName());
    assertEquals("plan", constraintResponses.get(0).type());
    assertTrue( constraintResponses.get(0).result().isPresent());
    assertEquals(0, constraintResponses.get(0).result().get().violations().size());
  }

  @Test
  void constraintCachedViolation() throws IOException {
    final var simDatasetId = hasura.awaitSimulation(planId).simDatasetId();
    final var constraintResponses = hasura.checkConstraints(planId);
    final var cachedRuns = hasura.getConstraintRuns(simDatasetId);

    // There's the correct number of results
    assertEquals(1, cachedRuns.size());
    assertEquals(1, constraintResponses.size());
    assertEquals(cachedRuns.get(0).constraintId(),constraintId);

    // Check properties
    final var cachedRun = cachedRuns.get(0);
    assertFalse(cachedRun.definitionOutdated());
    assertEquals(constraintId, cachedRun.constraintId());
    assertEquals(simDatasetId, cachedRun.simDatasetId());
    assertEquals(constraintDefinition, cachedRun.constraintDefinition());

    // Check results
    assertTrue(cachedRun.results().isPresent());
    final var constraintResponse = constraintResponses.get(0);
    assertTrue(constraintResponse.success());
    assertTrue(constraintResponse.result().isPresent());
    assertEquals(constraintResponse.result().get(), cachedRun.results().get());
  }

  @Test
  void constraintCachedNoViolations() throws IOException {
    // Delete activity to avoid violation
    hasura.deleteActivity(planId, activityId);
    final var simDatasetId = hasura.awaitSimulation(planId).simDatasetId();
    hasura.checkConstraints(planId);
    final var cachedRuns = hasura.getConstraintRuns(simDatasetId);

    // There's the correct number of results
    assertEquals(1, cachedRuns.size());

    // Check properties
    final var cachedRun = cachedRuns.get(0);
    assertFalse(cachedRun.definitionOutdated());
    assertEquals(constraintId, cachedRun.constraintId());
    assertEquals(simDatasetId, cachedRun.simDatasetId());
    assertEquals(constraintDefinition, cachedRun.constraintDefinition());

    // Check results
    assertTrue(cachedRun.results().isPresent());
    final var results = cachedRun.results().get();
    assertEquals(2,results.resourceIds().size());
    assertEquals("/peel",results.resourceIds().get(0));
    assertEquals("/fruit",results.resourceIds().get(1));
    assertEquals(0,results.violations().size());
    assertEquals(0,results.gaps().size());
  }

  @Test
  void constraintCacheInvalidatedDefinition() throws IOException {
    final int simDatasetId = hasura.awaitSimulation(planId).simDatasetId();
    hasura.checkConstraints(planId);

    // Updating the constraint definition should mark the constraint run as invalid
    final String newDefinition =
        "export default (): Constraint => Real.Resource(\"/peel\").equal(Real.Resource(\"/fruit\"))";
    hasura.updateConstraint(constraintId, newDefinition);

    final var cachedRuns = hasura.getConstraintRuns(simDatasetId);
    assertEquals(1, cachedRuns.size());
    assertTrue(cachedRuns.get(0).definitionOutdated());
  }

  /**
   * Test that an activity with a duration longer than one month is written to and read back from the database
   * successfully
   * by Aerie's simulation and constraints checking components respectively. The driving concern here is that Aerie
   * needs
   * to interpret span durations as microseconds; if the simulation results were to be written using the postgres
   * interval's "months" field, constraints checking would fail to load these values back from the database.
   */
  @Test
  void constraintsWorkMonthLongActivity() throws IOException {
    // Setup
    hasura.updateConstraint(
        constraintId,
        "export default (): Constraint => Windows.During(ActivityType.ControllableDurationActivity).not()");
    hasura.deleteActivity(planId, activityId);
    final long thirtyFiveDays = 35 * 24 * 60 * 60 * 1000L * 1000; // 35 days in microseconds
    hasura.insertActivity(
        planId,
        "ControllableDurationActivity",
        "0h",
        Json.createObjectBuilder().add("duration", thirtyFiveDays).build());
    hasura.awaitSimulation(planId);
    final var constraintsResponses = hasura.checkConstraints(planId);
    assertEquals(1, constraintsResponses.size());

    // Check the Response
    final var constraintResponse = constraintsResponses.get(0);
    assertTrue(constraintResponse.success());
    assertEquals(constraintId,constraintResponse.constraintId());
    assertEquals(constraintName, constraintResponse.constraintName());
    assertEquals("plan", constraintResponse.type());

    //Check Result
    assertTrue(constraintResponse.result().isPresent());
    final var constraintResult = constraintResponse.result().get();

    // Resources
    final var resources = constraintResult.resourceIds();
    assertEquals(0, resources.size());

    // Violation
    assertEquals(1, constraintResult.violations().size());
    final var violation = constraintResult.violations().get(0);
    assertEquals(1, violation.windows().size());

    assertEquals(0, violation.windows().get(0).start());
    assertEquals(thirtyFiveDays, violation.windows().get(0).end());
    // Gaps
    assertTrue(constraintResult.gaps().isEmpty());
  }

  @Test
  void runConstraintsOnOldSimulation() throws IOException {
    final int oldSimDatasetId = hasura.awaitSimulation(planId).simDatasetId();

    // Delete Activity to make the simulation outdated, then resim
    hasura.deleteActivity(planId, activityId);
    final int newSimDatasetId = hasura.awaitSimulation(planId).simDatasetId();

    // Expect no violations on the new simulation
    final var newConstraintResponses = hasura.checkConstraints(planId, newSimDatasetId);
    assertEquals(1, newConstraintResponses.size());
    assertEquals(constraintId, newConstraintResponses.get(0).constraintId());
    assertEquals(constraintName, newConstraintResponses.get(0).constraintName());
    assertEquals("plan", newConstraintResponses.get(0).type());
    assertTrue(newConstraintResponses.get(0).result().isPresent());
    final var newConstraintResult = newConstraintResponses.get(0).result().get();
    assertTrue(newConstraintResult.violations().isEmpty());


    // Expect one violation on the old simulation
    final var oldConstraintsResponses = hasura.checkConstraints(planId, oldSimDatasetId);
    assertEquals(1, oldConstraintsResponses.size());

    // Check the Result
    final var oldConstraintResponse = oldConstraintsResponses.get(0);
    assertTrue(oldConstraintResponse.success());
    assertEquals(constraintId, oldConstraintResponse.constraintId());
    assertEquals(constraintName, oldConstraintResponse.constraintName());
    assertEquals("plan", oldConstraintResponse.type());
    assertTrue(oldConstraintResponse.result().isPresent());
    final var constraintResult = oldConstraintResponse.result().get();

    // Resources
    final var resources = constraintResult.resourceIds();
    assertEquals(2, resources.size());
    assertTrue(resources.containsAll(List.of("/peel", "/fruit")));

    // Violation
    assertEquals(1, constraintResult.violations().size());
    final var violation = constraintResult.violations().get(0);
    assertEquals(1, violation.windows().size());

    final long activityOffset = 60 * 60 * 1000000L; // 1h in micros
    final long planDuration = 1212 * 60 * 60 * 1000000L; // 1212h in micros

    assertEquals(activityOffset, violation.windows().get(0).start());
    assertEquals(planDuration, violation.windows().get(0).end());
    // Gaps
    assertTrue(constraintResult.gaps().isEmpty());
  }

  @Test
  void cachedRunsNotOutdatedOnResim() throws IOException {
    final int oldSimDatasetId = hasura.awaitSimulation(planId).simDatasetId();
    hasura.checkConstraints(planId);

    // Delete Activity to make the simulation outdated, then resim
    hasura.deleteActivity(planId, activityId);
    hasura.awaitSimulation(planId);

    // Get the old run
    final var cachedRuns = hasura.getConstraintRuns(oldSimDatasetId);
    assertEquals(1, cachedRuns.size());
    assertFalse(cachedRuns.get(0).definitionOutdated());
  }

  @Nested
  class WithExternalDatasets {
    // Per-Test Data
    private int simDatasetId;
    private int externalDatasetId;
    // Cross-Test Constants
    private final ProfileInput myBooleanProfile = new ProfileInput(
        "/my_boolean",
        "discrete",
        ValueSchema.VALUE_SCHEMA_BOOLEAN,
        List.of(
            new ProfileSegmentInput(3600000000L, JsonValue.TRUE), // 1hr in micros
            new ProfileSegmentInput(3600000000L, JsonValue.FALSE)));

    public static final String constraintDefinition = "export default (): Constraint => Discrete.Resource(\"/my_boolean\").equal(true)";

    @BeforeEach
    void beforeEach() throws IOException {
      // Change Constraint to be about MyBoolean
      hasura.updateConstraint(
          constraintId,
          constraintDefinition);
      // Simulate Plan
      simDatasetId = hasura.awaitSimulation(planId).simDatasetId();
      // Add Simulation-Associated External Dataset
      final String externalDatasetStartTimestamp = "2021-001T06:00:00.000";
      externalDatasetId = hasura.insertExternalDataset(
          planId,
          simDatasetId,
          externalDatasetStartTimestamp,
          List.of(myBooleanProfile));
    }

    @AfterEach
    void afterEach() throws IOException {
      // Remove External Dataset
      hasura.deleteExternalDataset(planId, externalDatasetId);
    }

    @Test
    @DisplayName("Simulation-Associated External Datasets are loaded when Simulation is current")
    void oneViolationCurrentSimulation() throws IOException {
      // Constraint Results w/o SimDatasetId
      final var noDatasetResponses = hasura.checkConstraints(planId);
      assertEquals(1, noDatasetResponses.size());
      assertTrue(noDatasetResponses.get(0).success());
      assertEquals(constraintId, noDatasetResponses.get(0).constraintId());
      assertEquals(constraintName, noDatasetResponses.get(0).constraintName());
      assertEquals("plan", noDatasetResponses.get(0).type());
      assertTrue(noDatasetResponses.get(0).result().isPresent());
      final var nRecordResults = noDatasetResponses.get(0).result().get();

      // Constraint Results w/ SimDatasetId
      final var withDatasetResponses = hasura.checkConstraints(planId, simDatasetId);
      assertEquals(1, withDatasetResponses.size());
      assertTrue(withDatasetResponses.get(0).success());
      assertEquals(constraintId, withDatasetResponses.get(0).constraintId());
      assertEquals(constraintName, withDatasetResponses.get(0).constraintName());
      assertEquals("plan", withDatasetResponses.get(0).type());
      assertTrue(withDatasetResponses.get(0).result().isPresent());
      final var wRecordResults = withDatasetResponses.get(0).result().get();

      // The results should be the same
      assertEquals(nRecordResults, wRecordResults);


      // Resources
      assertEquals(1, nRecordResults.resourceIds().size());
      assertTrue(nRecordResults.resourceIds().contains("/my_boolean"));

      // Violation
      assertEquals(1, nRecordResults.violations().size());
      final var violation = nRecordResults.violations().get(0);
      assertEquals(1, violation.windows().size());

      final long falseBeginMicros = 7 * 60 * 60 * 1000000L; // 7h in micros
      final long datasetEndMicros = 8 * 60 * 60 * 1000000L; // 8h in micros

      assertEquals(falseBeginMicros, violation.windows().get(0).start());
      assertEquals(datasetEndMicros, violation.windows().get(0).end());

      // Gaps
      // Two are expected: 1 from plan start to the external dataset start
      // and 1 from the external dataset end to plan end
      assertEquals(2, nRecordResults.gaps().size());
      final long datasetOffsetMicros = 6 * 60 * 60 * 1000000L; // 6h in micros
      final long planDuration = 1212 * 60 * 60 * 1000000L; // 1212h in micros

      final var firstGap = nRecordResults.gaps().get(0);
      assertEquals(0, firstGap.start());
      assertEquals(datasetOffsetMicros, firstGap.end());

      final var secondGap = nRecordResults.gaps().get(1);
      assertEquals(datasetEndMicros, secondGap.start());
      assertEquals(planDuration, secondGap.end());
    }

    @Test
    @DisplayName("Outdated Simulation-Associated External Datasets are loaded when Outdated Simulation is passed")
    void oneViolationOutdatedSimIdPassed() throws IOException {
      // Delete Activity to make the simulation outdated, then resim
      hasura.deleteActivity(planId, activityId);
      hasura.awaitSimulation(planId);
      // Check constraints against the old simID (the one with the external dataset)
      final var constraintResponses = hasura.checkConstraints(planId, simDatasetId);
      assertEquals(1, constraintResponses.size());

      final var constraintResponse = constraintResponses.get(0);
      assertTrue(constraintResponse.success());
      assertEquals(constraintId, constraintResponse.constraintId());
      assertEquals(constraintName, constraintResponse.constraintName());
      assertEquals("plan", constraintResponse.type());
      assertTrue(constraintResponse.result().isPresent());
      final var record = constraintResponse.result().get();

      // Resources
      assertEquals(1, record.resourceIds().size());
      assertTrue(record.resourceIds().contains("/my_boolean"));

      // Violation
      assertEquals(1, record.violations().size());
      final var violation = record.violations().get(0);
      assertEquals(1, violation.windows().size());

      final long falseBeginMicros = 7 * 60 * 60 * 1000000L; // 7h in micros
      final long datasetEndMicros = 8 * 60 * 60 * 1000000L; // 8h in micros

      assertEquals(falseBeginMicros, violation.windows().get(0).start());
      assertEquals(datasetEndMicros, violation.windows().get(0).end());

      // Gaps
      // Two are expected: 1 from plan start to the external dataset start
      // and 1 from the external dataset end to plan end
      assertEquals(2, record.gaps().size());
      final long datasetOffsetMicros = 6 * 60 * 60 * 1000000L; // 6h in micros
      final long planDuration = 1212 * 60 * 60 * 1000000L; // 1212h in micros

      final var firstGap = record.gaps().get(0);
      assertEquals(0, firstGap.start());
      assertEquals(datasetOffsetMicros, firstGap.end());

      final var secondGap = record.gaps().get(1);
      assertEquals(datasetEndMicros, secondGap.start());
      assertEquals(planDuration, secondGap.end());
    }

    @Test
    @DisplayName("Simulation-Associated External Datasets aren't loaded when other SimIds are passed")
    void compilationFailsOutdatedSimulationSimDatasetId() throws IOException {
      // Delete Activity to make the simulation outdated, then resim
      hasura.deleteActivity(planId, activityId);
      final int newSimDatasetId = hasura.awaitSimulation(planId).simDatasetId();
      // This test causes the endpoint to throw an exception when it fails to compile the constraint,
      // as it cannot find the external dataset resource in the set of known resource types.
      final var constraintResponses = hasura.checkConstraints(planId, newSimDatasetId);
      assertEquals(1,constraintResponses.size());
      final var constraintResponse = constraintResponses.get(0);
      assertEquals(constraintId, constraintResponse.constraintId());
      assertEquals(constraintName, constraintResponse.constraintName());
      assertEquals("plan", constraintResponse.type());
      assertFalse(constraintResponse.success());
      assertTrue(constraintResponse.result().isEmpty());
      assertEquals(1,constraintResponse.errors().size());
      final var error = constraintResponse.errors().get(0);
      assertEquals("Constraint 'fruit_equal_peel' compilation failed:\n"
                   + " TypeError: TS2345 Argument of type '\"/my_boolean\"' is not assignable to parameter of type 'ResourceName'.",error.message());
    }

    @Test
    @DisplayName("Outdated Simulation-Associated External Datasets aren't loaded")
    void compilationFailsOutdatedSimulationNoSimDataset() throws IOException {
      // Delete Activity to make the simulation outdated, then resim
      hasura.deleteActivity(planId, activityId);
      hasura.awaitSimulation(planId);

      // The constraint run is expected to fail with the following message because the constraint
      // DSL isn't being generated for datasets that are outdated.
      final var constraintResponses = hasura.checkConstraints(planId);
      assertEquals(1,constraintResponses.size());
      final var constraintResponse = constraintResponses.get(0);
      assertFalse(constraintResponse.success());
      assertEquals(constraintId, constraintResponse.constraintId());
      assertEquals(constraintName, constraintResponse.constraintName());
      assertEquals("plan", constraintResponse.type());
      assertTrue(constraintResponse.result().isEmpty());
      assertEquals(1,constraintResponse.errors().size());
      final ConstraintError error = constraintResponse.errors().get(0);
      assertEquals("Constraint 'fruit_equal_peel' compilation failed:\n"
                   + " TypeError: TS2345 Argument of type '\"/my_boolean\"' is not assignable to parameter of type 'ResourceName'.",error.message());

    }

    /**
     *  This test case evaluates the scenario where a user initially has a functioning constraint.
     *  However, during the modification process, they introduce a compilation error.
     *  The goal is to confirm that the user can successfully fix the compilation error,
     *  ensuring that the constraint compiles correctly and returns a valid response.
     */
    @Test
    @DisplayName("If a constraint is updated and then reverted, Constraints will load the cached value from before the update.")
    void constraintInvalidModification() throws IOException {
      // Simulate the plan to insure a new simulation set
      hasura.awaitSimulation(planId);

      // Check the initial constraints responses it should compile
      final var constraintsResponses = hasura.checkConstraints(planId);
      assertEquals(1, constraintsResponses.size());
      assertTrue(constraintsResponses.get(0).success());
      assertEquals(constraintId, constraintsResponses.get(0).constraintId());
      assertEquals(constraintName, constraintsResponses.get(0).constraintName());
      assertEquals("plan", constraintsResponses.get(0).type());
      // Attempt to update a constraint with an invalid syntax and capture the error response
      hasura.updateConstraint(
          constraintId,
          WithExternalDatasets.constraintDefinition + "; error");

      // Check the constraints response after the invalid modification
      final var constraintsErrorResponses = hasura.checkConstraints(planId);
      assertEquals(1, constraintsErrorResponses.size());
      assertFalse(constraintsErrorResponses.get(0).success());
      assertEquals(constraintId, constraintsErrorResponses.get(0).constraintId());
      assertEquals(constraintName, constraintsErrorResponses.get(0).constraintName());
      assertEquals("plan", constraintsErrorResponses.get(0).type());
      assertTrue(constraintsErrorResponses.get(0).result().isEmpty());
      assertEquals(1, constraintsErrorResponses.get(0).errors().size());

      // Restore the original valid constraint
      hasura.updateConstraint(
          constraintId,
          WithExternalDatasets.constraintDefinition);

      // Check the constraints response after reverting to the valid constraint
      final var constraintsCacheResponses = hasura.checkConstraints(planId);
      assertEquals(1, constraintsCacheResponses.size());
      assertTrue(constraintsCacheResponses.get(0).success());

      // Ensure that the constraints responses are the same before and after the invalid modification
      assertEquals(constraintsResponses, constraintsCacheResponses);
    }

  }
}
