package gov.nasa.jpl.ammos.mpsa.aerie.adaptation;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonObject;

public class AppConfiguration {

    public final int HTTP_PORT;
    public final URI MONGO_URI;
    public final String MONGO_DATABASE;
    public final String MONGO_ADAPTATION_COLLECTION;

    private AppConfiguration(int httpPort, URI mongoUri, String mongoDatabase, String mongoAdaptationCollection) {
        this.HTTP_PORT = httpPort;
        this.MONGO_URI = mongoUri;
        this.MONGO_DATABASE = mongoDatabase;
        this.MONGO_ADAPTATION_COLLECTION = mongoAdaptationCollection;
    }

    public static AppConfiguration loadProperties() {
        int httpPort;
        URI mongoUri;
        String mongoDatabase;
        String mongoAdaptationCollection;

        try {
            InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.json");
            JsonObject config = (JsonObject)(Json.createReader(configStream).readValue());

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

    private static File getFileFromResources(String fileName) throws FileNotFoundException {
        ClassLoader classLoader = AppConfiguration.class.getClassLoader();

        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new FileNotFoundException("File is not found!");
        } else {
            return new File(resource.getFile());
        }

    }

    private static void reportConfigurationLoadError(Exception e) {
        System.err.println("Error while parsing configuration properties: " + e.getMessage());
    }
}
