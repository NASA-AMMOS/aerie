package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class AppConfiguration {
    public final int HTTP_PORT;
    public final URI ADAPTATION_URI;
    public final URI MONGO_URI;
    public final String MONGO_DATABASE;
    public final String MONGO_PLAN_COLLECTION;
    public final String MONGO_ACTIVITY_COLLECTION;

    public AppConfiguration(int httpPort, URI adaptationUri, URI mongoUri, String mongoDatabase,
                             String mongoPlanCollection, String mongoActvityCollection) {
        this.HTTP_PORT = httpPort;
        this.ADAPTATION_URI = adaptationUri;
        this.MONGO_URI = mongoUri;
        this.MONGO_DATABASE = mongoDatabase;
        this.MONGO_PLAN_COLLECTION = mongoPlanCollection;
        this.MONGO_ACTIVITY_COLLECTION = mongoActvityCollection;
    }

    public static AppConfiguration loadProperties(Path path) throws IOException {
        InputStream configStream = Files.newInputStream(path);
        JsonObject config = (JsonObject)(Json.createReader(configStream).readValue());
        return parseProperties(config);
    }

    public static AppConfiguration loadProperties() {
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.json");
        JsonObject config = (JsonObject)(Json.createReader(configStream).readValue());
        return parseProperties(config);
    }

    public static AppConfiguration parseProperties(JsonObject config) {
        int httpPort;
        URI adaptationUri;
        URI mongoUri;
        String mongoDatabase;
        String mongoPlanCollection;
        String mongoActivityCollection;

        try {
            httpPort = config.getInt("HTTP_PORT");
            adaptationUri = URI.create(config.getString("ADAPTATION_URI"));
            mongoUri = URI.create(config.getString("MONGO_URI"));
            mongoDatabase = config.getString("MONGO_DATABASE");
            mongoPlanCollection = config.getString("MONGO_PLAN_COLLECTION");
            mongoActivityCollection = config.getString("MONGO_ACTIVITY_COLLECTION");

            Objects.requireNonNull(adaptationUri);
            Objects.requireNonNull(mongoUri);
            Objects.requireNonNull(mongoDatabase);
            Objects.requireNonNull(mongoPlanCollection);
            Objects.requireNonNull(mongoActivityCollection);

        } catch (NullPointerException e) {
            reportConfigurationLoadError(e);
            return null;
        }

        return new AppConfiguration(httpPort, adaptationUri, mongoUri, mongoDatabase, mongoPlanCollection, mongoActivityCollection);
    }

    private static void reportConfigurationLoadError(Exception e) {
        System.err.println("Error while parsing configuration properties: " + e.getMessage());
    }
}
