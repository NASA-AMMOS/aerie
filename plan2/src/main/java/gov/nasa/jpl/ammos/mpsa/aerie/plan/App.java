package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.PlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.http.PlanBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.RemoteAdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.RemotePlanRepository;
import io.javalin.Javalin;

public final class App {
  private static final int HTTP_PORT = 27183;

  public static void main(final String[] args) {
    // Assemble the core non-web object graph.
    final PlanRepository planRepository = new RemotePlanRepository();
    final AdaptationService adaptationService = new RemoteAdaptationService();
    final IPlanController controller = new PlanController(planRepository, adaptationService);
    final PlanBindings bindings = new PlanBindings(controller);

    // Initiate an HTTP server.
    final Javalin javalin = Javalin.create();
    bindings.registerRoutes(javalin);
    javalin.start(HTTP_PORT);
  }
}
