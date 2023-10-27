package gov.nasa.jpl.aerie.merlin.worker;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gov.nasa.jpl.aerie.merlin.server.config.PostgresStore;
import gov.nasa.jpl.aerie.merlin.server.config.Store;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresMissionModelRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresPlanRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresResultsCellRepository;
import gov.nasa.jpl.aerie.merlin.server.services.LocalMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.services.LocalPlanService;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import gov.nasa.jpl.aerie.merlin.worker.capabilities.HandleSimulationCapability;
import gov.nasa.jpl.aerie.merlin.worker.capabilities.HandleValidationCapability;
import gov.nasa.jpl.aerie.merlin.worker.capabilities.ListenSimulationCapability;
import gov.nasa.jpl.aerie.merlin.worker.capabilities.ListenValidationCapability;
import gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresSimulationNotificationPayload;
import gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresValidationNotificationPayload;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

public final class MerlinWorkerAppDriver {

  private static final Logger logger = LoggerFactory.getLogger(MerlinWorkerAppDriver.class);

  public static void main(String[] args) throws InterruptedException {
    final var app = Javalin.create().start(8080);
    app.get("/health", ctx -> ctx.status(200));

    final var configuration = loadConfiguration();
    final var hikariDataSource = getHikariDataSource(configuration);

    final var stores = new Stores(
        new PostgresPlanRepository(hikariDataSource),
        new PostgresMissionModelRepository(hikariDataSource),
        new PostgresResultsCellRepository(hikariDataSource));

    final var missionModelController = new LocalMissionModelService(
        configuration.merlinFileStore(),
        stores.missionModels(),
        configuration.untruePlanStart()
    );
    final var planController = new LocalPlanService(stores.plans());
    final var simulationAgent = new SynchronousSimulationAgent(
        planController,
        missionModelController,
        configuration.simulationProgressPollPeriodMillis());

    final var simulationNotificationQueue = new LinkedBlockingQueue<PostgresSimulationNotificationPayload>();
    final var validationNotificationQueue = new LinkedBlockingQueue<PostgresValidationNotificationPayload>();

    final var targetThreadCount = configuration.validationThreadEnabled() ? 4 : 2;

    try (var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(targetThreadCount)) {

      // start simulation listener thread
      final var simulationListenAction = new ListenSimulationCapability(hikariDataSource, simulationNotificationQueue);
      final var simulationListenThread = simulationListenAction.registerListener();

      // start simulation handler thread
      final var simulationHandleAction = new HandleSimulationCapability(
          hikariDataSource,
          simulationNotificationQueue,
          stores,
          simulationAgent
      );
      final var simulationHandleThread = simulationHandleAction.registerHandler();

      executor.submit(simulationListenThread);
      executor.submit(simulationHandleThread);

      // conditionally start validation listener and handler threads
      if (configuration.validationThreadEnabled()) {
        final var validationListenAction = new ListenValidationCapability(hikariDataSource, validationNotificationQueue);
        final var validationListenThread = validationListenAction.registerListener();

        final var validationHandleAction = new HandleValidationCapability(
            hikariDataSource,
            validationNotificationQueue,
            missionModelController);
        final var validationHandleThread = new Thread(validationHandleAction::registerHandler);

        executor.submit(validationListenThread);
        executor.submit(validationHandleThread);
      }

      // threads should never stop running, so we busy wait on active count to detect if any threads
      // have failed. if any threads failed, we kill the rest so the merlin-worker process exits and restarts
      while (executor.getActiveCount() == targetThreadCount) {
        Thread.sleep(1000);
      }
      executor.shutdownNow();
      app.close();
    }
  }

  private static HikariDataSource getHikariDataSource(WorkerAppConfiguration configuration) {
    final var store = configuration.store();

    if (!(store instanceof final PostgresStore postgresStore)) {
      throw new UnexpectedSubtypeError(Store.class, store);
    }

    final var hikariConfig = new HikariConfig();
    hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
    hikariConfig.addDataSourceProperty("serverName", postgresStore.server());
    hikariConfig.addDataSourceProperty("portNumber", postgresStore.port());
    hikariConfig.addDataSourceProperty("databaseName", postgresStore.database());
    hikariConfig.addDataSourceProperty("applicationName", "Merlin Server");
    hikariConfig.setUsername(postgresStore.user());
    hikariConfig.setPassword(postgresStore.password());
    hikariConfig.setMaximumPoolSize(configuration.validationThreadEnabled() ? 4 : 2);

    hikariConfig.setConnectionInitSql("set time zone 'UTC'");

    return new HikariDataSource(hikariConfig);
  }

  private static String getEnv(final String key, final String fallback){
    final var env = System.getenv(key);
    return env == null ? fallback : env;
  }

  private static WorkerAppConfiguration loadConfiguration() {
    return new WorkerAppConfiguration(
        Path.of(getEnv("MERLIN_WORKER_LOCAL_STORE", "/usr/src/app/merlin_file_store")),
        new PostgresStore(getEnv("MERLIN_WORKER_DB_SERVER", "postgres"),
                          getEnv("MERLIN_WORKER_DB_USER", ""),
                          Integer.parseInt(getEnv("MERLIN_WORKER_DB_PORT", "5432")),
                          getEnv("MERLIN_WORKER_DB_PASSWORD", ""),
                          getEnv("MERLIN_WORKER_DB", "aerie_merlin")),
        Integer.parseInt(getEnv("SIMULATION_PROGRESS_POLL_PERIOD_MILLIS", "5000")),
        Instant.parse(getEnv("UNTRUE_PLAN_START", "")),
        Boolean.parseBoolean(getEnv("ENABLE_VALIDATION_THREAD", "false"))
    );
  }
}
