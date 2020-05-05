package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.CreateSimulationMessage;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.FakeFile;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.StubApp;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import io.javalin.Javalin;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public final class AdaptationBindingsTest {
    private static Javalin SERVER = null;

    @BeforeAll
    public static void setupServer() {
        final StubApp app = new StubApp();

        SERVER = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.enableCorsForAllOrigins();
            config.registerPlugin(new AdaptationBindings(app));
        });
        SERVER.start();
    }

    @AfterAll
    public static void shutdownServer() {
        SERVER.stop();
    }

    private final URI baseUri = URI.create("http://localhost:" + SERVER.port());
    private final HttpClient client = HttpClient.newHttpClient();

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

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // THEN
        assertThat(response.headers().allValues("Access-Control-Allow-Origin")).isNotEmpty();
    }

    @Test
    public void shouldGetAdaptations() throws IOException, InterruptedException {
        // GIVEN
        final JsonValue expectedResponse = ResponseSerializers.serializeAdaptations(Map.of(
            StubApp.EXISTENT_ADAPTATION_ID, StubApp.EXISTENT_ADAPTATION
        ));

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations");

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldGetAdaptationById() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;
        final JsonValue expectedResponse = ResponseSerializers.serializeAdaptation(StubApp.EXISTENT_ADAPTATION);

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldReturn404OnNonexistentAdaptationById() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldConfirmAdaptationExists() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
    }

    @Test
    public void shouldDenyNonexistentAdaptationExists() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldAddValidAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final Map<String, Object> adaptationRequest = StubApp.VALID_NEW_ADAPTATION;

        // WHEN
        final HttpResponse<String> response = sendRequest("POST", "/adaptations", adaptationRequest);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_CREATED);
    }

    @Test
    public void shouldNotAddInvalidAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final Map<String, Object> adaptationRequest = StubApp.INVALID_NEW_ADAPTATION;

        // WHEN
        final HttpResponse<String> response = sendRequest("POST", "/adaptations", adaptationRequest);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    @Test
    public void shouldRemoveExistentAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("DELETE", "/adaptations/" + adaptationId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
    }

    @Test
    public void shouldNotRemoveNonexistentAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("DELETE", "/adaptations/" + adaptationId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldGetActivityTypeList() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;
        final String activityId = StubApp.EXISTENT_ACTIVITY_TYPE;
        final JsonValue expectedResponse = ResponseSerializers.serializeActivityTypes(Map.of(activityId, StubApp.EXISTENT_ACTIVITY));

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities");

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldNotGetActivityTypeListForNonexistentAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities");

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldGetActivityTypeById() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;
        final String activityId = StubApp.EXISTENT_ACTIVITY_TYPE;
        final JsonValue expectedResponse = ResponseSerializers.serializeActivityType(StubApp.EXISTENT_ACTIVITY);

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldNotGetActivityTypeByIdForNonexistentAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.NONEXISTENT_ADAPTATION_ID;
        final String activityId = StubApp.EXISTENT_ACTIVITY_TYPE;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldNotGetActivityTypeByIdForNonexistentActivityType() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;
        final String activityId = StubApp.NONEXISTENT_ACTIVITY_TYPE;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldValidateValidActivityParameters() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;
        final String activityId = StubApp.EXISTENT_ACTIVITY_TYPE;
        final SerializedActivity activityParameters = StubApp.VALID_ACTIVITY_INSTANCE;

        final JsonValue expectedResponse = ResponseSerializers.serializeFailureList(List.of());

        // WHEN
        final HttpResponse<String> response = sendRequest(
            "POST",
            "/adaptations/" + adaptationId + "/activities/" + activityId + "/validate",
            ResponseSerializers.serializeActivityParameters(activityParameters.getParameters()));

        // THEN
        assertThat(response.statusCode()).isEqualTo(200);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldRejectActivityParametersForNonexistentActivityType() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;
        final String activityId = StubApp.NONEXISTENT_ACTIVITY_TYPE;
        final SerializedActivity activityParameters = StubApp.NONEXISTENT_ACTIVITY_INSTANCE;

        final JsonValue expectedResponse = ResponseSerializers.serializeFailureList(StubApp.NO_SUCH_ACTIVITY_TYPE_FAILURES);

        // WHEN
        final HttpResponse<String> response = sendRequest(
            "POST",
            "/adaptations/" + adaptationId + "/activities/" + activityId + "/validate",
            ResponseSerializers.serializeActivityParameters(activityParameters.getParameters()));

        // THEN
        assertThat(response.statusCode()).isEqualTo(200);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldRejectInvalidActivityParameters() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;
        final String activityId = StubApp.EXISTENT_ACTIVITY_TYPE;
        final SerializedActivity activityParameters = StubApp.INVALID_ACTIVITY_INSTANCE;

        final JsonValue expectedResponse = ResponseSerializers.serializeFailureList(StubApp.INVALID_ACTIVITY_INSTANCE_FAILURES);

        // WHEN
        final HttpResponse<String> response = sendRequest(
            "POST",
            "/adaptations/" + adaptationId + "/activities/" + activityId + "/validate",
            ResponseSerializers.serializeActivityParameters(activityParameters.getParameters()));

        // THEN
        assertThat(response.statusCode()).isEqualTo(200);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldRejectUnconstructableActivityParameters() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubApp.EXISTENT_ADAPTATION_ID;
        final String activityId = StubApp.EXISTENT_ACTIVITY_TYPE;
        final SerializedActivity activityParameters = StubApp.UNCONSTRUCTABLE_ACTIVITY_INSTANCE;

        final JsonValue expectedResponse = ResponseSerializers.serializeFailureList(StubApp.UNCONSTRUCTABLE_ACTIVITY_INSTANCE_FAILURES);

        // WHEN
        final HttpResponse<String> response = sendRequest(
            "POST",
            "/adaptations/" + adaptationId + "/activities/" + activityId + "/validate",
            ResponseSerializers.serializeActivityParameters(activityParameters.getParameters()));

        // THEN
        assertThat(response.statusCode()).isEqualTo(200);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldRunSimulation() throws IOException, InterruptedException {
        // GIVEN
        final CreateSimulationMessage message = new CreateSimulationMessage(
            StubApp.EXISTENT_ADAPTATION_ID,
            Instant.EPOCH,
            Duration.ZERO, Duration.ZERO,
            List.of(
                Pair.of(Duration.ZERO, StubApp.VALID_ACTIVITY_INSTANCE)
            )
        );

        final JsonValue expectedResponse = ResponseSerializers.serializeSimulationResults(StubApp.SUCCESSFUL_SIMULATION_RESULTS);

        // WHEN
        final HttpResponse<String> response = sendRequest(
            "POST",
            "/simulations",
            ResponseSerializers.serializeCreateSimulationMessage(message));

        // THEN
        assertThat(response.statusCode()).isEqualTo(200);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    private HttpResponse<String> sendRequest(final String method, final String path)
            throws IOException, InterruptedException
    {
        return sendRequest(method, path, HttpRequest.BodyPublishers.noBody(), Optional.empty());
    }

    private HttpResponse<String> sendRequest(final String method, final String path, final Map<String, Object> body)
            throws IOException, InterruptedException
    {
        final String boundary = new BigInteger(256, new Random()).toString();
        final HttpRequest.BodyPublisher bodyPublisher = ofMimeMultipartData(body, boundary);

        return sendRequest(method, path, bodyPublisher, Optional.of(boundary));
    }

    private HttpResponse<String> sendRequest(final String method, final String path, final JsonValue body)
            throws IOException, InterruptedException
    {
        final HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(body.toString());

        return sendRequest(method, path, bodyPublisher, Optional.empty());
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

        return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest.BodyPublisher ofMimeMultipartData(final Map<String, Object> data, final String boundary) {
        final StringBuilder bodyBuilder = new StringBuilder();
        for (final var entry : data.entrySet()) {
            if (entry.getValue() instanceof FakeFile) {
                final FakeFile file = (FakeFile) entry.getValue();

                bodyBuilder.append("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"; filename=\"" + file.filename + "\"\r\n"
                    + "Content-Type: " + file.contentType + "\r\n"
                    + "\r\n"
                    + file.contents + "\r\n"
                );
            } else {
                bodyBuilder.append("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n"
                    + "\r\n"
                    + entry.getValue() + "\r\n"
                );
            }
        }
        bodyBuilder.append("--" + boundary + "--");

        return HttpRequest.BodyPublishers.ofString(bodyBuilder.toString(), StandardCharsets.UTF_8);
    }
}
