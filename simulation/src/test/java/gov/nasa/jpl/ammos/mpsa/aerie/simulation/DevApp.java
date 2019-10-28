package gov.nasa.jpl.ammos.mpsa.aerie.simulation;

import gov.nasa.jpl.ammos.mpsa.aerie.simulation.http.SimulationBindings;
import io.javalin.Javalin;

public class DevApp {
  static private final int HTTP_PORT = 27185;

  static public void main(final String[] args) {
    // Assemble the core non-web object graph.
    final SimulationBindings bindings = new SimulationBindings();

    // Initiate an HTTP server.
    final Javalin javalin = Javalin.create();
    bindings.registerRoutes(javalin);
    javalin.start(HTTP_PORT);
  }
}
