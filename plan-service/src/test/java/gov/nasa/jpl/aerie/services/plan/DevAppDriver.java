package gov.nasa.jpl.aerie.services.plan;

import gov.nasa.jpl.aerie.services.plan.services.LocalAdaptationService;
import gov.nasa.jpl.aerie.services.plan.http.AdaptationExceptionBindings;
import gov.nasa.jpl.aerie.services.plan.http.AdaptationRepositoryExceptionBindings;
import gov.nasa.jpl.aerie.services.plan.http.LocalAppExceptionBindings;
import gov.nasa.jpl.aerie.services.plan.mocks.MockAdaptationRepository;
import gov.nasa.jpl.aerie.services.plan.http.MerlinBindings;
import gov.nasa.jpl.aerie.services.plan.mocks.Fixtures;
import gov.nasa.jpl.aerie.services.plan.services.LocalPlanService;
import io.javalin.Javalin;

public final class DevAppDriver {
  private static final int HTTP_PORT = 27183;

  public static void main(final String[] args) {
    // Assemble the core non-web object graph.
    final var fixtures = new Fixtures();
    final var adaptationController = new LocalAdaptationService(new MockAdaptationRepository());
    final var planController = new LocalPlanService(fixtures.planRepository, adaptationController);

    // Configure an HTTP server.
    final Javalin javalin = Javalin.create(config -> config
        .enableDevLogging()
        .enableCorsForAllOrigins()
        .registerPlugin(new MerlinBindings(planController, adaptationController))
        .registerPlugin(new LocalAppExceptionBindings())
        .registerPlugin(new AdaptationRepositoryExceptionBindings())
        .registerPlugin(new AdaptationExceptionBindings()));

    // Start the HTTP server.
    javalin.start(HTTP_PORT);
  }
}
