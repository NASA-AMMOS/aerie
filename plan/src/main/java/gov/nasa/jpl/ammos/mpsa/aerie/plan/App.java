package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.PlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.http.PlanBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.RemoteAdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.RemotePlanRepository;
import io.javalin.Javalin;

import java.net.URI;

public final class App {
  private static final int HTTP_PORT = 27183;

  private static final URI ADAPTATION_URI = URI.create("http://adaptation:27182");

  private static final URI MONGO_URI = URI.create("mongodb://plan_mongo:27018");
  private static final String MONGO_DATABASE = "plan-service";
  private static final String MONGO_PLAN_COLLECTION = "plans";
  private static final String MONGO_ACTIVITY_COLLECTION = "activities";

  public static void main(final String[] args) {
    // Assemble the core non-web object graph.
    final PlanRepository planRepository = new RemotePlanRepository(MONGO_URI, MONGO_DATABASE, MONGO_PLAN_COLLECTION, MONGO_ACTIVITY_COLLECTION);
    final AdaptationService adaptationService = new RemoteAdaptationService(ADAPTATION_URI);
    final IPlanController controller = new PlanController(planRepository, adaptationService);
    final PlanBindings bindings = new PlanBindings(controller);

    // Initiate an HTTP server.
    final Javalin javalin = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config.enableCorsForAllOrigins();
    });
    bindings.registerRoutes(javalin);
    javalin.start(HTTP_PORT);
  }
}
