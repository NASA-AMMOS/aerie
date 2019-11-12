package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import static org.assertj.core.api.Assertions.assertThat;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks.StubPlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.utils.HttpRequester;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.JsonbBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlanBindingsTest {
  private static Javalin app = null;

  @BeforeAll
  public static void setupServer() {
    final StubPlanController controller = new StubPlanController();
    final PlanBindings bindings = new PlanBindings(controller);

    app = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config.enableCorsForAllOrigins();
    });
    bindings.registerRoutes(app);
    app.start();
  }

  @AfterAll
  public static void shutdownServer() {
    app.stop();
  }

  private final URI baseUri = URI.create("http://localhost:" + app.port());
  private final HttpClient rawHttpClient = HttpClient.newHttpClient();
  private final HttpRequester client = new HttpRequester(rawHttpClient, baseUri);

  @Test
  public void shouldEnableCors() throws IOException, InterruptedException {
    // GIVEN
    final String origin = "http://localhost";

    // WHEN
    final HttpRequest request = HttpRequest.newBuilder()
        .uri(baseUri.resolve("/plans"))
        .header("Origin", origin)
        .GET()
        .build();

    final HttpResponse<String> response = rawHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

    // THEN
    assertThat(response.headers().allValues("Access-Control-Allow-Origin")).isNotEmpty();
  }

  @Test
  public void shouldGetPlans() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final Plan plan = StubPlanController.EXISTENT_PLAN;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans");

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    // TODO: Verify the structure of the response entity.
  }

  @Test
  public void shouldGetPlanById() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    // TODO: Verify the structure of the response entity.
  }

  @Test
  public void shouldReturn404OnNonexistentPlanById() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldAddValidPlan() throws IOException, InterruptedException {
    // GIVEN
    final NewPlan plan = StubPlanController.VALID_NEW_PLAN;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans", plan);

    // THEN
    final String expectedPlanId = StubPlanController.EXISTENT_PLAN_ID;

    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(response.headers().firstValue("Location")).get().isEqualTo("/plans/" + expectedPlanId);

    final JsonObject responseBody = JsonbBuilder.create().fromJson(response.body(), JsonObject.class);
    assertThat(responseBody).containsKey("id");
  }

  @Test
  public void shouldNotAddInvalidPlan() throws IOException, InterruptedException {
    // GIVEN
    final NewPlan plan = StubPlanController.INVALID_NEW_PLAN;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans", plan);

    // THEN
    assertThat(response.statusCode()).isEqualTo(400);

    // TODO: Verify the structure of the error response entity.
  }

  @Test
  public void shouldReplaceExistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final NewPlan plan = StubPlanController.VALID_NEW_PLAN;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId, plan);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotReplaceNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;
    final NewPlan plan = StubPlanController.VALID_NEW_PLAN;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId, plan);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotReplaceInvalidPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final NewPlan plan = StubPlanController.INVALID_NEW_PLAN;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId, plan);

    // THEN
    assertThat(response.statusCode()).isEqualTo(400);

    // TODO: Verify the structure of the error response entity.
  }

  @Test
  public void shouldPatchExistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final Plan patch = StubPlanController.VALID_PATCH;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotPatchNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;
    final Plan patch = StubPlanController.VALID_PATCH;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotPatchInvalidPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final Plan patch = StubPlanController.INVALID_PATCH;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(422);

    final JsonValue responseJson = JsonbBuilder.create().fromJson(response.body(), JsonValue.class);
    final JsonValue expectedJson = ResponseSerializers.serializeValidationMessages(StubPlanController.VALIDATION_ERRORS);
    assertThat(responseJson).isEqualTo(expectedJson);
  }

  @Test
  public void shouldRemoveExistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotRemoveNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldGetActivityInstances() throws IOException, InterruptedException, InvalidEntityException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityId = StubPlanController.EXISTENT_ACTIVITY_ID;
    final ActivityInstance activity = StubPlanController.EXISTENT_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances");

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final var responseJson = Json.createReader(new StringReader(response.body())).readValue();
    final Map<String, ActivityInstance> activities = RequestDeserializers.deserializeActivityInstanceMap(responseJson);

    assertThat(activities).containsEntry(activityId, activity);
  }

  @Test
  public void shouldNotGetActivityInstancesForNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances");

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldGetActivityInstanceById() throws IOException, InterruptedException, InvalidEntityException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;
    final ActivityInstance expectedActivityInstance = StubPlanController.EXISTENT_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final var responseJson = Json.createReader(new StringReader(response.body())).readValue();
    final ActivityInstance activityInstance = RequestDeserializers.deserializeActivityInstance(responseJson);
    assertThat(activityInstance).isEqualTo(expectedActivityInstance);
  }

  @Test
  public void shouldNotGetActivityInstanceFromNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotGetNonexistentActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.NONEXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldAddActivityInstancesToPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final ActivityInstance activityInstance = StubPlanController.VALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans/" + planId + "/activity_instances", List.of(activityInstance));

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    // TODO: Verify the structure of the error response entity.
  }

  @Test
  public void shouldNotAddActivityInstancesToNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;
    final ActivityInstance activityInstance = StubPlanController.VALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans/" + planId + "/activity_instances", List.of(activityInstance));

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotAddInvalidActivityInstancesToPlan() throws IOException, InterruptedException, InvalidEntityException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final ActivityInstance activityInstance = StubPlanController.INVALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans/" + planId + "/activity_instances", List.of(activityInstance));

    // THEN
    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  public void shouldDeleteActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotDeleteActivityInstanceFromNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotDeleteNonexistentActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.NONEXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldPatchActivityInstanceById() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;
    final ActivityInstance patch = StubPlanController.VALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId + "/activity_instances/" + activityInstanceId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotPatchActivityInstanceInNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;
    final ActivityInstance patch = StubPlanController.VALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId + "/activity_instances/" + activityInstanceId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotPatchNonexistentActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.NONEXISTENT_ACTIVITY_ID;
    final ActivityInstance patch = StubPlanController.VALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId + "/activity_instances/" + activityInstanceId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotPatchInvalidActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;
    final ActivityInstance patch = StubPlanController.INVALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId + "/activity_instances/" + activityInstanceId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(422);

    final JsonValue responseJson = JsonbBuilder.create().fromJson(response.body(), JsonValue.class);
    final JsonValue expectedJson = ResponseSerializers.serializeValidationMessages(StubPlanController.VALIDATION_ERRORS);
    assertThat(responseJson).isEqualTo(expectedJson);
  }

  @Test
  public void shouldReplaceActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;
    final ActivityInstance activityInstance = StubPlanController.VALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId + "/activity_instances/" + activityInstanceId, activityInstance);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotReplaceActivityInstanceOfNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;
    final ActivityInstance activityInstance = StubPlanController.VALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId + "/activity_instances/" + activityInstanceId, activityInstance);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotReplaceNonexistentActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.NONEXISTENT_ACTIVITY_ID;
    final ActivityInstance activityInstance = StubPlanController.VALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId + "/activity_instances/" + activityInstanceId, activityInstance);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotReplaceInvalidActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanController.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanController.EXISTENT_ACTIVITY_ID;
    final ActivityInstance activityInstance = StubPlanController.INVALID_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId + "/activity_instances/" + activityInstanceId, activityInstance);

    // THEN
    assertThat(response.statusCode()).isEqualTo(422);

    final JsonValue responseJson = JsonbBuilder.create().fromJson(response.body(), JsonValue.class);
    final JsonValue expectedJson = ResponseSerializers.serializeValidationMessages(StubPlanController.VALIDATION_ERRORS);
    assertThat(responseJson).isEqualTo(expectedJson);
  }
}
