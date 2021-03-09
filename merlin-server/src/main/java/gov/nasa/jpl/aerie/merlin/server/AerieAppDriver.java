package gov.nasa.jpl.aerie.merlin.server;

import com.mongodb.client.MongoClients;
import gov.nasa.jpl.aerie.merlin.server.services.LocalAdaptationService;
import gov.nasa.jpl.aerie.merlin.server.http.AdaptationExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.AdaptationRepositoryExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.LocalAppExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.remotes.RemoteAdaptationRepository;
import gov.nasa.jpl.aerie.merlin.server.http.MerlinBindings;
import gov.nasa.jpl.aerie.merlin.server.remotes.RemotePlanRepository;
import gov.nasa.jpl.aerie.merlin.server.services.LocalPlanService;
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
    final var configuration = loadConfiguration(args);

    // Assemble the core non-web object graph.
    final var mongoDatabase = MongoClients
        .create(configuration.MONGO_URI.toString())
        .getDatabase(configuration.MONGO_DATABASE);
    final var planRepository = new RemotePlanRepository(
        mongoDatabase,
        configuration.MONGO_PLAN_COLLECTION,
        configuration.MONGO_ACTIVITY_COLLECTION);
    final var adaptationRepository = new RemoteAdaptationRepository(
        mongoDatabase,
        configuration.MONGO_ADAPTATION_COLLECTION);
    final var adaptationController = new LocalAdaptationService(adaptationRepository);
    final var planController = new LocalPlanService(planRepository, adaptationController);

    // Configure an HTTP server.
    final var javalin = Javalin.create(config -> {
      config.showJavalinBanner = false;
      if (configuration.enableJavalinLogging) config.enableDevLogging();
      config
          .enableCorsForAllOrigins()
          .registerPlugin(new MerlinBindings(planController, adaptationController))
          .registerPlugin(new LocalAppExceptionBindings())
          .registerPlugin(new AdaptationRepositoryExceptionBindings())
          .registerPlugin(new AdaptationExceptionBindings());
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
      configStream = AerieAppDriver.class.getResourceAsStream("config.json");
    }

    // Read and process the configuration source.
    final var config = (JsonObject)(Json.createReader(configStream).readValue());
    return AppConfiguration.parseProperties(config);
  }
}
