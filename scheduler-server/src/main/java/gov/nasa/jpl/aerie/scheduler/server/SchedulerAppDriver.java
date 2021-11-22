package gov.nasa.jpl.aerie.scheduler.server;

import gov.nasa.jpl.aerie.scheduler.server.config.AppConfiguration;
import gov.nasa.jpl.aerie.scheduler.server.config.AppConfigurationJsonMapper;
import gov.nasa.jpl.aerie.scheduler.server.config.JavalinLoggingState;
import gov.nasa.jpl.aerie.scheduler.server.http.SchedulerBindings;
import gov.nasa.jpl.aerie.scheduler.server.services.GraphQLMerlinService;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleAction;
import gov.nasa.jpl.aerie.scheduler.server.services.SynchronousSchedulerAgent;
import gov.nasa.jpl.aerie.scheduler.server.services.UncachedSchedulerService;
import io.javalin.Javalin;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * scheduler service entry point class; services pending scheduler requests until terminated
 */
public final class SchedulerAppDriver {
  /**
   * resource path to the default scheduler configuration (if not provided on the command line)
   */
  private static final String defaultConfigResourceName = "scheduler_config.json";

  /**
   * scheduler service entry point; services pending scheduler requests until terminated
   *
   * reads configuration options from specified (or default) config file to control how the scheduler connects to
   * its data stores or services scheduling requests
   *
   * this method never naturally returns; it will service requests until externally terminated (or exception)
   *
   * @param args command-line args passed to the executable, used to set up the configuration:
   *     [0] the file path to a configuration json file to load (optional, otherwise uses jar-baked default)
   *     [...] remaining arguments are ignored
   */
  public static void main(final String[] args) throws IOException {
    //load the service configuration options
    final var appConfig = readConfiguration(openConfiguration(args));

    //create objects in each service abstraction layer (mirroring MerlinApp)
    final var merlinService = new GraphQLMerlinService();
    final var scheduleAgent = new SynchronousSchedulerAgent(merlinService);
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
   * opens the input stream to load configuration options from, based on command-line arguments
   *
   * if an input configuration file is specified directly, that file is used to load configuration options; otherwise,
   * a built-in default file in the server jar resources is used
   *
   * raises exception if the chosen file cannot be read (it does not fall-back)
   *
   * @param args the command-line args passed to the executable, used to locate the configuration file:
   *     [0] path to a configuration json file to load (optional, otherwise uses jar-baked default)
   *     [...] remaining arguments are ignored
   * @return opened configuration file chosen via the command line arguments (possibly the jar-baked default)
   */
  private static InputStream openConfiguration(final String[] args) throws IOException {
    final InputStream configStream;
    if (args.length > 0) {
      //use args[0] specified file as config source (and ignore rest of args)
      configStream = Files.newInputStream(Path.of(args[0]));
    } else { //args.length==0
      //no input file; so load from baked-in resource
      configStream = SchedulerAppDriver.class.getResourceAsStream(defaultConfigResourceName);
      if (configStream == null) {
        throw new IOException("Could not locate default configuration resource: " + defaultConfigResourceName);
      }
    }
    return configStream;
  }

  /**
   * collects configuration options from the provided json configuration stream
   *
   * any options not specified in the input stream fall back to their parser-level defaults
   *
   * @param configStream the input stream to read the json-formatted configuration options from
   * @return a complete configuration object reflecting choices elected in the input stream or the defaults
   */
  private static AppConfiguration readConfiguration(final InputStream configStream) {
    // Read and process the configuration source.
    final var config = (JsonObject) (Json.createReader(configStream).readValue());
    return AppConfigurationJsonMapper.fromJson(config);
  }


}
