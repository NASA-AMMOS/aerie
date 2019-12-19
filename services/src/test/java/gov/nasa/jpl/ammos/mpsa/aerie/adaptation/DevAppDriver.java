package gov.nasa.jpl.ammos.mpsa.aerie.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.LocalApp;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.App;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http.AdaptationBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http.AdaptationRepositoryExceptionBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http.LocalAppExceptionBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.Fixtures;
import io.javalin.Javalin;

public final class DevAppDriver {
    private static final int HTTP_PORT = 27182;

    public static void main(final String[] args) {
        // Assemble the core non-web object graph.
        final Fixtures fixtures = new Fixtures();
        final App app = new LocalApp(fixtures.adaptationRepository);

        // Configure an HTTP server.
        final Javalin javalin = Javalin.create(config -> {
            config.enableCorsForAllOrigins();
            config.registerPlugin(new AdaptationBindings(app));
            config.registerPlugin(new LocalAppExceptionBindings());
            config.registerPlugin(new AdaptationRepositoryExceptionBindings());
        });

        // Start the HTTP server.
        javalin.start(HTTP_PORT);
    }
}
