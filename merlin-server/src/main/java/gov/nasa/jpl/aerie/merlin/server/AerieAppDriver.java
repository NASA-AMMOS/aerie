package gov.nasa.jpl.aerie.merlin.server;

import com.impossibl.postgres.jdbc.PGDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gov.nasa.jpl.aerie.merlin.server.config.AppConfiguration;
import gov.nasa.jpl.aerie.merlin.server.config.InMemoryStore;
import gov.nasa.jpl.aerie.merlin.server.config.PostgresStore;
import gov.nasa.jpl.aerie.merlin.server.config.Store;
import gov.nasa.jpl.aerie.merlin.server.http.LocalAppExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.MerlinBindings;
import gov.nasa.jpl.aerie.merlin.server.http.MissionModelExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.MissionModelRepositoryExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.mocks.InMemoryMissionModelRepository;
import gov.nasa.jpl.aerie.merlin.server.mocks.InMemoryPlanRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.InMemoryResultsCellRepository;
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
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.ThreadedSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.TypescriptCodeGenerationService;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import io.javalin.Javalin;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class AerieAppDriver {

  public static void main(final String[] args) {
    // Fetch application configuration properties.
    final var configuration = loadConfiguration();
    final var stores = loadStores(configuration);

    final var typescriptCodeGenerationService = new TypescriptCodeGenerationService();

    final ConstraintsDSLCompilationService constraintsDSLCompilationService;
    try {
      constraintsDSLCompilationService = new ConstraintsDSLCompilationService(typescriptCodeGenerationService);
    } catch (IOException e) {
      throw new Error("Failed to start ConstraintsDSLCompilationService", e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(constraintsDSLCompilationService::close));

    // Assemble the core non-web object graph.
    final var missionModelController = new LocalMissionModelService(configuration.merlinFileStore(), stores.missionModels());
    final var planController = new LocalPlanService(stores.plans());
    final var simulationAgent = ThreadedSimulationAgent.spawn(
        "simulation-agent",
        new SynchronousSimulationAgent(planController, missionModelController));
    final var simulationController = new CachedSimulationService(stores.results(), simulationAgent);
    final var simulationAction = new GetSimulationResultsAction(
        planController,
        missionModelController,
        simulationController,
        constraintsDSLCompilationService,
        configuration.useNewConstraintPipeline()
    );
    final var generateSchedulingLibAction = new GenerateConstraintsLibAction(typescriptCodeGenerationService);
    final var merlinBindings = new MerlinBindings(
        missionModelController,
        planController,
        simulationAction,
        generateSchedulingLibAction
    );

    // Configure an HTTP server.
    final var javalin = Javalin.create(config -> {
      config.showJavalinBanner = false;
      if (configuration.enableJavalinDevLogging()) config.enableDevLogging();
      config.enableCorsForAllOrigins();
      config.registerPlugin(merlinBindings);
      config.registerPlugin(new LocalAppExceptionBindings());
      config.registerPlugin(new MissionModelRepositoryExceptionBindings());
      config.registerPlugin(new MissionModelExceptionBindings());
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
    } else if (store instanceof InMemoryStore c) {
      final var inMemoryPlanRepository = new InMemoryPlanRepository();
      return new Stores(
          inMemoryPlanRepository,
          new InMemoryMissionModelRepository(),
          new InMemoryResultsCellRepository(inMemoryPlanRepository));

    } else {
      throw new UnexpectedSubtypeError(Store.class, store);
    }
  }

  private static final String getEnv(final String key, final String fallback){
    final var env = System.getenv(key);
    return env == null ? fallback : env;
  }

  private static AppConfiguration loadConfiguration() {
    final var logger = LoggerFactory.getLogger(AerieAppDriver.class);
    return new AppConfiguration(
        Integer.parseInt(getEnv("MERLIN_PORT","27183")),
        logger.isDebugEnabled(),
        Path.of(getEnv("MERLIN_LOCAL_STORE","/usr/src/app/merlin_file_store")),
        new PostgresStore(getEnv("MERLIN_DB_SERVER","postgres"),
                          getEnv("MERLIN_DB_USER","aerie"),
                          Integer.parseInt(getEnv("MERLIN_DB_PORT","5432")),
                          getEnv("MERLIN_DB_PASSWORD","aerie"),
                          getEnv("MERLIN_DB","aerie_merlin")),
        Boolean.parseBoolean(getEnv("MERLIN_USE_NEW_CONSTRAINT_PIPELINE", "false"))
    );
  }
}
