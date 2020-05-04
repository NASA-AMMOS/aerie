package gov.nasa.jpl.ammos.mpsa.aerie.adaptation;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import javax.json.JsonObject;

public final class AppConfiguration {
    public final int HTTP_PORT;
    public final URI MONGO_URI;
    public final String MONGO_DATABASE;
    public final String MONGO_ADAPTATION_COLLECTION;

    public AppConfiguration(final Builder builder) {
        this.HTTP_PORT = Objects.requireNonNull(builder.httpPort.orElse(null));
        this.MONGO_URI = Objects.requireNonNull(builder.mongoUri.orElse(null));
        this.MONGO_DATABASE = Objects.requireNonNull(builder.mongoDatabase.orElse(null));
        this.MONGO_ADAPTATION_COLLECTION = Objects.requireNonNull(builder.mongoAdaptationCollection.orElse(null));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AppConfiguration parseProperties(final JsonObject config) {
        return builder()
            .setHttpPort(config.getInt("HTTP_PORT"))
            .setMongoUri(URI.create(config.getString("MONGO_URI")))
            .setMongoDatabase(config.getString("MONGO_DATABASE"))
            .setMongoAdaptationCollection(config.getString("MONGO_ADAPTATION_COLLECTION"))
            .build();
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

    public static final class Builder {
        private Optional<Integer> httpPort;
        private Optional<URI> mongoUri;
        private Optional<String> mongoDatabase;
        private Optional<String> mongoAdaptationCollection;

        private Builder() {}

        public Builder setHttpPort(int httpPort) {
            this.httpPort = Optional.of(httpPort);
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
        public Builder setMongoAdaptationCollection(String mongoAdaptationCollection) {
            this.mongoAdaptationCollection = Optional.of(mongoAdaptationCollection);
            return this;
        }

        public AppConfiguration build() {
            return new AppConfiguration(this);
        }
    }
}
