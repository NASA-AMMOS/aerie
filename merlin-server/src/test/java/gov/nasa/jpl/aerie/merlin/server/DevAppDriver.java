package gov.nasa.jpl.aerie.merlin.server;

import gov.nasa.jpl.aerie.merlin.server.http.LocalAppExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.MerlinBindings;
import gov.nasa.jpl.aerie.merlin.server.http.MissionModelExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.MissionModelRepositoryExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.mocks.Fixtures;
import gov.nasa.jpl.aerie.merlin.server.mocks.InMemoryMissionModelRepository;
import gov.nasa.jpl.aerie.merlin.server.services.ConstraintsDSLCompilationService;
import gov.nasa.jpl.aerie.merlin.server.services.GenerateConstraintsLibAction;
import gov.nasa.jpl.aerie.merlin.server.services.GetSimulationResultsAction;
import gov.nasa.jpl.aerie.merlin.server.services.LocalMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.services.LocalPlanService;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.TypescriptCodeGenerationServiceAdapter;
import gov.nasa.jpl.aerie.merlin.server.services.UncachedSimulationService;
import io.javalin.Javalin;

import java.io.IOException;
import java.nio.file.Path;

public final class DevAppDriver {
  private static final int HTTP_PORT = 27183;

  public static void main(final String[] args) {
    // Assemble the core non-web object graph.
    final var fixtures = new Fixtures();
    final var missionModelController = new LocalMissionModelService(Path.of("/dev/null"), new InMemoryMissionModelRepository());
    final var planController = new LocalPlanService(fixtures.planRepository);

    final var typescriptCodeGenerationService = new TypescriptCodeGenerationServiceAdapter(missionModelController);

    final ConstraintsDSLCompilationService constraintsDSLCompilationService;
    try {
      constraintsDSLCompilationService = new ConstraintsDSLCompilationService(typescriptCodeGenerationService);
    } catch (IOException e) {
      throw new Error("Failed to start ConstraintsDSLCompilationService", e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(constraintsDSLCompilationService::close));

    final var simulationAction = new GetSimulationResultsAction(
        planController,
        missionModelController,
        new UncachedSimulationService(new SynchronousSimulationAgent(planController, missionModelController)),
        constraintsDSLCompilationService,
        false
    );

    final var generateConstraintsLibAction = new GenerateConstraintsLibAction(typescriptCodeGenerationService);

    // Configure an HTTP server.
    final Javalin javalin = Javalin.create(config -> {
        config.enableDevLogging();
        config.enableCorsForAllOrigins();
        config.registerPlugin(new MerlinBindings(missionModelController, planController, simulationAction, generateConstraintsLibAction));
        config.registerPlugin(new LocalAppExceptionBindings());
        config.registerPlugin(new MissionModelRepositoryExceptionBindings());
        config.registerPlugin(new MissionModelExceptionBindings());
    });

    // Start the HTTP server.
    javalin.start(HTTP_PORT);
  }
}
