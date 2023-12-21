package gov.nasa.jpl.aerie.e2e;

import com.microsoft.playwright.Playwright;
import gov.nasa.jpl.aerie.e2e.types.SimulationDataset.SimulatedActivity;
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
import java.io.IOException;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SimulationTests {
  // Requests
  private Playwright playwright;
  private HasuraRequests hasura;

  // Per-Test Data
  private int modelId;
  private int planId;

  // Cross-Test Constants
  private final String planStartTimestamp = "2023-01-01T00:00:00+00:00";
  private final String  midwayPlanTimestamp = "2023-01-01T12:00:00+00:00";
  private final String  planEndTimestamp = "2023-01-02T00:00:00+00:00";

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
          "Simulation Tests");
    }
    // Insert the Plan
    planId = hasura.createPlan(
        modelId,
        "Test Plan - Simulation Tests",
        "24:00:00",
        planStartTimestamp);
  }

  @AfterEach
  void afterEach() throws IOException {
    // Remove Model and Plan
    hasura.deletePlan(planId);
    hasura.deleteMissionModel(modelId);
  }

  /**
   * GrowBanana with duration of 1 microsecond should finish with a successful simulation
   */
  @Test
  void simulationMicrosecondResolution() throws IOException {
    final var activityArgs = Json.createObjectBuilder().add("duration", 1).build();
    hasura.insertActivity(planId, "ControllableDurationActivity", "1h", activityArgs);
    assertDoesNotThrow(() -> hasura.awaitSimulation(planId));
  }

  @Nested
  class TemporalSubsetSimulation {
    private int firstHalfActivityId;
    private int midpointActivityId;
    private int secondHalfActivityId;

    private final String firstHalfActivityStartTime = "2023-01-01T01:00:00+00:00";
    private final String midpointActivityStartTime = midwayPlanTimestamp;
    private final String secondHalfActivityStartTime = "2023-01-01T14:00:00+00:00";

    @BeforeEach
    void beforeEach() throws IOException {
      // Insert Activities
      firstHalfActivityId = hasura.insertActivity(
          planId,
          "ControllableDurationActivity",
          "1hr",
          Json.createObjectBuilder().add("duration", 3600000000L).build()); //1hr duration
      midpointActivityId = hasura.insertActivity(
          planId,
          "ControllableDurationActivity",
          "12hr",
          Json.createObjectBuilder().add("duration", 7200000000L).build()); //2hr duration
      secondHalfActivityId = hasura.insertActivity(
          planId,
          "ControllableDurationActivity",
          "14hr",
          Json.createObjectBuilder().add("duration", 10800000000L).build()); //3hr duration
    }

    // No AfterEach in the nested class, as SimulationTests' AfterEach will perform the necessary cleanup

    /**
     * Expect the first half of the plan to be simulated
     */
    @Test
    void simulateFirstHalf() throws IOException {
      hasura.updateSimBounds(planId, planStartTimestamp, midwayPlanTimestamp);

      // Assert SimDataset Properties
      final var simDataset = hasura.getSimulationDataset(hasura.awaitSimulation(planId).simDatasetId());
      assertFalse(simDataset.canceled());
      assertEquals(planStartTimestamp, simDataset.simStartTime());
      assertEquals(midwayPlanTimestamp, simDataset.simEndTime());
      assertEquals(2, simDataset.activities().size());

      // Sort to be certain of the order
      simDataset.activities().sort(Comparator.comparingInt(SimulatedActivity::directiveId));

      // Assert Activities
      final var firstActivity = simDataset.activities().get(0);
      assertEquals(firstHalfActivityId, firstActivity.directiveId());
      assertEquals("01:00:00", firstActivity.duration());
      assertEquals("01:00:00", firstActivity.startOffset());
      assertEquals(firstHalfActivityStartTime, firstActivity.startTime());

      final var unfinishedActivity = simDataset.activities().get(1);
      assertEquals(midpointActivityId, unfinishedActivity.directiveId());
      assertNull(unfinishedActivity.duration());
      assertEquals("12:00:00", unfinishedActivity.startOffset());
      assertEquals(midpointActivityStartTime, unfinishedActivity.startTime());
    }

    /**
     * Expect the second half of the plan to be simulated
     */
    @Test
    void simulateSecondHalf() throws IOException {
      hasura.updateSimBounds(planId, midwayPlanTimestamp, planEndTimestamp);

      // Assert SimDataset Properties
      final var simDataset = hasura.getSimulationDataset(hasura.awaitSimulation(planId).simDatasetId());
      assertFalse(simDataset.canceled());
      assertEquals(midwayPlanTimestamp, simDataset.simStartTime());
      assertEquals(planEndTimestamp, simDataset.simEndTime());
      assertEquals(2, simDataset.activities().size());

      // Sort to be certain of the order
      simDataset.activities().sort(Comparator.comparingInt(SimulatedActivity::directiveId));

      // Assert Activities
      final var firstActivity = simDataset.activities().get(0);
      assertEquals(midpointActivityId, firstActivity.directiveId());
      assertEquals("02:00:00", firstActivity.duration());
      assertEquals("00:00:00", firstActivity.startOffset());
      assertEquals(midpointActivityStartTime, firstActivity.startTime());

      final var secondActivity = simDataset.activities().get(1);
      assertEquals(secondHalfActivityId, secondActivity.directiveId());
      assertEquals("03:00:00", secondActivity.duration());
      assertEquals("02:00:00", secondActivity.startOffset());
      assertEquals(secondHalfActivityStartTime, secondActivity.startTime());
    }

    /**
     * In this test:
     *  - Simulation's simulation_start_time: (January 1, 2023, at 02:00:00 Z)
     *  - Simulation's simulation_end_time: (January 1, 2023, at 13:00:00 Z)
     * Expect the simulation to be 11 Hours long (02:00:00 Z to 13:00:00 Z)
    */
    @Test
    void simulateMiddle() throws IOException {
      hasura.updateSimBounds(planId, "2023-01-01T02:00:00+00:00", "2023-01-01T13:00:00+00:00");

      // Assert SimDataset Properties
      final var simDataset = hasura.getSimulationDataset(hasura.awaitSimulation(planId).simDatasetId());
      assertFalse(simDataset.canceled());
      assertEquals("2023-01-01T02:00:00+00:00", simDataset.simStartTime());
      assertEquals("2023-01-01T13:00:00+00:00", simDataset.simEndTime());
      assertEquals(1, simDataset.activities().size());

      // Assert Activities
      final var unfinishedActivity = simDataset.activities().get(0);
      assertEquals(midpointActivityId, unfinishedActivity.directiveId());
      assertNull(unfinishedActivity.duration());
      assertEquals("10:00:00", unfinishedActivity.startOffset());
      assertEquals(midpointActivityStartTime, unfinishedActivity.startTime());
    }

    /**
     * Expect the entire plan to be simulated
     */
    @Test
    void simulateCompletePlan() throws IOException {
      hasura.updateSimBounds(planId, planStartTimestamp, planEndTimestamp);

      // Assert SimDataset Properties
      final var simDataset = hasura.getSimulationDataset(hasura.awaitSimulation(planId).simDatasetId());
      assertFalse(simDataset.canceled());
      assertEquals(planStartTimestamp, simDataset.simStartTime());
      assertEquals(planEndTimestamp, simDataset.simEndTime());
      assertEquals(3, simDataset.activities().size());

      // Sort to be certain of the order
      simDataset.activities().sort(Comparator.comparingInt(SimulatedActivity::directiveId));

      // Assert Activities
      final var firstActivity = simDataset.activities().get(0);
      assertEquals(firstHalfActivityId, firstActivity.directiveId());
      assertEquals("01:00:00", firstActivity.duration());
      assertEquals("01:00:00", firstActivity.startOffset());
      assertEquals(firstHalfActivityStartTime, firstActivity.startTime());

      final var secondActivity = simDataset.activities().get(1);
      assertEquals(midpointActivityId, secondActivity.directiveId());
      assertEquals("02:00:00", secondActivity.duration());
      assertEquals("12:00:00", secondActivity.startOffset());
      assertEquals(midpointActivityStartTime, secondActivity.startTime());

      final var thirdActivity = simDataset.activities().get(2);
      assertEquals(secondHalfActivityId, thirdActivity.directiveId());
      assertEquals("03:00:00", thirdActivity.duration());
      assertEquals("14:00:00", thirdActivity.startOffset());
      assertEquals(secondHalfActivityStartTime, thirdActivity.startTime());
    }
  }
}
