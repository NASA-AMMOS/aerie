package gov.nasa.jpl.aerie.merlin.server;

import javax.json.JsonObject;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public final class AppConfiguration {

    public final int HTTP_PORT;
    public final URI MONGO_URI;
    public final String MONGO_DATABASE;
    public final String MONGO_PLAN_COLLECTION;
    public final String MONGO_ACTIVITY_COLLECTION;
    public final String MONGO_ADAPTATION_COLLECTION;
    public final boolean enableJavalinLogging;
    public final Optional<String> MISSION_MODEL_CONFIG_PATH;

    private AppConfiguration(final Builder builder) {
        this.HTTP_PORT = Objects.requireNonNull(builder.httpPort.orElse(null));
        this.MONGO_URI = Objects.requireNonNull(builder.mongoUri.orElse(null));
        this.MONGO_DATABASE = Objects.requireNonNull(builder.mongoDatabase.orElse(null));
        this.MONGO_PLAN_COLLECTION = Objects.requireNonNull(builder.mongoPlanCollection.orElse(null));
        this.MONGO_ACTIVITY_COLLECTION = Objects.requireNonNull(builder.mongoActivityCollection.orElse(null));
        this.MONGO_ADAPTATION_COLLECTION = Objects.requireNonNull(builder.mongoAdaptationCollection.orElse(null));
        this.enableJavalinLogging = builder.enableJavalinLogging.orElse(false);
        this.MISSION_MODEL_CONFIG_PATH = Objects.requireNonNull(builder.missionModelConfigPath);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AppConfiguration parseProperties(final JsonObject config) {
        final var builder = builder()
            .setHttpPort(config.getInt("HTTP_PORT"))
            .setMongoUri(URI.create(config.getString("MONGO_URI")))
            .setMongoDatabase(config.getString("MONGO_DATABASE"))
            .setMongoPlanCollection(config.getString("MONGO_PLAN_COLLECTION"))
            .setMongoActivityCollection(config.getString("MONGO_ACTIVITY_COLLECTION"))
            .setMongoAdaptationCollection(config.getString("MONGO_ADAPTATION_COLLECTION"))
            .setJavalinLogging(config.getBoolean("enable-javalin-logging", false));

        Optional.ofNullable(config.getString("MISSION_MODEL_CONFIG_PATH", null)).map(builder::setMissionModelConfigPath);
        return builder.build();
    }

    // SAFETY: When equals is overridden, so too must hashCode
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof AppConfiguration)) return false;
        final var other = (AppConfiguration)o;

        return (   (this.HTTP_PORT == other.HTTP_PORT)
                && Objects.equals(this.MONGO_URI, other.MONGO_URI)
                && Objects.equals(this.MONGO_DATABASE, other.MONGO_DATABASE)
                && Objects.equals(this.MONGO_PLAN_COLLECTION, other.MONGO_PLAN_COLLECTION)
                && Objects.equals(this.MONGO_ACTIVITY_COLLECTION, other.MONGO_ACTIVITY_COLLECTION)
                && Objects.equals(this.MONGO_ADAPTATION_COLLECTION, other.MONGO_ADAPTATION_COLLECTION)
                && (this.enableJavalinLogging == other.enableJavalinLogging)
                && Objects.equals(this.MISSION_MODEL_CONFIG_PATH, other.MISSION_MODEL_CONFIG_PATH) );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.HTTP_PORT,
            this.MONGO_URI,
            this.MONGO_DATABASE,
            this.MONGO_PLAN_COLLECTION,
            this.MONGO_ACTIVITY_COLLECTION,
            this.MONGO_ADAPTATION_COLLECTION,
            this.enableJavalinLogging,
            this.MISSION_MODEL_CONFIG_PATH);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " {\n" +
                "  HTTP_PORT = " + this.HTTP_PORT + ",\n" +
                "  MONGO_URI = " + this.MONGO_URI + ",\n" +
                "  MONGO_DATABASE = " + this.MONGO_DATABASE + ",\n" +
                "  MONGO_PLAN_COLLECTION = " + this.MONGO_PLAN_COLLECTION + ",\n" +
                "  MONGO_ACTIVITY_COLLECTION = " + this.MONGO_ACTIVITY_COLLECTION + ",\n" +
                "  MONGO_ADAPTATION_COLLECTION = " + this.MONGO_ADAPTATION_COLLECTION + ",\n" +
                "  enableJavalinLogging = " + this.enableJavalinLogging + ",\n" +
                (this.MISSION_MODEL_CONFIG_PATH.map(s -> "  MISSION_MODEL_CONFIG_PATH = " + s + ",\n").orElse("")) +
                "}";
    }

    public static final class Builder {
        private Optional<Integer> httpPort = Optional.empty();
        private Optional<URI> mongoUri = Optional.empty();
        private Optional<String> mongoDatabase = Optional.empty();
        private Optional<String> mongoPlanCollection = Optional.empty();
        private Optional<String> mongoActivityCollection = Optional.empty();
        private Optional<String> mongoAdaptationCollection = Optional.empty();
        private Optional<Boolean> enableJavalinLogging = Optional.empty();
        private Optional<String> missionModelConfigPath = Optional.empty();

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
        public Builder setMongoPlanCollection(String mongoPlanCollection) {
            this.mongoPlanCollection = Optional.of(mongoPlanCollection);
            return this;
        }
        public Builder setMongoActivityCollection(String mongoActivityCollection) {
            this.mongoActivityCollection = Optional.of(mongoActivityCollection);
            return this;
        }
        public Builder setMongoAdaptationCollection(String mongoAdaptationCollection) {
            this.mongoAdaptationCollection = Optional.of(mongoAdaptationCollection);
            return this;
        }
        public Builder setJavalinLogging(boolean enableJavalinLogging) {
            this.enableJavalinLogging = Optional.of(enableJavalinLogging);
            return this;
        }
        public Builder setMissionModelConfigPath(final String missionModelConfigPath) {
            this.missionModelConfigPath = Optional.of(missionModelConfigPath);
            return this;
        }

        public AppConfiguration build() {
            return new AppConfiguration(this);
        }
    }
}
