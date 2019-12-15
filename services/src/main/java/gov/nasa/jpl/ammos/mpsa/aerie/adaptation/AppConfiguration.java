package gov.nasa.jpl.ammos.mpsa.aerie.adaptation;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonObject;

public final class AppConfiguration {
    public final int HTTP_PORT;
    public final URI MONGO_URI;
    public final String MONGO_DATABASE;
    public final String MONGO_ADAPTATION_COLLECTION;

    public AppConfiguration(int httpPort, URI mongoUri, String mongoDatabase, String mongoAdaptationCollection) {
        this.HTTP_PORT = httpPort;
        this.MONGO_URI = mongoUri;
        this.MONGO_DATABASE = mongoDatabase;
        this.MONGO_ADAPTATION_COLLECTION = mongoAdaptationCollection;
    }

    public static AppConfiguration loadProperties(Path path) throws IOException {
        InputStream configStream = Files.newInputStream(path);
        JsonObject config = (JsonObject)(Json.createReader(configStream).readValue());
        return parseProperties(config);
    }

    public static AppConfiguration loadProperties() {
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("gov/nasa/jpl/ammos/mpsa/aerie/adaptation/config.json");
        JsonObject config = (JsonObject)(Json.createReader(configStream).readValue());
        return parseProperties(config);
    }

    public static AppConfiguration parseProperties(JsonObject config) {
        int httpPort;
        URI mongoUri;
        String mongoDatabase;
        String mongoAdaptationCollection;

        try {
            httpPort = config.getInt("HTTP_PORT");
            mongoUri = URI.create(config.getString("MONGO_URI"));
            mongoDatabase = config.getString("MONGO_DATABASE");
            mongoAdaptationCollection = config.getString("MONGO_ADAPTATION_COLLECTION");

            Objects.requireNonNull(mongoUri);
            Objects.requireNonNull(mongoDatabase);
            Objects.requireNonNull(mongoAdaptationCollection);

        } catch (NullPointerException e) {
            reportConfigurationLoadError(e);
            return null;
        }

        return new AppConfiguration(httpPort, mongoUri, mongoDatabase, mongoAdaptationCollection);
    }

    private static void reportConfigurationLoadError(Exception e) {
        System.err.println("Error while parsing configuration properties: " + e.getMessage());
    }

    // SAFETY: When equals is overridden, so too must hashCode
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppConfiguration)) return false;

        AppConfiguration other = (AppConfiguration)o;

        return this.HTTP_PORT == other.HTTP_PORT
                && this.MONGO_URI.equals(other.MONGO_URI)
                && this.MONGO_DATABASE.equals(other.MONGO_DATABASE)
                && this.MONGO_ADAPTATION_COLLECTION.equals(other.MONGO_ADAPTATION_COLLECTION);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.HTTP_PORT, this.MONGO_URI, this.MONGO_DATABASE, this.MONGO_ADAPTATION_COLLECTION);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " {\n" +
                "  HTTP_PORT = " + this.HTTP_PORT + ",\n" +
                "  MONGO_URI = " + this.MONGO_URI + ",\n" +
                "  MONGO_DATABASE = " + this.MONGO_DATABASE + ",\n" +
                "  MONGO_ADAPTATION_COLLECTION = " + this.MONGO_ADAPTATION_COLLECTION + ",\n" +
                "}";
    }
}
