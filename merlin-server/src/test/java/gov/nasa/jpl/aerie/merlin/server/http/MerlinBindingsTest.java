package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.server.mocks.FakeFile;
import gov.nasa.jpl.aerie.merlin.server.mocks.StubAdaptationService;
import gov.nasa.jpl.aerie.merlin.server.mocks.StubPlanService;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.services.GetSimulationResultsAction;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationService;
import gov.nasa.jpl.aerie.merlin.server.utils.HttpRequester;
import io.javalin.Javalin;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.JsonbBuilder;
import javax.json.stream.JsonParsingException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.activityInstanceP;
import static org.assertj.core.api.Assertions.assertThat;

public final class MerlinBindingsTest {
  private static Javalin SERVER = null;

  @BeforeClass
  public static void setupServer() {
    final var planApp = new StubPlanService();
    final var adaptationApp = new StubAdaptationService();
    final var simulationAction = new GetSimulationResultsAction(
        planApp,
        adaptationApp,
        new SynchronousSimulationService(new SynchronousSimulationAgent(planApp, adaptationApp)));

    SERVER = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config.enableCorsForAllOrigins();
      config.registerPlugin(new MerlinBindings(planApp, adaptationApp, simulationAction));
    });

    SERVER.start();
  }

  @AfterClass
  public static void shutdownServer() {
    SERVER.stop();
  }

  private final URI baseUri = URI.create("http://localhost:" + SERVER.port());
  private final HttpClient rawHttpClient = HttpClient.newHttpClient();
  private final HttpRequester client = new HttpRequester(rawHttpClient, baseUri);

  private <T> T parseJson(final String subject, final JsonParser<T> parser)
  throws InvalidJsonException, InvalidEntityException
  {
    try {
      final var requestJson = Json.createReader(new StringReader(subject)).readValue();
      final var result = parser.parse(requestJson);
      return result.getSuccessOrThrow(() -> new InvalidEntityException(List.of(result.failureReason())));
    } catch (JsonParsingException e) {
      throw new InvalidJsonException(e);
    }
  }

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

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans");

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    // TODO: Verify the structure of the response entity.
  }

  @Test
  public void shouldGetPlanById() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    // TODO: Verify the structure of the response entity.
  }

  @Test
  public void shouldReturn404OnNonexistentPlanById() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldAddValidPlan() throws IOException, InterruptedException {
    // GIVEN
    final JsonValue plan = StubPlanService.VALID_NEW_PLAN_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans", plan);

    // THEN
    final String expectedPlanId = StubPlanService.EXISTENT_PLAN_ID;

    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(response.headers().firstValue("Location")).get().isEqualTo("/plans/" + expectedPlanId);

    final JsonObject responseBody = JsonbBuilder.create().fromJson(response.body(), JsonObject.class);
    assertThat(responseBody).containsKey("id");
  }

  @Test
  public void shouldNotAddInvalidPlan() throws IOException, InterruptedException {
    // GIVEN
    final JsonValue plan = StubPlanService.INVALID_NEW_PLAN_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans", plan);

    // THEN
    assertThat(response.statusCode()).isEqualTo(400);

    // TODO: Verify the structure of the error response entity.
  }

  @Test
  public void shouldReplaceExistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final JsonValue plan = StubPlanService.VALID_NEW_PLAN_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId, plan);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotReplaceNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;
    final JsonValue plan = StubPlanService.VALID_NEW_PLAN_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId, plan);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotReplaceInvalidPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final JsonValue plan = StubPlanService.INVALID_NEW_PLAN_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId, plan);

    // THEN
    assertThat(response.statusCode()).isEqualTo(400);

    // TODO: Verify the structure of the error response entity.
  }

  @Test
  public void shouldPatchExistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final JsonValue patch = StubPlanService.VALID_PATCH_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotPatchNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;
    final JsonValue patch = StubPlanService.VALID_PATCH_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotPatchInvalidPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final JsonValue patch = StubPlanService.INVALID_PATCH_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(422);

    final JsonValue responseJson = JsonbBuilder.create().fromJson(response.body(), JsonValue.class);
    final JsonValue expectedJson = ResponseSerializers.serializeValidationMessages(StubPlanService.VALIDATION_ERRORS);
    assertThat(responseJson).isEqualTo(expectedJson);
  }

  @Test
  public void shouldRemoveExistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotRemoveNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldGetActivityInstances() throws IOException, InterruptedException, InvalidEntityException, InvalidJsonException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityId = StubPlanService.EXISTENT_ACTIVITY_ID;
    final ActivityInstance activity = StubPlanService.EXISTENT_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances");

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final var responseJson = Json.createReader(new StringReader(response.body())).readValue();
    final Map<String, ActivityInstance> activities = parseJson(responseJson.toString(), mapP(activityInstanceP));

    assertThat(activities).containsEntry(activityId, activity);
  }

  @Test
  public void shouldNotGetActivityInstancesForNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances");

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldGetActivityInstanceById() throws IOException, InterruptedException, InvalidEntityException, InvalidJsonException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;
    final ActivityInstance expectedActivityInstance = StubPlanService.EXISTENT_ACTIVITY;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final var responseJson = Json.createReader(new StringReader(response.body())).readValue();
    final ActivityInstance activityInstance = parseJson(responseJson.toString(), activityInstanceP);
    assertThat(activityInstance).isEqualTo(expectedActivityInstance);
  }

  @Test
  public void shouldNotGetActivityInstanceFromNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotGetNonexistentActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.NONEXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldAddActivityInstancesToPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final JsonValue activityInstanceList = StubPlanService.VALID_ACTIVITY_LIST_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans/" + planId + "/activity_instances", activityInstanceList);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    // TODO: Verify the structure of the error response entity.
  }

  @Test
  public void shouldNotAddActivityInstancesToNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;
    final JsonValue activityInstanceList = StubPlanService.VALID_ACTIVITY_LIST_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans/" + planId + "/activity_instances", activityInstanceList);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotAddInvalidActivityInstancesToPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final JsonValue activityInstanceList = StubPlanService.INVALID_ACTIVITY_LIST_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("POST", "/plans/" + planId + "/activity_instances", activityInstanceList);

    // THEN
    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  public void shouldDeleteActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotDeleteActivityInstanceFromNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotDeleteNonexistentActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.NONEXISTENT_ACTIVITY_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/plans/" + planId + "/activity_instances/" + activityInstanceId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldPatchActivityInstanceById() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;
    final JsonValue patch = StubPlanService.VALID_ACTIVITY_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId + "/activity_instances/" + activityInstanceId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotPatchActivityInstanceInNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;
    final JsonValue patch = StubPlanService.VALID_ACTIVITY_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId + "/activity_instances/" + activityInstanceId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotPatchNonexistentActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.NONEXISTENT_ACTIVITY_ID;
    final JsonValue patch = StubPlanService.VALID_ACTIVITY_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId + "/activity_instances/" + activityInstanceId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotPatchInvalidActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;
    final JsonValue patch = StubPlanService.INVALID_ACTIVITY_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PATCH", "/plans/" + planId + "/activity_instances/" + activityInstanceId, patch);

    // THEN
    assertThat(response.statusCode()).isEqualTo(422);

    final JsonValue responseJson = JsonbBuilder.create().fromJson(response.body(), JsonValue.class);
    final JsonValue expectedJson = ResponseSerializers.serializeValidationMessages(StubPlanService.VALIDATION_ERRORS);
    assertThat(responseJson).isEqualTo(expectedJson);
  }

  @Test
  public void shouldReplaceActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;
    final JsonValue activityInstance = StubPlanService.VALID_ACTIVITY_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId + "/activity_instances/" + activityInstanceId, activityInstance);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotReplaceActivityInstanceOfNonexistentPlan() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;
    final JsonValue activityInstance = StubPlanService.VALID_ACTIVITY_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId + "/activity_instances/" + activityInstanceId, activityInstance);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotReplaceNonexistentActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.NONEXISTENT_ACTIVITY_ID;
    final JsonValue activityInstance = StubPlanService.VALID_ACTIVITY_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId + "/activity_instances/" + activityInstanceId, activityInstance);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotReplaceInvalidActivityInstance() throws IOException, InterruptedException {
    // GIVEN
    final String planId = StubPlanService.EXISTENT_PLAN_ID;
    final String activityInstanceId = StubPlanService.EXISTENT_ACTIVITY_ID;
    final JsonValue activityInstance = StubPlanService.INVALID_ACTIVITY_JSON;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("PUT", "/plans/" + planId + "/activity_instances/" + activityInstanceId, activityInstance);

    // THEN
    assertThat(response.statusCode()).isEqualTo(400);

    // TODO: Verify the structure of the error response entity.
  }

  @Test
  public void shouldGetAdaptations() throws IOException, InterruptedException {
    // GIVEN
    final JsonValue expectedResponse = ResponseSerializers.serializeAdaptations(Map.of(
        StubAdaptationService.EXISTENT_ADAPTATION_ID,
        StubAdaptationService.EXISTENT_ADAPTATION
    ));

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations");

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
    assertThat(responseJson).isEqualTo(expectedResponse);
  }

  @Test
  public void shouldGetAdaptationById() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;
    final JsonValue expectedResponse = ResponseSerializers.serializeAdaptation(StubAdaptationService.EXISTENT_ADAPTATION);

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations/" + adaptationId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
    assertThat(responseJson).isEqualTo(expectedResponse);
  }

  @Test
  public void shouldReturn404OnNonexistentAdaptationById() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.NONEXISTENT_ADAPTATION_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations/" + adaptationId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldConfirmAdaptationExists() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations/" + adaptationId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldDenyNonexistentAdaptationExists() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.NONEXISTENT_ADAPTATION_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations/" + adaptationId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldAddValidAdaptation() throws IOException, InterruptedException {
    // GIVEN
    final Map<String, Object> adaptationRequest = StubAdaptationService.VALID_NEW_ADAPTATION;

    // WHEN
    final HttpResponse<String> response = sendRequest("POST", "/adaptations", adaptationRequest);

    // THEN
    assertThat(response.statusCode()).isEqualTo(201);
  }

  @Test
  public void shouldNotAddInvalidAdaptation() throws IOException, InterruptedException {
    // GIVEN
    final Map<String, Object> adaptationRequest = StubAdaptationService.INVALID_NEW_ADAPTATION;

    // WHEN
    final HttpResponse<String> response = sendRequest("POST", "/adaptations", adaptationRequest);

    // THEN
    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  public void shouldRemoveExistentAdaptation() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/adaptations/" + adaptationId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void shouldNotRemoveNonexistentAdaptation() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.NONEXISTENT_ADAPTATION_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("DELETE", "/adaptations/" + adaptationId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldGetActivityTypeList() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;
    final String activityId = StubAdaptationService.EXISTENT_ACTIVITY_TYPE;
    final JsonValue expectedResponse = ResponseSerializers.serializeActivityTypes(Map.of(activityId, StubAdaptationService.EXISTENT_ACTIVITY));

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations/" + adaptationId + "/activities");

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
    assertThat(responseJson).isEqualTo(expectedResponse);
  }

  @Test
  public void shouldNotGetActivityTypeListForNonexistentAdaptation() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.NONEXISTENT_ADAPTATION_ID;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations/" + adaptationId + "/activities");

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldGetActivityTypeById() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;
    final String activityId = StubAdaptationService.EXISTENT_ACTIVITY_TYPE;
    final JsonValue expectedResponse = ResponseSerializers.serializeActivityType(StubAdaptationService.EXISTENT_ACTIVITY);

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
    assertThat(responseJson).isEqualTo(expectedResponse);
  }

  @Test
  public void shouldNotGetActivityTypeByIdForNonexistentAdaptation() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.NONEXISTENT_ADAPTATION_ID;
    final String activityId = StubAdaptationService.EXISTENT_ACTIVITY_TYPE;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldNotGetActivityTypeByIdForNonexistentActivityType() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;
    final String activityId = StubAdaptationService.NONEXISTENT_ACTIVITY_TYPE;

    // WHEN
    final HttpResponse<String> response = client.sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId);

    // THEN
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  public void shouldValidateValidActivityParameters() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;
    final String activityId = StubAdaptationService.EXISTENT_ACTIVITY_TYPE;
    final SerializedActivity activityParameters = StubAdaptationService.VALID_ACTIVITY_INSTANCE;

    final JsonValue expectedResponse = ResponseSerializers.serializeFailureList(List.of());

    // WHEN
    final HttpResponse<String> response = client.sendRequest(
        "POST",
        "/adaptations/" + adaptationId + "/activities/" + activityId + "/validate",
        ResponseSerializers.serializeActivityParameterMap(activityParameters.getParameters()));

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
    assertThat(responseJson).isEqualTo(expectedResponse);
  }

  @Test
  public void shouldRejectActivityParametersForNonexistentActivityType() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;
    final String activityId = StubAdaptationService.NONEXISTENT_ACTIVITY_TYPE;
    final SerializedActivity activityParameters = StubAdaptationService.NONEXISTENT_ACTIVITY_INSTANCE;

    final JsonValue expectedResponse = ResponseSerializers.serializeFailureList(StubAdaptationService.NO_SUCH_ACTIVITY_TYPE_FAILURES);

    // WHEN
    final HttpResponse<String> response = client.sendRequest(
        "POST",
        "/adaptations/" + adaptationId + "/activities/" + activityId + "/validate",
        ResponseSerializers.serializeActivityParameterMap(activityParameters.getParameters()));

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
    assertThat(responseJson).isEqualTo(expectedResponse);
  }

  @Test
  public void shouldRejectInvalidActivityParameters() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;
    final String activityId = StubAdaptationService.EXISTENT_ACTIVITY_TYPE;
    final SerializedActivity activityParameters = StubAdaptationService.INVALID_ACTIVITY_INSTANCE;

    final JsonValue expectedResponse = ResponseSerializers.serializeFailureList(StubAdaptationService.INVALID_ACTIVITY_INSTANCE_FAILURES);

    // WHEN
    final HttpResponse<String> response = client.sendRequest(
        "POST",
        "/adaptations/" + adaptationId + "/activities/" + activityId + "/validate",
        ResponseSerializers.serializeActivityParameterMap(activityParameters.getParameters()));

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
    assertThat(responseJson).isEqualTo(expectedResponse);
  }

  @Test
  public void shouldRejectUnconstructableActivityParameters() throws IOException, InterruptedException {
    // GIVEN
    final String adaptationId = StubAdaptationService.EXISTENT_ADAPTATION_ID;
    final String activityId = StubAdaptationService.EXISTENT_ACTIVITY_TYPE;
    final SerializedActivity activityParameters = StubAdaptationService.UNCONSTRUCTABLE_ACTIVITY_INSTANCE;

    final JsonValue expectedResponse = ResponseSerializers.serializeFailureList(StubAdaptationService.UNCONSTRUCTABLE_ACTIVITY_INSTANCE_FAILURES);

    // WHEN
    final HttpResponse<String> response = client.sendRequest(
        "POST",
        "/adaptations/" + adaptationId + "/activities/" + activityId + "/validate",
        ResponseSerializers.serializeActivityParameterMap(activityParameters.getParameters()));

    // THEN
    assertThat(response.statusCode()).isEqualTo(200);

    final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
    assertThat(responseJson).isEqualTo(expectedResponse);
  }

  private HttpResponse<String> sendRequest(final String method, final String path, final Map<String, Object> body)
  throws IOException, InterruptedException
  {
    final String boundary = new BigInteger(256, new Random()).toString();
    final HttpRequest.BodyPublisher bodyPublisher = ofMimeMultipartData(body, boundary);

    return sendRequest(method, path, bodyPublisher, Optional.of(boundary));
  }

  private HttpResponse<String> sendRequest(final String method, final String path, final HttpRequest.BodyPublisher bodyPublisher, final Optional<String> boundary)
  throws IOException, InterruptedException
  {
    final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();

    if (boundary.isPresent()) {
      requestBuilder.headers("Content-Type", "multipart/form-data;boundary=" + boundary.get());
    }

    final HttpRequest request = requestBuilder
        .uri(baseUri.resolve(path))
        .method(method, bodyPublisher)
        .build();

    return this.rawHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static HttpRequest.BodyPublisher ofMimeMultipartData(final Map<String, Object> data, final String boundary) {
    final StringBuilder bodyBuilder = new StringBuilder();
    for (final var entry : data.entrySet()) {
      if (entry.getValue() instanceof FakeFile) {
        final FakeFile file = (FakeFile) entry.getValue();

        bodyBuilder
            .append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; ")
                .append("name=\"").append(entry.getKey()).append("\"; ")
                .append("filename=\"").append(file.filename).append("\"\r\n")
            .append("Content-Type: ").append(file.contentType).append("\r\n")
            .append("\r\n")
            .append(file.contents).append("\r\n");
      } else {
        bodyBuilder
            .append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n")
            .append("\r\n")
            .append(entry.getValue()).append("\r\n");
      }
    }
    bodyBuilder.append("--").append(boundary).append("--");

    return HttpRequest.BodyPublishers.ofString(bodyBuilder.toString(), StandardCharsets.UTF_8);
  }
}
