package gov.nasa.jpl.ammos.mpsa.aerie.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.AdaptationController;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.IAdaptationController;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http.AdaptationBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.Fixtures;
import io.javalin.Javalin;

import java.io.IOException;

public class DevApp {
    private static final int HTTP_PORT = 27182;

    public static void main(final String[] args) {
        // Assemble the core non-web object graph.
        final Fixtures fixtures = new Fixtures();
        final IAdaptationController controller = new AdaptationController(fixtures.adaptationRepository);
        final AdaptationBindings bindings = new AdaptationBindings(controller);

        // Initiate an HTTP server.
        final Javalin javalin = Javalin.create();
        bindings.registerRoutes(javalin);

        // Start the HTTP server.
        javalin.start(HTTP_PORT);
    }
}
