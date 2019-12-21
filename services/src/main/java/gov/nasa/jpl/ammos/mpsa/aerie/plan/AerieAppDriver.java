package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import com.mongodb.client.MongoClients;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.PlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.http.PlanBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.RemoteAdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.RemotePlanRepository;
import io.javalin.Javalin;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AerieAppDriver {
  public static void main(final String[] args) {
    // Fetch application configuration properties.
    final AppConfiguration configuration = loadConfiguration(args);

    // Assemble the core non-web object graph.
    final PlanRepository planRepository;
    {
      final var mongoDatabase = MongoClients
          .create(configuration.MONGO_URI.toString())
          .getDatabase(configuration.MONGO_DATABASE);

      planRepository = new RemotePlanRepository(
          mongoDatabase,
          configuration.MONGO_PLAN_COLLECTION,
          configuration.MONGO_ACTIVITY_COLLECTION);
    }

    final AdaptationService adaptationService = new RemoteAdaptationService(configuration.ADAPTATION_URI);
    final IPlanController controller = new PlanController(planRepository, adaptationService);

    // Configure an HTTP server.
    final Javalin javalin = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config
          .enableCorsForAllOrigins()
          .registerPlugin(new PlanBindings(controller));
    });

    // Start the HTTP server.
    javalin.start(configuration.HTTP_PORT);
  }

  private static AppConfiguration loadConfiguration(final String[] args) {
    // Determine where we're getting our configuration from.
    final InputStream configStream;
    if (args.length > 0) {
      try {
        configStream = Files.newInputStream(Path.of(args[0]));
      } catch (final IOException ex) {
        System.err.println(String.format("Configuration file \"%s\" could not be loaded: %s", args[0], ex.getMessage()));
        System.exit(1);
        throw new Error(ex);
      }
    } else {
      configStream = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("gov/nasa/jpl/ammos/mpsa/aerie/plan/config.json");
    }

    // Read and process the configuration source.
    final JsonObject config = (JsonObject)(Json.createReader(configStream).readValue());
    return AppConfiguration.parseProperties(config);
  }
}
