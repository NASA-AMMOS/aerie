package gov.nasa.jpl.aerie.merlin.server.config;

import javax.json.JsonObject;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public record AppConfiguration (
    int httpPort,
    JavalinLoggingState javalinLogging,
    Optional<String> missionModelConfigPath,
    MongoStore store
) {
    public AppConfiguration {
        Objects.requireNonNull(javalinLogging);
        Objects.requireNonNull(missionModelConfigPath);
        Objects.requireNonNull(store);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AppConfiguration parseProperties(final JsonObject config) {
        var builder = builder();

        builder.setHttpPort(config.getInt("HTTP_PORT"));
        builder.setMongoUri(URI.create(config.getString("MONGO_URI")));
        builder.setMongoDatabase(config.getString("MONGO_DATABASE"));
        builder.setMongoPlanCollection(config.getString("MONGO_PLAN_COLLECTION"));
        builder.setMongoActivityCollection(config.getString("MONGO_ACTIVITY_COLLECTION"));
        builder.setMongoAdaptationCollection(config.getString("MONGO_ADAPTATION_COLLECTION"));
        builder.setMongoSimulationResultsCollection(config.getString("MONGO_SIMULATION_RESULTS_COLLECTION"));

        if (config.getBoolean("enable-javalin-logging", false)) {
            builder.setJavalinLogging(JavalinLoggingState.Enabled);
        }

        if (config.containsKey("MISSION_MODEL_CONFIG_PATH")) {
            builder.setMissionModelConfigPath(config.getString("MISSION_MODEL_CONFIG_PATH"));
        }

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
        private Optional<JavalinLoggingState> enableJavalinLogging = Optional.empty();
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
        public Builder setJavalinLogging(JavalinLoggingState enableJavalinLogging) {
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
                this.enableJavalinLogging.orElse(JavalinLoggingState.Disabled),
                this.missionModelConfigPath,
                new MongoStore(
                    this.mongoUri.orElseThrow(),
                    this.mongoDatabase.orElseThrow(),
                    this.mongoPlanCollection.orElseThrow(),
                    this.mongoActivityCollection.orElseThrow(),
                    this.mongoAdaptationCollection.orElseThrow(),
                    this.mongoSimulationResultsCollection.orElseThrow()));
        }
    }
}
