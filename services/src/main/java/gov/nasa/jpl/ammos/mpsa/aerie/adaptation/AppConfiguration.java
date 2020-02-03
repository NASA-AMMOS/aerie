package gov.nasa.jpl.ammos.mpsa.aerie.adaptation;

import java.net.URI;
import java.util.Objects;
import javax.json.JsonObject;

public final class AppConfiguration {
    public final int HTTP_PORT;
    public final URI MONGO_URI;
    public final String MONGO_DATABASE;
    public final String MONGO_ADAPTATION_COLLECTION;

    public AppConfiguration(final int httpPort, final URI mongoUri, final String mongoDatabase, final String mongoAdaptationCollection) {
        this.HTTP_PORT = httpPort;
        this.MONGO_URI = Objects.requireNonNull(mongoUri);
        this.MONGO_DATABASE = Objects.requireNonNull(mongoDatabase);
        this.MONGO_ADAPTATION_COLLECTION = Objects.requireNonNull(mongoAdaptationCollection);
    }

    public static AppConfiguration parseProperties(final JsonObject config) {
        final int httpPort = config.getInt("HTTP_PORT");
        final URI mongoUri = URI.create(config.getString("MONGO_URI"));
        final String mongoDatabase = config.getString("MONGO_DATABASE");
        final String mongoAdaptationCollection = config.getString("MONGO_ADAPTATION_COLLECTION");

        return new AppConfiguration(httpPort, mongoUri, mongoDatabase, mongoAdaptationCollection);
    }

    // SAFETY: When equals is overridden, so too must hashCode
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppConfiguration)) return false;
        final AppConfiguration other = (AppConfiguration)o;

        return this.HTTP_PORT == other.HTTP_PORT
                && Objects.equals(this.MONGO_URI, other.MONGO_URI)
                && Objects.equals(this.MONGO_DATABASE, other.MONGO_DATABASE)
                && Objects.equals(this.MONGO_ADAPTATION_COLLECTION, other.MONGO_ADAPTATION_COLLECTION);
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
