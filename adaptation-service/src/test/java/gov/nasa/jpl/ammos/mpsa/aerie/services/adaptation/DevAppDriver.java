package gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.app.LocalApp;
import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.app.App;
import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.http.AdaptationBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.http.AdaptationExceptionBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.http.AdaptationRepositoryExceptionBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.http.LocalAppExceptionBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.mocks.MockAdaptationRepository;
import io.javalin.Javalin;

public final class DevAppDriver {
    private static final int HTTP_PORT = 27182;

    public static void main(final String[] args) {
        // Assemble the core non-web object graph.
        final App app = new LocalApp(new MockAdaptationRepository());

        // Configure an HTTP server.
        final Javalin javalin = Javalin.create(config -> config
            .enableDevLogging()
            .enableCorsForAllOrigins()
            .registerPlugin(new AdaptationBindings(app))
            .registerPlugin(new LocalAppExceptionBindings())
            .registerPlugin(new AdaptationRepositoryExceptionBindings())
            .registerPlugin(new AdaptationExceptionBindings()));

        // Start the HTTP server.
        javalin.start(HTTP_PORT);
    }
}
