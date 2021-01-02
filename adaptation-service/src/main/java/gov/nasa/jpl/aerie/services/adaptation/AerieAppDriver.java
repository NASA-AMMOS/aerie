package gov.nasa.jpl.aerie.services.adaptation;

import gov.nasa.jpl.aerie.services.adaptation.app.App;
import gov.nasa.jpl.aerie.services.adaptation.app.LocalApp;
import gov.nasa.jpl.aerie.services.adaptation.http.AdaptationBindings;
import gov.nasa.jpl.aerie.services.adaptation.http.AdaptationExceptionBindings;
import gov.nasa.jpl.aerie.services.adaptation.http.AdaptationRepositoryExceptionBindings;
import gov.nasa.jpl.aerie.services.adaptation.http.LocalAppExceptionBindings;
import gov.nasa.jpl.aerie.services.adaptation.remotes.AdaptationRepository;
import gov.nasa.jpl.aerie.services.adaptation.remotes.RemoteAdaptationRepository;
import io.javalin.Javalin;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AerieAppDriver {
    public static void main(final String[] args) {
        // Fetch application configuration properties.
        final AppConfiguration configuration = loadConfiguration(args);

        // Assemble the core domain object graph.
        final AdaptationRepository adaptationRepository = new RemoteAdaptationRepository(
            configuration.MONGO_URI,
            configuration.MONGO_DATABASE,
            configuration.MONGO_ADAPTATION_COLLECTION);
        final App app = new LocalApp(adaptationRepository);

        // Configure an HTTP server.
        final Javalin javalin = Javalin.create(config -> {
            config.showJavalinBanner = false;
            if (configuration.enableJavalinLogging) config.enableDevLogging();
            config
                .enableCorsForAllOrigins()
                .registerPlugin(new AdaptationBindings(app))
                .registerPlugin(new LocalAppExceptionBindings())
                .registerPlugin(new AdaptationRepositoryExceptionBindings())
                .registerPlugin(new AdaptationExceptionBindings());
        });

        // Start the HTTP server.
        javalin.start(configuration.HTTP_PORT);
    }

    private static AppConfiguration loadConfiguration(final String[] args) {
        // Determine where we're getting our configuration from.
        final InputStream configStream;
        if (args.length > 0) {
            try {
                configStream = Files.newInputStream(Path.of(args[0]));
            } catch (final IOException ex) {
                System.err.printf("Configuration file \"%s\" could not be loaded: %s\n", args[0], ex.getMessage());
                System.exit(1);
                throw new Error(ex);
            }
        } else {
            configStream = AerieAppDriver.class.getResourceAsStream("config.json");
        }

        // Read and process the configuration source.
        final JsonObject config = (JsonObject)(Json.createReader(configStream).readValue());
        return AppConfiguration.parseProperties(config);
    }
}
