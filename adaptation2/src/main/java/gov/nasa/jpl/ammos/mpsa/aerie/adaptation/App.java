package gov.nasa.jpl.ammos.mpsa.aerie.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.AdaptationController;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.IAdaptationController;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http.AdaptationBindings;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.RemoteAdaptationRepository;
import io.javalin.Javalin;

import java.net.URI;

public class App {
    private static final int HTTP_PORT = 27182;

    private static final URI MONGO_URI = URI.create("mongodb://adaptation_mongo:27020");
    private static final String MONGO_DATABASE = "adaptation-service";
    private static final String MONGO_ADAPTATION_COLLECTION = "adaptations";

    public static void main(final String[] args) {
        // Assemble the core non-web object graph.
        final AdaptationRepository adaptationRepository = new RemoteAdaptationRepository(MONGO_URI, MONGO_DATABASE, MONGO_ADAPTATION_COLLECTION);
        final IAdaptationController controller = new AdaptationController(adaptationRepository);
        final AdaptationBindings bindings = new AdaptationBindings(controller);
        // Initiate an HTTP server.
        final Javalin javalin = Javalin.create();
        bindings.registerRoutes(javalin);
        javalin.start(HTTP_PORT);
    }
}
