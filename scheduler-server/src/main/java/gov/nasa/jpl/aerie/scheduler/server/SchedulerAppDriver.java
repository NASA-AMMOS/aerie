package gov.nasa.jpl.aerie.scheduler.server;

import gov.nasa.jpl.aerie.scheduler.server.config.AppConfiguration;
import gov.nasa.jpl.aerie.scheduler.server.config.JavalinLoggingState;
import gov.nasa.jpl.aerie.scheduler.server.http.SchedulerBindings;
import gov.nasa.jpl.aerie.scheduler.server.services.GraphQLMerlinService;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleAction;
import gov.nasa.jpl.aerie.scheduler.server.services.SynchronousSchedulerAgent;
import gov.nasa.jpl.aerie.scheduler.server.services.UncachedSchedulerService;
import io.javalin.Javalin;

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
    final var appConfig = loadConfiguration();

    //create objects in each service abstraction layer (mirroring MerlinApp)
    final var merlinService = new GraphQLMerlinService(appConfig.merlinGraphqlURI());
    final var scheduleAgent = new SynchronousSchedulerAgent(
        merlinService, appConfig.missionModelJarsDir(), appConfig.missionRuleJarPath());
    final var schedulerService = new UncachedSchedulerService(scheduleAgent);
    final var scheduleAction = new ScheduleAction(merlinService, schedulerService);

    //establish bindings to the service layers
    final var bindings = new SchedulerBindings(schedulerService, scheduleAction);

    //configure the http server (the consumer lambda overlays additional config on the input javalinConfig)
    final var javalin = Javalin.create(javalinConfig -> {
      javalinConfig.showJavalinBanner = false;
      if (appConfig.javalinLogging() == JavalinLoggingState.Enabled) {
        javalinConfig.enableDevLogging();
      }
      javalinConfig
          .enableCorsForAllOrigins() //TODO: probably don't want literally any cross-origin request...
          .registerPlugin(bindings);
      //TODO: exception handling (should elevate/reuse from MerlinApp for consistency?)
    });

    //start the http server and handle requests as configured above
    javalin.start(appConfig.httpPort());
  }

  /**
   * collects configuration options from the environment
   *
   * any options not specified in the input stream fall back to the hard-coded defaults here
   *
   * @return a complete configuration object reflecting choices elected in the environment or the defaults
   */
  private static AppConfiguration loadConfiguration() {
    return new AppConfiguration(
        Integer.parseInt(getEnvOrFallback("SCHED_PORT", "27193")),
        Boolean.parseBoolean(getEnvOrFallback("SCHED_LOGGING", "true")) ?
            JavalinLoggingState.Enabled : JavalinLoggingState.Disabled,
        URI.create(getEnvOrFallback("MERLIN_GRAPHQL_URL", "http://localhost:8080/v1/graphql")),
        Path.of(getEnvOrFallback("MERLIN_LOCAL_STORE", "/usr/src/app/merlin_file_store")),
        Path.of(getEnvOrFallback("SCHED_RULES_JAR", "/usr/src/app/merlin_file_store/sched_rules.jar")));
  }

  /**
   * fetch the value of the requested environment variable if available, otherwise return the given fallback
   *
   * @param key the name of the environment variable to fetch
   * @param fallback the value to use in case the requested environment variable does not exist in the environment
   * @return the value of the requested environment variable if it exists in the environment (even if it is the empty
   *     string), otherwise the specified fallback value
   */
  private static final String getEnvOrFallback(final String key, final String fallback) {
    final var env = System.getenv(key);
    return env == null ? fallback : env;
  }

}
