package gov.nasa.jpl.aerie.merlin.server.config;

import javax.json.Json;
import javax.json.JsonObject;
import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;

public final class AppConfigurationJsonMapper {
  // TODO: Return something more helpful for failures than an `Optional`.
  public static Optional<AppConfiguration> fromJson(final JsonObject config) {
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

    return Optional.of(new AppConfiguration(
        httpPort,
        javalinLogging,
        missionModelConfigPath,
        new MongoStore(
            mongoUri,
            mongoDatabase,
            mongoPlanCollection,
            mongoActivityCollection,
            mongoAdaptationCollection,
            mongoSimulationResultsCollection)));
  }

  private static <T>
  Optional<T> getOptional(final JsonObject object, final String key, final BiFunction<JsonObject, String, T> getter) {
    if (object.containsKey(key)) {
      return Optional.of(getter.apply(object, key));
    } else {
      return Optional.empty();
    }
  }


  public static JsonObject toJson(final AppConfiguration config) {
    final var builder = Json.createObjectBuilder();

    builder.add("HTTP_PORT", config.httpPort());
    builder.add("enable-javalin-logging", config.javalinLogging().isEnabled());
    config.missionModelConfigPath().ifPresent($ -> builder.add("MISSION_MODEL_CONFIG_PATH", $));
    builder.add("MONGO_URI", config.store().uri().toString());
    builder.add("MONGO_DATABASE", config.store().database());
    builder.add("MONGO_PLAN_COLLECTION", config.store().planCollection());
    builder.add("MONGO_ACTIVITY_COLLECTION", config.store().activityCollection());
    builder.add("MONGO_ADAPTATION_COLLECTION", config.store().adaptationCollection());
    builder.add("MONGO_SIMULATION_RESULTS_COLLECTION", config.store().simulationResultsCollection());

    return builder.build();
  }
}
