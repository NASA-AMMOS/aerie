package gov.nasa.jpl.aerie.merlin.server;

import com.impossibl.postgres.jdbc.PGDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gov.nasa.jpl.aerie.merlin.server.config.AppConfiguration;
import gov.nasa.jpl.aerie.merlin.server.config.PostgresStore;
import gov.nasa.jpl.aerie.merlin.server.config.Store;
import gov.nasa.jpl.aerie.merlin.server.http.LocalAppExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.MerlinBindings;
import gov.nasa.jpl.aerie.merlin.server.http.MissionModelRepositoryExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresMissionModelRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresPlanRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresResultsCellRepository;
import gov.nasa.jpl.aerie.merlin.server.services.CachedSimulationService;
import gov.nasa.jpl.aerie.merlin.server.services.ConstraintsDSLCompilationService;
import gov.nasa.jpl.aerie.merlin.server.services.GenerateConstraintsLibAction;
import gov.nasa.jpl.aerie.merlin.server.services.GetSimulationResultsAction;
import gov.nasa.jpl.aerie.merlin.server.services.LocalMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.services.LocalPlanService;
import gov.nasa.jpl.aerie.merlin.server.services.TypescriptCodeGenerationServiceAdapter;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import io.javalin.Javalin;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

public final class AerieAppDriver {

  public static void main(final String[] args) {
    // Fetch application configuration properties.
    final var configuration = loadConfiguration();
    final var stores = loadStores(configuration);

    final var missionModelController = new LocalMissionModelService(
        configuration.merlinFileStore(),
        stores.missionModels(),
        configuration.untruePlanStart());
    final var planController = new LocalPlanService(stores.plans());

    final var typescriptCodeGenerationService = new TypescriptCodeGenerationServiceAdapter(missionModelController, planController);

    final ConstraintsDSLCompilationService constraintsDSLCompilationService;
    try {
      constraintsDSLCompilationService = new ConstraintsDSLCompilationService(typescriptCodeGenerationService);
    } catch (IOException e) {
      throw new Error("Failed to start ConstraintsDSLCompilationService", e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(constraintsDSLCompilationService::close));

    // Assemble the core non-web object graph.
//<<<<<<< HEAD
    final var simulationController = new CachedSimulationService(stores.results());
//=======
//    final var simulationAgent = ThreadedSimulationAgent.spawn(
//        "simulation-agent",
//        new SynchronousSimulationAgent(planController, missionModelController, false));
//    final var simulationController = new CachedSimulationService(simulationAgent, stores.results());
//>>>>>>> prototype/excise-resources-from-sim-engine
    final var simulationAction = new GetSimulationResultsAction(
        planController,
        missionModelController,
        simulationController,
        constraintsDSLCompilationService
    );
    final var generateConstraintsLibAction = new GenerateConstraintsLibAction(typescriptCodeGenerationService);
    final var merlinBindings = new MerlinBindings(
        missionModelController,
        planController,
        simulationAction,
        generateConstraintsLibAction
    );
    // Configure an HTTP server.
    //default javalin jetty server has a QueuedThreadPool with maxThreads to 250
    final var server = new Server(new QueuedThreadPool(250));
    final var connector = new ServerConnector(server);
    connector.setPort(configuration.httpPort());
    //set idle timeout to be equal to the idle timeout of hasura
    connector.setIdleTimeout(180000);
    server.addBean(new LowResourceMonitor(server));
    server.insertHandler(new StatisticsHandler());
    server.setConnectors(new Connector[]{connector});
    final var javalin = Javalin.create(config -> {
      config.showJavalinBanner = false;
      if (configuration.enableJavalinDevLogging()) config.plugins.enableDevLogging();
      config.plugins.enableCors(cors -> cors.add(it -> it.anyHost()));
      config.plugins.register(merlinBindings);
      config.plugins.register(new LocalAppExceptionBindings());
      config.plugins.register(new MissionModelRepositoryExceptionBindings());
      config.jetty.server(() -> server);
    });

    // Start the HTTP server.
    javalin.start(configuration.httpPort());
  }

  private record Stores (PlanRepository plans, MissionModelRepository missionModels, ResultsCellRepository results) {}

  private static Stores loadStores(final AppConfiguration config) {
    final var store = config.store();
    if (store instanceof PostgresStore c) {
      final var pgDataSource = new PGDataSource();
      pgDataSource.setServerName(c.server());
      pgDataSource.setPortNumber(c.port());
      pgDataSource.setDatabaseName(c.database());
      pgDataSource.setApplicationName("Merlin Server");

      final var hikariConfig = new HikariConfig();
      hikariConfig.setUsername(c.user());
      hikariConfig.setPassword(c.password());
      hikariConfig.setDataSource(pgDataSource);

      final var hikariDataSource = new HikariDataSource(hikariConfig);

      return new Stores(
          new PostgresPlanRepository(hikariDataSource),
          new PostgresMissionModelRepository(hikariDataSource),
          new PostgresResultsCellRepository(hikariDataSource));
    } else {
      throw new UnexpectedSubtypeError(Store.class, store);
    }
  }

  private static String getEnv(final String key, final String fallback) {
    final var env = System.getenv(key);
    return env == null ? fallback : env;
  }

  private static AppConfiguration loadConfiguration() {
    final var logger = LoggerFactory.getLogger(AerieAppDriver.class);
    return new AppConfiguration(
        Integer.parseInt(getEnv("MERLIN_PORT", "27183")),
        logger.isDebugEnabled(),
        Path.of(getEnv("MERLIN_LOCAL_STORE", "/usr/src/app/merlin_file_store")),
        new PostgresStore(getEnv("MERLIN_DB_SERVER", "postgres"),
                          getEnv("MERLIN_DB_USER", ""),
                          Integer.parseInt(getEnv("MERLIN_DB_PORT", "5432")),
                          getEnv("MERLIN_DB_PASSWORD", ""),
                          getEnv("MERLIN_DB", "aerie_merlin")),
        Instant.parse(getEnv("UNTRUE_PLAN_START", ""))
    );
  }
}
