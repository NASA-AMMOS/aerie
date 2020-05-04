package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import javax.json.JsonObject;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public final class AppConfiguration {
    public final int HTTP_PORT;
    public final URI ADAPTATION_URI;
    public final URI MONGO_URI;
    public final String MONGO_DATABASE;
    public final String MONGO_PLAN_COLLECTION;
    public final String MONGO_ACTIVITY_COLLECTION;

    private AppConfiguration(final Builder builder) {
        this.HTTP_PORT = Objects.requireNonNull(builder.httpPort.orElse(null));
        this.ADAPTATION_URI = Objects.requireNonNull(builder.adaptationServiceUri.orElse(null));
        this.MONGO_URI = Objects.requireNonNull(builder.mongoUri.orElse(null));
        this.MONGO_DATABASE = Objects.requireNonNull(builder.mongoDatabase.orElse(null));
        this.MONGO_PLAN_COLLECTION = Objects.requireNonNull(builder.mongoPlanCollection.orElse(null));
        this.MONGO_ACTIVITY_COLLECTION = Objects.requireNonNull(builder.mongoActivityCollection.orElse(null));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AppConfiguration parseProperties(final JsonObject config) {
        return builder()
            .setHttpPort(config.getInt("HTTP_PORT"))
            .setAdaptationServiceUri(URI.create(config.getString("ADAPTATION_URI")))
            .setMongoUri(URI.create(config.getString("MONGO_URI")))
            .setMongoDatabase(config.getString("MONGO_DATABASE"))
            .setMongoPlanCollection(config.getString("MONGO_PLAN_COLLECTION"))
            .setMongoActivityCollection(config.getString("MONGO_ACTIVITY_COLLECTION"))
            .build();
    }

    // SAFETY: When equals is overridden, so too must hashCode
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppConfiguration)) return false;
        final var other = (AppConfiguration)o;

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

    public static final class Builder {
        private Optional<Integer> httpPort;
        private Optional<URI> adaptationServiceUri;
        private Optional<URI> mongoUri;
        private Optional<String> mongoDatabase;
        private Optional<String> mongoPlanCollection;
        private Optional<String> mongoActivityCollection;

        private Builder() {}

        public Builder setHttpPort(int httpPort) {
            this.httpPort = Optional.of(httpPort);
            return this;
        }
        public Builder setAdaptationServiceUri(URI adaptationServiceUri) {
            this.adaptationServiceUri = Optional.of(adaptationServiceUri);
            return this;
        }
        public Builder setMongoUri(URI mongoUri) {
            this.mongoUri = Optional.of(mongoUri);
            return this;
        }
        public Builder setMongoDatabase(String mongoDatabase) {
            this.mongoDatabase = Optional.of(mongoDatabase);
            return this;
        }
        public Builder setMongoPlanCollection(String mongoPlanCollection) {
            this.mongoPlanCollection = Optional.of(mongoPlanCollection);
            return this;
        }
        public Builder setMongoActivityCollection(String mongoActivityCollection) {
            this.mongoActivityCollection = Optional.of(mongoActivityCollection);
            return this;
        }

        public AppConfiguration build() {
            return new AppConfiguration(this);
        }
    }
}
