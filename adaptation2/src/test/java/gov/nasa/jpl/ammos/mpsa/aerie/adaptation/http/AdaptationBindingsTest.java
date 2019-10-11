package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.StubAdaptationController;
import io.javalin.Javalin;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public final class AdaptationBindingsTest {
    private static Javalin app = null;

    @BeforeAll
    public static void setupServer() {
        final StubAdaptationController controller = new StubAdaptationController();
        final AdaptationBindings bindings = new AdaptationBindings(controller);

        app = Javalin.create();
        bindings.registerRoutes(app);
        app.start();
    }

    @AfterAll
    public static void shutdownServer() {
        app.stop();
    }

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    public void shouldGetAdaptations() throws IOException, InterruptedException {
        // GIVEN
        final JsonValue expectedResponse = ResponseSerializers.serializeAdaptations(Map.of(
            StubAdaptationController.EXISTENT_ADAPTATION_ID, StubAdaptationController.EXISTENT_ADAPTATION
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
        final String adaptationId = StubAdaptationController.EXISTENT_ADAPTATION_ID;
        final JsonValue expectedResponse = ResponseSerializers.serializeAdaptation(StubAdaptationController.EXISTENT_ADAPTATION);

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
        final String adaptationId = StubAdaptationController.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldAddValidAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final Map<Object, Object> adaptationRequest = StubAdaptationController.VALID_NEW_ADAPTATION;

        // WHEN
        final HttpResponse<String> response = sendRequest("POST", "/adaptations", adaptationRequest);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_CREATED);
    }

    @Test
    public void shouldNotAddInvalidAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final Map<Object, Object> adaptationRequest = StubAdaptationController.INVALID_NEW_ADAPTATION;

        // WHEN
        final HttpResponse<String> response = sendRequest("POST", "/adaptations", adaptationRequest);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    @Test
    public void shouldRemoveExistentAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubAdaptationController.EXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("DELETE", "/adaptations/" + adaptationId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
    }

    @Test
    public void shouldNotRemoveNonexistentAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubAdaptationController.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("DELETE", "/adaptations/" + adaptationId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldGetActivityTypeList() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubAdaptationController.EXISTENT_ADAPTATION_ID;
        final String activityId = StubAdaptationController.EXISTENT_ACTIVITY_ID;
        final JsonValue expectedResponse = ResponseSerializers.serializeActivityTypes(Map.of(activityId, StubAdaptationController.EXISTENT_ACTIVITY));

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
        final String adaptationId = StubAdaptationController.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities");

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldGetActivityTypeById() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubAdaptationController.EXISTENT_ADAPTATION_ID;
        final String activityId = StubAdaptationController.EXISTENT_ACTIVITY_ID;
        final JsonValue expectedResponse = ResponseSerializers.serializeActivityType(StubAdaptationController.EXISTENT_ACTIVITY);

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
        final String adaptationId = StubAdaptationController.NONEXISTENT_ADAPTATION_ID;
        final String activityId = StubAdaptationController.EXISTENT_ACTIVITY_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldNotGetActivityTypeByIdForNonexistentActivityType() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubAdaptationController.EXISTENT_ADAPTATION_ID;
        final String activityId = StubAdaptationController.NONEXISTENT_ACTIVITY_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldGetActivityTypeParameters() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubAdaptationController.EXISTENT_ADAPTATION_ID;
        final String activityId = StubAdaptationController.EXISTENT_ACTIVITY_ID;
        final JsonValue expectedResponse = ResponseSerializers.serializeParameterSchemas(StubAdaptationController.EXISTENT_ACTIVITY.parameters);

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId + "/parameters");

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);

        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        assertThat(responseJson).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldNotGetActivityTypeParametersForNonexistentAdaptation() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubAdaptationController.NONEXISTENT_ADAPTATION_ID;
        final String activityId = StubAdaptationController.EXISTENT_ACTIVITY_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId + "/parameters");

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void shouldNotGetActivityTypeParametersForNonexistentActivity() throws IOException, InterruptedException {
        // GIVEN
        final String adaptationId = StubAdaptationController.EXISTENT_ADAPTATION_ID;
        final String activityId = StubAdaptationController.NONEXISTENT_ACTIVITY_ID;

        // WHEN
        final HttpResponse<String> response = sendRequest("GET", "/adaptations/" + adaptationId + "/activities/" + activityId + "/parameters");

        // THEN
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    private HttpResponse<String> sendRequest(final String method, final String path)
            throws IOException, InterruptedException
    {
        return sendRequest(method, path, HttpRequest.BodyPublishers.noBody(), Optional.empty());
    }

    private HttpResponse<String> sendRequest(final String method, final String path, final Map<Object, Object> body)
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

        if (boundary.isPresent())
            requestBuilder.headers("Content-Type", "multipart/form-data;boundary="+boundary.get());

        final HttpRequest request = requestBuilder
                .uri(URI.create("http://localhost:" + app.port() + path))
                .method(method, bodyPublisher)
                .build();

        return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest.BodyPublisher ofMimeMultipartData(final Map<Object, Object> data, final String boundary) throws IOException {
        final List<byte[]> byteArrays = new ArrayList<>();
        final byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=")
                .getBytes(StandardCharsets.UTF_8);
        for (final Map.Entry<Object, Object> entry : data.entrySet()) {
            byteArrays.add(separator);

            if (entry.getValue() instanceof Path) {
                final Path path = (Path) entry.getValue();
                final String mimeType = Files.probeContentType(path);
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName()
                        + "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                byteArrays.add(Files.readAllBytes(path));
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            else {
                byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
        }
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
}
