package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import javax.json.JsonObject;
import java.net.URI;
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
        this.ADAPTATION_URI = Objects.requireNonNull(adaptationUri);
        this.MONGO_URI = Objects.requireNonNull(mongoUri);
        this.MONGO_DATABASE = Objects.requireNonNull(mongoDatabase);
        this.MONGO_PLAN_COLLECTION = Objects.requireNonNull(mongoPlanCollection);
        this.MONGO_ACTIVITY_COLLECTION = Objects.requireNonNull(mongoActvityCollection);
    }

    public static AppConfiguration parseProperties(JsonObject config) {
        final int httpPort = config.getInt("HTTP_PORT");
        final URI adaptationUri = URI.create(config.getString("ADAPTATION_URI"));
        final URI mongoUri = URI.create(config.getString("MONGO_URI"));
        final String mongoDatabase = config.getString("MONGO_DATABASE");
        final String mongoPlanCollection = config.getString("MONGO_PLAN_COLLECTION");
        final String mongoActivityCollection = config.getString("MONGO_ACTIVITY_COLLECTION");

        return new AppConfiguration(httpPort, adaptationUri, mongoUri, mongoDatabase, mongoPlanCollection, mongoActivityCollection);
    }

    // SAFETY: When equals is overridden, so too must hashCode
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppConfiguration)) return false;

        AppConfiguration other = (AppConfiguration)o;

        return this.HTTP_PORT == other.HTTP_PORT
                && Objects.equals(this.ADAPTATION_URI, other.ADAPTATION_URI)
                && Objects.equals(this.MONGO_URI, other.MONGO_URI)
                && Objects.equals(this.MONGO_DATABASE, other.MONGO_DATABASE)
                && Objects.equals(this.MONGO_PLAN_COLLECTION, other.MONGO_PLAN_COLLECTION)
                && Objects.equals(this.MONGO_ACTIVITY_COLLECTION, other.MONGO_ACTIVITY_COLLECTION);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.HTTP_PORT, this.ADAPTATION_URI, this.MONGO_URI, this.MONGO_DATABASE, this.MONGO_PLAN_COLLECTION, this.MONGO_ACTIVITY_COLLECTION);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " {\n" +
                "  HTTP_PORT = " + this.HTTP_PORT + ",\n" +
                "  ADAPTATION_URI = " + this.ADAPTATION_URI + ",\n" +
                "  MONGO_URI = " + this.MONGO_URI + ",\n" +
                "  MONGO_DATABASE = " + this.MONGO_DATABASE + ",\n" +
                "  MONGO_PLAN_COLLECTION = " + this.MONGO_PLAN_COLLECTION + ",\n" +
                "  MONGO_ACTIVITY_COLLECTION = " + this.MONGO_ACTIVITY_COLLECTION + ",\n" +
                "}";
    }
}
