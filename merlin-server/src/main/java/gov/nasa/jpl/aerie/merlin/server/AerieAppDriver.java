package gov.nasa.jpl.aerie.merlin.server;

import com.mongodb.client.MongoClients;
import gov.nasa.jpl.aerie.merlin.driver.json.JsonEncoding;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.config.AppConfiguration;
import gov.nasa.jpl.aerie.merlin.server.config.AppConfigurationJsonMapper;
import gov.nasa.jpl.aerie.merlin.server.config.MongoStore;
import gov.nasa.jpl.aerie.merlin.server.services.CachedSimulationService;
import gov.nasa.jpl.aerie.merlin.server.services.GetSimulationResultsAction;
import gov.nasa.jpl.aerie.merlin.server.services.LocalAdaptationService;
import gov.nasa.jpl.aerie.merlin.server.http.AdaptationExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.AdaptationRepositoryExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.LocalAppExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.remotes.RemoteAdaptationRepository;
import gov.nasa.jpl.aerie.merlin.server.http.MerlinBindings;
import gov.nasa.jpl.aerie.merlin.server.remotes.RemotePlanRepository;
import gov.nasa.jpl.aerie.merlin.server.services.LocalPlanService;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.ThreadedSimulationAgent;
import io.javalin.Javalin;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class AerieAppDriver {
  private static final Logger log = Logger.getLogger(AerieAppDriver.class.getName());

  public static void main(final String[] args) {
    // Fetch application configuration properties.
    final var configuration = loadConfiguration(args);

    if (!(configuration.store() instanceof MongoStore store)) {
      throw new RuntimeException(
          "Aerie can currently only run on MongoDB, but it was configured with a %s"
              .formatted(configuration.store().getClass()));
    }

    // Assemble the core non-web object graph.
    final var mongoDatabase = MongoClients
        .create(store.uri().toString())
        .getDatabase(store.database());
    final var planRepository = new RemotePlanRepository(
        mongoDatabase,
        store.planCollection(),
        store.activityCollection());
    final var adaptationRepository = new RemoteAdaptationRepository(
        mongoDatabase,
        store.adaptationCollection());

    final var missionModelConfigGet = makeMissionModelConfigSupplier(configuration);
    final var adaptationController = new LocalAdaptationService(missionModelConfigGet, adaptationRepository);
    final var planController = new LocalPlanService(planRepository, adaptationController);

    final var simulationAgent = ThreadedSimulationAgent.spawn(
        "simulation-agent",
        new SynchronousSimulationAgent(planController, adaptationController));

    final var simulationAction = new GetSimulationResultsAction(
        planController,
        adaptationController,
        new CachedSimulationService(
            new MongoResultsCellRepository(mongoDatabase.getCollection(store.simulationResultsCollection())),
            simulationAgent));

    // Configure an HTTP server.
    final var javalin = Javalin.create(config -> {
      config.showJavalinBanner = false;
      if (configuration.javalinLogging().isEnabled()) config.enableDevLogging();
      config
          .enableCorsForAllOrigins()
          .registerPlugin(new MerlinBindings(planController, adaptationController, simulationAction))
          .registerPlugin(new LocalAppExceptionBindings())
          .registerPlugin(new AdaptationRepositoryExceptionBindings())
          .registerPlugin(new AdaptationExceptionBindings());
    });

    // Start the HTTP server.
    javalin.start(configuration.httpPort());
  }

  /** Allows for mission model configuration to be loaded when an adaptation is loaded. */
  private static Supplier<SerializedValue> makeMissionModelConfigSupplier(final AppConfiguration configuration)
  {
    // Try to deserialize JSON configuration to a serialized configuration
    return () -> configuration.missionModelConfigPath()
        .map(p -> {
          try {
            final var fs = new FileInputStream(p);
            final var sv = JsonEncoding.decode(Json.createReader(fs).read());
            log.info(String.format("Successfully loaded mission model configuration from: %s", p));
            return sv;
          } catch (final FileNotFoundException ex) {
            log.warning(String.format("Unable to find mission model configuration path: \"%s\". Simulations will receive an empty set of configuration arguments.", p));
            return SerializedValue.NULL;
          }
        })
        .orElseGet(() -> {
          log.warning("No mission model configuration specified in server configuration. Simulations will receive an empty set of configuration arguments.");
          return SerializedValue.NULL;
        });
  }

  private static AppConfiguration loadConfiguration(final String[] args) {
    // Determine where we're getting our configuration from.
    final InputStream configStream;
    if (args.length > 0) {
      try {
        configStream = Files.newInputStream(Path.of(args[0]));
      } catch (final IOException ex) {
        log.warning(String.format("Configuration file \"%s\" could not be loaded: %s", args[0], ex.getMessage()));
        System.exit(1);
        throw new Error(ex);
      }
    } else {
      configStream = AerieAppDriver.class.getResourceAsStream("config.json");
    }

    // Read and process the configuration source.
    final var config = (JsonObject)(Json.createReader(configStream).readValue());
    return AppConfigurationJsonMapper.fromJson(config).orElseThrow();
  }
}
