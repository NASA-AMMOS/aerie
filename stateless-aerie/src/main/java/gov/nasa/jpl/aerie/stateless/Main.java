package gov.nasa.jpl.aerie.stateless;

import gov.nasa.jpl.aerie.stateless.simulation.CanceledListener;
import gov.nasa.jpl.aerie.stateless.simulation.SimulationExtentConsumer;
import gov.nasa.jpl.aerie.stateless.simulation.SimulationResultsWriter;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationException;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.resources.InMemorySimulationResourceManager;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.*;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

public class Main {
  private static final String VERSION = "v2.16.0";
  private static final String FOOTER = "\nStateless Aerie "+VERSION;

  private static final Option HELP_OPTION = new Option("h", "help", false, "display this message and exit");

  private sealed interface Arguments {
    record SimulationArguments <Model> (
        MissionModel<Model> missionModel,
        Plan plan,
        boolean verbose,
        Optional<Path> outputFilePath,
        long extentUpdatePeriod
    ) implements Arguments {}
  }

  public static void main(String[] args) {
    final var command = args[0];

    switch (command.toLowerCase()) {
      case "simulate": {
        simulate(parseSimulationArgs(args));
        break;
      }
      case "-h":
      case "--help":
      default:
        displayTopLevelHelp();
        break;
    }
  }

  private static Arguments.SimulationArguments<?> parseSimulationArgs(String[] args) {
    final Path modelJarPath;
    final Path planJsonPath;
    final Optional<Path> configJsonPath;
    final boolean verbose;
    final Optional<Path> outputFilePath;
    final long extentUpdatePeriod;

    // Parse the command line arguments
    final Options simulationOptions = createSimulationOptions();
    try {
      checkForHelp(args, simulationOptions, "simulate", "Simulate a plan using the specified model and configuration");

      final CommandLineParser parser = new DefaultParser();
      final CommandLine cmd = parser.parse(simulationOptions, args);

      modelJarPath = cmd.getParsedOptionValue('m');
      planJsonPath = cmd.getParsedOptionValue('p');
      verbose = cmd.hasOption("verbose");
      // Parser sets unused fields to 'null'
      configJsonPath = cmd.getParsedOptionValue('s', Optional.empty());
      outputFilePath = cmd.getParsedOptionValue('f', Optional.empty());
      extentUpdatePeriod = cmd.getParsedOptionValue('i', 500L);
    } catch (ParseException e) {
      simulationOptions.addOption(HELP_OPTION);
      new HelpFormatter().printHelp(
          "stateless-aerie simulate",
          "Simulate a plan using the specified model and configuration",
          simulationOptions,
          FOOTER,
          true);
      System.exit(2);
      // The below is included as java doesn't recognize System.exit() as stopping the method,
      // which causes compilation methods when trying to use the values assigned above
      throw new RuntimeException(e);
    }

    // Parse the plan and simulation config files into a Plan object
    if (verbose) { System.out.println("Parsing plan "+planJsonPath+"..."); }
    final var plan = PlanJsonParser.parsePlan(planJsonPath);
    configJsonPath.ifPresent(path -> {
      if (verbose) { System.out.println("Parsing simulation configuration "+path+"..."); }
      PlanJsonParser.parseSimulationConfiguration(path, plan);
    });

    // Load the mission model
    try {
      if (verbose) { System.out.println("Loading mission model "+modelJarPath+"..."); }
      final var model = MissionModelLoader.loadMissionModel(
          plan.simulationStartTimestamp.toInstant(),
          SerializedValue.of(plan.configuration),
          modelJarPath,
          modelJarPath.getFileName().toString(),
          ""
      );

      return new Arguments.SimulationArguments<>(model, plan, verbose, outputFilePath, extentUpdatePeriod);
    } catch (MissionModelLoader.MissionModelLoadException e) {
      throw new RuntimeException("Error while loading mission model file: "+modelJarPath,e);
    }
  }

  private static void simulate(Arguments.SimulationArguments<?> simArgs) {
    if (simArgs.verbose()) { System.out.println("Simulating Plan..."); }

    // TODO: Update this to a streaming manager streaming to temp files
    //       this will impact how results are written
    final InMemorySimulationResourceManager rmgr = new InMemorySimulationResourceManager();
    final var canceledListener = new CanceledListener();
    final var extentConsumer = simArgs.verbose? new SimulationExtentConsumer(simArgs.extentUpdatePeriod) : new SimulationExtentConsumer();

    final var planDuration = Duration.of(simArgs.plan.startTimestamp.microsUntil(simArgs.plan.endTimestamp),
                                     Duration.MICROSECOND);
    final var simulationDuration = Duration.of(simArgs.plan.simulationStartTimestamp.microsUntil(simArgs.plan.simulationEndTimestamp),
                                     Duration.MICROSECOND);

    // Cancel support
    Thread shutdownHook = null;
    final var resultsThread = new Callable<SimulationResults>() {
        @Override
        public SimulationResults call() {
          return SimulationDriver.simulate(
          simArgs.missionModel,
          simArgs.plan.activityDirectives,
          simArgs.plan.simulationStartTimestamp.toInstant(),
          simulationDuration,
          simArgs.plan.startTimestamp.toInstant(),
          planDuration,
          canceledListener,
          extentConsumer,
          rmgr);
        }
      };

    try(final var exec = Executors.newSingleThreadExecutor()) {
      final Future<SimulationResults> f = exec.submit(resultsThread);

      shutdownHook = new Thread(() -> {
        canceledListener.cancel();
        try {
          final var results = f.get();

          if (simArgs.verbose()) { System.out.println("Writing Results..."); }
          final var resultsWriter = new SimulationResultsWriter(results, simArgs.plan);

          simArgs.outputFilePath().ifPresentOrElse(
              p -> resultsWriter.writeResults(canceledListener, p, extentConsumer),
              () -> resultsWriter.writeResults(canceledListener, extentConsumer)
          );

        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
        }
      });

      // Surround awaiting sim results in a thread to output partial results during SIGINT
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      final var results = f.get();
      if(!canceledListener.get()) {
        // Avoid two threads writing to the output file at the same time
        Runtime.getRuntime().removeShutdownHook(shutdownHook);

        if (simArgs.verbose()) { System.out.println("Writing Results..."); }
        final var resultsWriter = new SimulationResultsWriter(results, simArgs.plan);
        simArgs.outputFilePath().ifPresentOrElse(
            p -> resultsWriter.writeResults(canceledListener, p, extentConsumer),
            () -> resultsWriter.writeResults(canceledListener, extentConsumer)
        );
      }
    } catch (ExecutionException e) {
      if(e.getCause() instanceof SimulationException se) {
        writeSimulationException(se);
        System.exit(1);
      }
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (IllegalStateException ise) {
      // If this is the message, it must've come from Runtime.getRuntime().removeShutdownHook and can be safely ignored
      if(!ise.getMessage().contains("Shutdown in progress")) throw ise;
    } finally {
      extentConsumer.close();
      // Try-catch wrapping in case this is executed while the shutdown hook is running.
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalStateException ise) {}
    }
  }

  private static void writeSimulationException(SimulationException ex) {
    final var dataBuilder = Json.createObjectBuilder()
                                  .add("elapsedTime", SimulationException.formatDuration(ex.elapsedTime))
                                  .add("utcTimeDoy", SimulationException.formatInstant(ex.instant));
      ex.directiveId.ifPresent(directiveId -> dataBuilder.add("executingDirectiveId", directiveId.id()));
      ex.activityType.ifPresent(activityType -> dataBuilder.add("executingActivityType", activityType));
      ex.activityStackTrace.ifPresent(activityStackTrace -> dataBuilder.add("activityStackTrace", activityStackTrace));

      final var exception = Json.createObjectBuilder()
                                .add("type", "SIMULATION_EXCEPTION")
                                .add("message", ex.cause.getMessage())
                                .add("data", dataBuilder)
                                .add("trace", ex.cause.toString())
                                .build();

      final Map<String,String> config = Map.of(JsonGenerator.PRETTY_PRINTING, "");
      try(final var jsonWriter = Json.createWriterFactory(config).createWriter(System.err)) {
        jsonWriter.writeObject(exception);
      }
  }

  /**
   * Display top-level help for the application
   */
  private static void displayTopLevelHelp() {
    System.out.printf(
    """
    usage: stateless-aerie COMMAND [ARGS]...

    Available commands:
     - simulate: Simulate a plan using the specified model and configuration
    %s
    %n""", FOOTER);
  }

  /**
   * Build the parser options for the "simulate" command.
   */
  private static Options createSimulationOptions() {
    // Required Args
    final Option modelPath = new Option("m", "model", true, "path to model jar");
    modelPath.setRequired(true);
    modelPath.setConverter(Path::of);

    final Option planPath = new Option("p", "plan", true, "path to plan json");
    planPath.setRequired(true);
    planPath.setConverter(Path::of);

    // Optional Path Args
    final Option simConfigPath = new Option("s", "sim_config", true, "path to simulation configuration json");
    simConfigPath.setRequired(false);
    simConfigPath.setConverter(s -> Optional.of(Path.of(s)));

    final Option outputFile = new Option("f", "file", true, "output file path");
    outputFile.setRequired(false);
    outputFile.setConverter(f -> Optional.of(Path.of(f)));

    // Other Optional Args
    final Option verbose = new Option("v", "verbose", false, "verbosity of simulation");

    final Option extentUpdateFrequency = new Option("i", "update_interval", true, "minimum interval that simulation extent updates are posted, in milliseconds" );
    extentUpdateFrequency.setRequired(false);
    extentUpdateFrequency.setConverter(Long::parseLong);

    final Options simulationOptions = new Options();
    simulationOptions.addOption(verbose);
    simulationOptions.addOption(modelPath);
    simulationOptions.addOption(planPath);
    simulationOptions.addOption(simConfigPath);
    simulationOptions.addOption(outputFile);
    simulationOptions.addOption(extentUpdateFrequency);
    return simulationOptions;
  }

  /**
   * Check if the "help" option was passed for a given command
   *   and, if so, print the command's help message and exit the program with status code 0.
   * Checked independently to avoid required args for the command causing parsing issues.
   * @param args the args passed into the commandline.
   * @param subCommandOptions the parser options normally used to parse this command.
   * @param subcommand the name of the subcommand (ie "simulate").
   * @param subcommandDescription the description of what the subcommand does.
   */
  private static void checkForHelp(
      String[] args,
      Options subCommandOptions,
      String subcommand,
      String subcommandDescription
  ) throws ParseException  {
    for(final var opt : args) {
      if (opt.equals("-h") || opt.equals("--help")) {
        subCommandOptions.addOption(HELP_OPTION);
        new HelpFormatter().printHelp(
            "stateless-aerie " + subcommand,
            subcommandDescription,
            subCommandOptions,
            FOOTER,
            true);
        System.exit(0);
      }
    }
  }
}
