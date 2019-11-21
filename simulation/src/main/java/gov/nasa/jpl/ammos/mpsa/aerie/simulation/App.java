package gov.nasa.jpl.ammos.mpsa.aerie.simulation;

import gov.nasa.jpl.ammos.mpsa.aerie.simulation.http.SimulationBindings;
import io.javalin.Javalin;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {
  static private final String DEFAULT_CONFIG_RESOURCE = "/gov/nasa/jpl/ammos/mpsa/aerie/simulation/config-default.json";

  static private Reader readFile(final Path path) {
    try {
      return Files.newBufferedReader(path);
    } catch (final IOException ex) {
      throw new RuntimeException("Could not open file `" + path + "`", ex);
    }
  }

  static private Reader readResource(final String resource) {
    return new InputStreamReader(App.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE));
  }

  static private AppConfiguration readArguments(final String[] args) {
    final Reader configReader;
    if (args.length == 1) {
      configReader = readFile(Path.of(args[0]));
    } else {
      configReader = readResource(DEFAULT_CONFIG_RESOURCE);
    }

    final JsonObject json = Json.createParser(configReader).getObject();

    return AppConfiguration.parseProperties(json);
  }

  static public void main(final String[] args) {
    final AppConfiguration config = readArguments(args);

    // Assemble the core non-web object graph.
    // TODO: Provide reference to a SimulationGroup (or some such name)
    //   that will track and provide control over simulations.
    final SimulationBindings bindings = new SimulationBindings();

    // Initiate an HTTP server.
    final Javalin javalin = Javalin.create();
    bindings.registerRoutes(javalin);
    javalin.start(config.http_port);
  }
}
