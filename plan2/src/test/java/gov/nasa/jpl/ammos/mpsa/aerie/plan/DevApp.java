package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.PlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks.Fixtures;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.http.PlanBindings;
import io.javalin.Javalin;

public final class DevApp {
  private static final int HTTP_PORT = 27183;

  public static void main(final String[] args) {
    // Assemble the core non-web object graph.
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);
    final PlanBindings bindings = new PlanBindings(controller);

    // Initiate an HTTP server.
    final Javalin javalin = Javalin.create();
    bindings.registerRoutes(javalin);

    // Start the HTTP server.
    javalin.start(HTTP_PORT);
  }
}
