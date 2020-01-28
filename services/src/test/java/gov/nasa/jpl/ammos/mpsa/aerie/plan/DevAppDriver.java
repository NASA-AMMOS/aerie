package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.App;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.LocalApp;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks.Fixtures;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.http.PlanBindings;
import io.javalin.Javalin;

public final class DevAppDriver {
  private static final int HTTP_PORT = 27183;

  public static void main(final String[] args) {
    // Assemble the core non-web object graph.
    final Fixtures fixtures = new Fixtures();
    final App controller = new LocalApp(fixtures.planRepository, fixtures.adaptationService);

    // Configure an HTTP server.
    final Javalin javalin = Javalin.create(config -> config
        .enableCorsForAllOrigins()
        .registerPlugin(new PlanBindings(controller)));

    // Start the HTTP server.
    javalin.start(HTTP_PORT);
  }
}
