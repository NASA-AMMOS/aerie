package gov.nasa.jpl.aerie.merlin.server.config;

import javax.json.JsonObject;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public record AppConfiguration (
    int HTTP_PORT,
    URI MONGO_URI,
    String MONGO_DATABASE,
    String MONGO_PLAN_COLLECTION,
    String MONGO_ACTIVITY_COLLECTION,
    String MONGO_ADAPTATION_COLLECTION,
    String MONGO_SIMULATION_RESULTS_COLLECTION,
    boolean enableJavalinLogging,
    Optional<String> MISSION_MODEL_CONFIG_PATH
) {
    public AppConfiguration {
        Objects.requireNonNull(MONGO_URI);
        Objects.requireNonNull(MONGO_DATABASE);
        Objects.requireNonNull(MONGO_PLAN_COLLECTION);
        Objects.requireNonNull(MONGO_ACTIVITY_COLLECTION);
        Objects.requireNonNull(MONGO_ADAPTATION_COLLECTION);
        Objects.requireNonNull(MONGO_SIMULATION_RESULTS_COLLECTION);
        Objects.requireNonNull(MISSION_MODEL_CONFIG_PATH);
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
            .setMongoSimulationResultsCollection(config.getString("MONGO_SIMULATION_RESULTS_COLLECTION"))
            .setJavalinLogging(config.getBoolean("enable-javalin-logging", false));

        Optional.ofNullable(config.getString("MISSION_MODEL_CONFIG_PATH", null)).map(builder::setMissionModelConfigPath);
        return builder.build();
    }

    public static final class Builder {
        private Optional<Integer> httpPort = Optional.empty();
        private Optional<URI> mongoUri = Optional.empty();
        private Optional<String> mongoDatabase = Optional.empty();
        private Optional<String> mongoPlanCollection = Optional.empty();
        private Optional<String> mongoActivityCollection = Optional.empty();
        private Optional<String> mongoAdaptationCollection = Optional.empty();
        private Optional<String> mongoSimulationResultsCollection = Optional.empty();
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
        public Builder setMongoSimulationResultsCollection(String mongoSimulationResultsCollection) {
          this.mongoSimulationResultsCollection = Optional.of(mongoSimulationResultsCollection);
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
            return new AppConfiguration(
                this.httpPort.orElseThrow(),
                this.mongoUri.orElseThrow(),
                this.mongoDatabase.orElseThrow(),
                this.mongoPlanCollection.orElseThrow(),
                this.mongoActivityCollection.orElseThrow(),
                this.mongoAdaptationCollection.orElseThrow(),
                this.mongoSimulationResultsCollection.orElseThrow(),
                this.enableJavalinLogging.orElse(false),
                this.missionModelConfigPath);
        }
    }
}
