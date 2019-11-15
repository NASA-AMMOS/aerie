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


  public static void main(final String[] args) {

    // Load the properties
    AppConfiguration configuration = AppConfiguration.loadProperties();
    if (configuration == null) {
      System.err.println("Not all properties loaded. Exiting.");
      System.exit(1);
    }

    // Assemble the core non-web object graph.
    final PlanRepository planRepository = new RemotePlanRepository(configuration.MONGO_URI, configuration.MONGO_DATABASE, configuration.MONGO_PLAN_COLLECTION, configuration.MONGO_ACTIVITY_COLLECTION);
    final AdaptationService adaptationService = new RemoteAdaptationService(configuration.ADAPTATION_URI);
    final IPlanController controller = new PlanController(planRepository, adaptationService);
    final PlanBindings bindings = new PlanBindings(controller);

    // Initiate an HTTP server.
    final Javalin javalin = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config.enableCorsForAllOrigins();
    });
    bindings.registerRoutes(javalin);
    javalin.start(configuration.HTTP_PORT);
  }
}
