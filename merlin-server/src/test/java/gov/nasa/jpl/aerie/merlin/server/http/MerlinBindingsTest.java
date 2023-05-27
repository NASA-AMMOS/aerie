package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.merlin.server.mocks.StubMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.mocks.StubPlanService;
import gov.nasa.jpl.aerie.merlin.server.services.ConstraintsDSLCompilationService;
import gov.nasa.jpl.aerie.merlin.server.services.GenerateConstraintsLibAction;
import gov.nasa.jpl.aerie.merlin.server.services.GetSimulationResultsAction;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.TypescriptCodeGenerationServiceAdapter;
import gov.nasa.jpl.aerie.merlin.server.services.UncachedSimulationService;
import io.javalin.Javalin;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

public final class MerlinBindingsTest {
  private static Javalin SERVER = null;

  @BeforeAll
  public static void setupServer() {
    final var planApp = new StubPlanService();
    final var missionModelApp = new StubMissionModelService();

    final var typescriptCodeGenerationService = new TypescriptCodeGenerationServiceAdapter(missionModelApp, planApp);

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
        new UncachedSimulationService(new SynchronousSimulationAgent(planApp, missionModelApp, false)),
        constraintsDSLCompilationService
    );

    final var generateConstraintsLibAction = new GenerateConstraintsLibAction(typescriptCodeGenerationService);

    SERVER = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config.plugins.enableCors(cors -> cors.add(CorsPluginConfig::anyHost));
      config.plugins.register(new MerlinBindings(missionModelApp, planApp, simulationAction, generateConstraintsLibAction));
    });

    SERVER.start(54321); // Use likely unused port to avoid clash with any currently hosted port 80 services
  }

  @AfterAll
  public static void shutdownServer() {
    SERVER.stop();
  }

  private final URI baseUri = URI.create("http://localhost:" + SERVER.port());
  private final HttpClient rawHttpClient = HttpClient.newHttpClient();

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
}
