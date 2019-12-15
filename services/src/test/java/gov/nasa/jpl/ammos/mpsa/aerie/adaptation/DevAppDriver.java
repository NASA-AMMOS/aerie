package gov.nasa.jpl.ammos.mpsa.aerie.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.LocalApp;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.App;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http.AdaptationBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.Fixtures;
import io.javalin.Javalin;

public class DevAppDriver {
    private static final int HTTP_PORT = 27182;

    public static void main(final String[] args) {
        // Assemble the core non-web object graph.
        final Fixtures fixtures = new Fixtures();
        final App app = new LocalApp(fixtures.adaptationRepository);
        final AdaptationBindings bindings = new AdaptationBindings(app);

        // Initiate an HTTP server.
        final Javalin javalin = Javalin.create(config -> {
            config.enableCorsForAllOrigins();
        });
        bindings.registerRoutes(javalin);

        // Start the HTTP server.
        javalin.start(HTTP_PORT);
    }
}
