package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.server.mocks.FakeFile;
import gov.nasa.jpl.aerie.merlin.server.mocks.StubMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.mocks.StubPlanService;
import gov.nasa.jpl.aerie.merlin.server.services.ConstraintsDSLCompilationService;
import gov.nasa.jpl.aerie.merlin.server.services.GenerateConstraintsLibAction;
import gov.nasa.jpl.aerie.merlin.server.services.GetSimulationResultsAction;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.TypescriptCodeGenerationServiceAdapter;
import gov.nasa.jpl.aerie.merlin.server.services.UncachedSimulationService;
import gov.nasa.jpl.aerie.merlin.server.utils.HttpRequester;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.json.Json;
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

import static org.assertj.core.api.Assertions.assertThat;

public final class MerlinBindingsTest {
  private static Javalin SERVER = null;

  @BeforeAll
  public static void setupServer() {
    final var planApp = new StubPlanService();
    final var missionModelApp = new StubMissionModelService();

    final var typescriptCodeGenerationService = new TypescriptCodeGenerationServiceAdapter(missionModelApp);

    final ConstraintsDSLCompilationService constraintsDSLCompilationService;
    try {
      constraintsDSLCompilationService = new ConstraintsDSLCompilationService(typescriptCodeGenerationService);
    } catch (IOException e) {
      throw new Error("Failed to start ConstraintsDSLCompilationService", e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(constraintsDSLCompilationService::close));

    final var simulationAction = new GetSimulationResultsAction(
        planApp,
        missionModelApp,
        new UncachedSimulationService(new SynchronousSimulationAgent(planApp, missionModelApp)),
        constraintsDSLCompilationService
    );

    final var generateConstraintsLibAction = new GenerateConstraintsLibAction(typescriptCodeGenerationService);

    SERVER = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config.enableCorsForAllOrigins();
      config.registerPlugin(new MerlinBindings(missionModelApp, planApp, simulationAction, generateConstraintsLibAction));
    });

    SERVER.start(54321); // Use likely unused port to avoid clash with any currently hosted port 80 services
  }

  @AfterAll
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
      return result.getSuccessOrThrow($ -> new InvalidEntityException(List.of($)));
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
