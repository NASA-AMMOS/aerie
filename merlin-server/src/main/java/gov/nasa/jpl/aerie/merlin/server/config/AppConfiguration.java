package gov.nasa.jpl.aerie.merlin.server.config;

import javax.json.JsonObject;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

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

    public static AppConfiguration parseProperties(final JsonObject config) {
        final var httpPort = config.getInt("HTTP_PORT");
        final var javalinLogging =
            (config.getBoolean("enable-javalin-logging", false))
                ? JavalinLoggingState.Enabled
                : JavalinLoggingState.Disabled;

        final var missionModelConfigPath = getOptional(config, "MISSION_MODEL_CONFIG_PATH", JsonObject::getString);

        final var mongoUri = URI.create(config.getString("MONGO_URI"));
        final var mongoDatabase = config.getString("MONGO_DATABASE");
        final var mongoPlanCollection = config.getString("MONGO_PLAN_COLLECTION");
        final var mongoActivityCollection = config.getString("MONGO_ACTIVITY_COLLECTION");
        final var mongoAdaptationCollection = config.getString("MONGO_ADAPTATION_COLLECTION");
        final var mongoSimulationResultsCollection = config.getString("MONGO_SIMULATION_RESULTS_COLLECTION");

        return new AppConfiguration(
            httpPort,
            javalinLogging,
            missionModelConfigPath,
            new MongoStore(
                mongoUri,
                mongoDatabase,
                mongoPlanCollection,
                mongoActivityCollection,
                mongoAdaptationCollection,
                mongoSimulationResultsCollection));
    }

    private static <T>
    Optional<T> getOptional(final JsonObject object, final String key, final BiFunction<JsonObject, String, T> getter) {
        if (object.containsKey(key)) {
            return Optional.of(getter.apply(object, key));
        } else {
            return Optional.empty();
        }
    }
}
