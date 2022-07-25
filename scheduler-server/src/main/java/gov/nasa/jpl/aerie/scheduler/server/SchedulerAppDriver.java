package gov.nasa.jpl.aerie.scheduler.server;

import com.impossibl.postgres.jdbc.PGDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gov.nasa.jpl.aerie.scheduler.server.config.AppConfiguration;
import gov.nasa.jpl.aerie.scheduler.server.config.InMemoryStore;
import gov.nasa.jpl.aerie.scheduler.server.config.PlanOutputMode;
import gov.nasa.jpl.aerie.scheduler.server.config.PostgresStore;
import gov.nasa.jpl.aerie.scheduler.server.config.Store;
import gov.nasa.jpl.aerie.scheduler.server.http.SchedulerBindings;
import gov.nasa.jpl.aerie.scheduler.server.mocks.InMemoryResultsCellRepository;
import gov.nasa.jpl.aerie.scheduler.server.mocks.InMemorySpecificationRepository;
import gov.nasa.jpl.aerie.scheduler.server.remotes.ResultsCellRepository;
import gov.nasa.jpl.aerie.scheduler.server.remotes.SpecificationRepository;
import gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.PostgresResultsCellRepository;
import gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.PostgresSpecificationRepository;
import gov.nasa.jpl.aerie.scheduler.server.services.CachedSchedulerService;
import gov.nasa.jpl.aerie.scheduler.server.services.GenerateSchedulingLibAction;
import gov.nasa.jpl.aerie.scheduler.server.services.GraphQLMerlinService;
import gov.nasa.jpl.aerie.scheduler.server.services.LocalSpecificationService;
import gov.nasa.jpl.aerie.scheduler.server.services.MissionModelService;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleAction;
import gov.nasa.jpl.aerie.scheduler.server.services.SchedulingDSLCompilationService;
import gov.nasa.jpl.aerie.scheduler.server.services.SynchronousSchedulerAgent;
import gov.nasa.jpl.aerie.scheduler.server.services.ThreadedSchedulerAgent;
import gov.nasa.jpl.aerie.scheduler.server.services.UnexpectedSubtypeError;
import io.javalin.Javalin;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * scheduler service entry point class; services pending scheduler requests until terminated
 */
public final class SchedulerAppDriver {

  /**
   * scheduler service entry point; services pending scheduler requests until terminated
   *
   * reads configuration options from the environment (if available, otherwise uses hardcoded defaults) to control how
   * the scheduler connects to its data stores or services scheduling requests
   *
   * this method never naturally returns; it will service requests until externally terminated (or exception)
   *
   * @param args command-line args passed to the executable
   *     [...] all arguments are ignored
   */
  public static void main(final String[] args) {
    //load the service configuration options
    final var config = loadConfiguration();

    final var merlinService = new GraphQLMerlinService(config.merlinGraphqlURI());

    final SchedulingDSLCompilationService schedulingDSLCompilationService;
    try {
      schedulingDSLCompilationService = new SchedulingDSLCompilationService();
    } catch (IOException e) {
      throw new Error("Failed to start SchedulingDSLCompilationService", e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(schedulingDSLCompilationService::close));

    final var stores = loadStores(config, schedulingDSLCompilationService, merlinService);

    //create objects in each service abstraction layer (mirroring MerlinApp)
    final var specificationService = new LocalSpecificationService(stores.specifications());
    final var scheduleAgent = ThreadedSchedulerAgent.spawn(
        "scheduler-agent",
        new SynchronousSchedulerAgent(specificationService,
                                      merlinService,
                                      config.merlinFileStore(),
                                      config.missionRuleJarPath(),
                                      config.outputMode()));
    final var schedulerService = new CachedSchedulerService(stores.results(), scheduleAgent);
    final var scheduleAction = new ScheduleAction(specificationService, schedulerService);

    final var generateSchedulingLibAction = new GenerateSchedulingLibAction(merlinService);

    //establish bindings to the service layers
    final var bindings = new SchedulerBindings(schedulerService, scheduleAction, generateSchedulingLibAction);

    //default javalin jetty server has a QueuedThreadPool with maxThreads to 250
    final var server = new Server(new QueuedThreadPool(250));
    final var connector = new ServerConnector(server);
    connector.setPort(config.httpPort());
    //set idle timeout to be equal to the idle timeout of hasura
    connector.setIdleTimeout(180000);
    server.addBean(new LowResourceMonitor(server));
    server.insertHandler(new StatisticsHandler());
    server.setConnectors(new Connector[]{connector});
    //configure the http server (the consumer lambda overlays additional config on the input javalinConfig)
    final var javalin = Javalin.create(javalinConfig -> {
      javalinConfig.showJavalinBanner = false;
      if (config.enableJavalinDevLogging()) javalinConfig.enableDevLogging();
      javalinConfig.enableCorsForAllOrigins(); //TODO: probably don't want literally any cross-origin request...
      javalinConfig.registerPlugin(bindings);
      javalinConfig.server(() -> server);
      //TODO: exception handling (shxould elevate/reuse from MerlinApp for consistency?)
    });

    //start the http server and handle requests as configured above
    javalin.start(config.httpPort());
  }

  private record Stores(SpecificationRepository specifications, ResultsCellRepository results) { }

  private static Stores loadStores(
      final AppConfiguration config,
      final SchedulingDSLCompilationService schedulingDSLCompilationService,
      final MissionModelService missionModelService) {
    final var store = config.store();
    if (store instanceof final PostgresStore pgStore) {
      final var pgDataSource = new PGDataSource();
      pgDataSource.setServerName(pgStore.server());
      pgDataSource.setPortNumber(pgStore.port());
      pgDataSource.setDatabaseName(pgStore.database());
      pgDataSource.setApplicationName("Scheduler Server");

      final var hikariConfig = new HikariConfig();
      hikariConfig.setUsername(pgStore.user());
      hikariConfig.setPassword(pgStore.password());
      hikariConfig.setDataSource(pgDataSource);

      final var hikariDataSource = new HikariDataSource(hikariConfig);

      return new Stores(
          new PostgresSpecificationRepository(hikariDataSource, schedulingDSLCompilationService, missionModelService),
          new PostgresResultsCellRepository(hikariDataSource));
    } else if (store instanceof InMemoryStore) {
      final var inMemorySchedulerRepository = new InMemorySpecificationRepository();
      return new Stores(
          inMemorySchedulerRepository,
          new InMemoryResultsCellRepository(inMemorySchedulerRepository));

    } else {
      throw new UnexpectedSubtypeError(Store.class, store);
    }
  }

  /**
   * fetch the value of the requested environment variable if available, otherwise return the given fallback
   *
   * @param key the name of the environment variable to fetch
   * @param fallback the value to use in case the requested environment variable does not exist in the environment
   * @return the value of the requested environment variable if it exists in the environment (even if it is the empty
   *     string), otherwise the specified fallback value
   */
  private static String getEnv(final String key, final String fallback) {
    final var env = System.getenv(key);
    return env == null ? fallback : env;
  }

  /**
   * collects configuration options from the environment
   *
   * any options not specified in the input stream fall back to the hard-coded defaults here
   *
   * @return a complete configuration object reflecting choices elected in the environment or the defaults
   */
  private static AppConfiguration loadConfiguration() {
    final var logger = LoggerFactory.getLogger(SchedulerAppDriver.class);
    return new AppConfiguration(
        Integer.parseInt(getEnv("SCHEDULER_PORT", "27185")),
        logger.isDebugEnabled(),
        Path.of(getEnv("SCHEDULER_LOCAL_STORE", "/usr/src/app/scheduler_file_store")),
        new PostgresStore(getEnv("SCHEDULER_DB_SERVER", "postgres"),
                          getEnv("SCHEDULER_DB_USER", "aerie"),
                          Integer.parseInt(getEnv("SCHEDULER_DB_PORT", "5432")),
                          getEnv("SCHEDULER_DB_PASSWORD", "aerie"),
                          getEnv("SCHEDULER_DB", "aerie_scheduler")),
        URI.create(getEnv("MERLIN_GRAPHQL_URL", "http://localhost:8080/v1/graphql")),
        Path.of(getEnv("MERLIN_LOCAL_STORE", "/usr/src/app/merlin_file_store")),
        Path.of(getEnv("SCHEDULER_RULES_JAR", "/usr/src/app/merlin_file_store/scheduler_rules.jar")),
        PlanOutputMode.valueOf((getEnv("SCHEDULER_OUTPUT_MODE", "CreateNewOutputPlan")))
    );
  }
}
