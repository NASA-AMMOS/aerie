package gov.nasa.jpl.ammos.mpsa.aerie.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.AdaptationController;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.IAdaptationController;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http.AdaptationBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.RemoteAdaptationRepository;
import io.javalin.Javalin;

public class App {

    public static void main(final String[] args) {

        // Load the properties
        AppConfiguration configuration = AppConfiguration.loadProperties();
        if (configuration == null) {
            System.err.println("Not all properties loaded. Exiting.");
            System.exit(1);
        }

        // Assemble the core non-web object graph.
        final AdaptationRepository adaptationRepository = new RemoteAdaptationRepository(configuration.MONGO_URI, configuration.MONGO_DATABASE, configuration.MONGO_ADAPTATION_COLLECTION);
        final IAdaptationController controller = new AdaptationController(adaptationRepository);
        final AdaptationBindings bindings = new AdaptationBindings(controller);
        // Initiate an HTTP server.
        final Javalin javalin = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.enableCorsForAllOrigins();
        });
        bindings.registerRoutes(javalin);
        javalin.start(configuration.HTTP_PORT);
    }


}
