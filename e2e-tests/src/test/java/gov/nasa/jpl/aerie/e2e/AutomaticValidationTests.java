package gov.nasa.jpl.aerie.e2e;

import com.microsoft.playwright.Playwright;
import gov.nasa.jpl.aerie.e2e.types.ActivityValidation;
import gov.nasa.jpl.aerie.e2e.utils.GatewayRequests;
import gov.nasa.jpl.aerie.e2e.utils.HasuraRequests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.json.Json;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AutomaticValidationTests {
  // Requests
  private Playwright playwright;
  private HasuraRequests hasura;

  // Per-Test Data
  private int modelId;
  private int planId;

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
        "Test Plan - Automatic Validation Tests",
        "1212h",
        "2021-01-01T00:00:00Z");
  }

  @AfterEach
  void afterEach() throws IOException {
    // Remove Model, Plan, and Constraint
    hasura.deletePlan(planId);
    hasura.deleteMissionModel(modelId);
  }

  @Test
  void validationSuccess() throws IOException, InterruptedException {
    final var activityId = hasura.insertActivity(
        planId,
        "BiteBanana",
        "1h",
        Json.createObjectBuilder().add("biteSize", 1).build());
    Thread.sleep(1000); // TODO consider a while loop here
    final var activityValidations = hasura.getActivityValidations(planId);
    final ActivityValidation activityValidation = activityValidations.get((long) activityId);
    assertEquals(new ActivityValidation.Success(), activityValidation);
  }

  @Test
  void exceptionDuringValidationHandled() throws IOException, InterruptedException {
    final var exceptionActivityId = hasura.insertActivity(
        planId,
        "ExceptionActivity",
        "1h",
        Json.createObjectBuilder().add("throwException", true).build());

    // sleep to make sure exception activity is picked up
    Thread.sleep(1000); // TODO consider a while loop here

    final var biteActivityId = hasura.insertActivity(
        planId,
        "BiteBanana",
        "1h",
        Json.createObjectBuilder().add("biteSize", 1).build());

    Thread.sleep(1000); // TODO consider a while loop here

    final var activityValidations = hasura.getActivityValidations(planId);

    final ActivityValidation exceptionValidations = activityValidations.get((long) exceptionActivityId);
    final ActivityValidation biteValidations = activityValidations.get((long) biteActivityId);

    // then make sure the exception was caught and serialized, and didn't crash the worker thread
    assertEquals(new ActivityValidation.RuntimeError("Throwing runtime exception during validation"), exceptionValidations);
    // if the above is true, bite banana will have its validation still
    assertEquals(new ActivityValidation.Success(), biteValidations);
  }
}
