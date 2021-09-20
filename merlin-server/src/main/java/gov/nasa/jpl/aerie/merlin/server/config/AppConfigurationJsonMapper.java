package gov.nasa.jpl.aerie.merlin.server.config;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.boolP;
import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.PathJsonParser.pathP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.server.config.AppConfigurationJsonMapper.UriJsonParser.uriP;

public final class AppConfigurationJsonMapper {
  public static AppConfiguration fromJson(final JsonValue config) {
    return configP.parse(config).getSuccessOrThrow();
  }

  public static JsonValue toJson(final AppConfiguration config) {
    return configP.unparse(config);
  }

  private record Server(int port, JavalinLoggingState loggingState, Path merlinFileStore) {}
  private static final JsonParser<Server> serverP =
      productP
          .field("port", intP)
          .optionalField("logging", boolP)
          .field("file-store", pathP)
          .map(Iso.of(
              untuple((port, logging, merlinFileStorePath) -> new Server(
                  port,
                  logging.orElse(false)
                      ? JavalinLoggingState.Enabled
                      : JavalinLoggingState.Disabled,
                  merlinFileStorePath)),
              $ -> tuple(
                  $.port(),
                  Optional.of($.loggingState() == JavalinLoggingState.Enabled),
                  $.merlinFileStore)));

  private record Collections(String plans, String activities, String missionModels, String results) {}
  private static final JsonParser<Collections> mongoCollectionsP =
      productP
          .field("plans", stringP)
          .field("activities", stringP)
          .field("mission-models", stringP)
          .field("results", stringP)
          .map(Iso.of(
              untuple((plans, activities, missionModels, results) ->
                  new Collections(plans, activities, missionModels, results)),
              $ -> tuple($.plans(), $.activities(), $.missionModels(), $.results())));

  private static final JsonParser<MongoStore> mongoStoreP =
      productP
          .field("type", literalP("mongo"))
          .field("uri", uriP)
          .field("database", stringP)
          .field("collections", mongoCollectionsP)
          .map(Iso.of(
              untuple((type, uri, database, collections) -> new MongoStore(
                  uri,
                  database,
                  collections.plans(),
                  collections.activities(),
                  collections.missionModels(),
                  collections.results())),
              $ -> tuple(
                  Unit.UNIT,
                  $.uri(),
                  $.database(),
                  new Collections(
                      $.planCollection(),
                      $.activityCollection(),
                      $.adaptationCollection(),
                      $.simulationResultsCollection()))));

  private static final JsonParser<Store> storeP = chooseP(mongoStoreP);

  private static final JsonParser<AppConfiguration> configP =
      productP
          .field("server", serverP)
          .field("store", storeP)
          .map(Iso.of(
              untuple((server, store) -> new AppConfiguration(
                  server.port(),
                  server.loggingState(),
                  server.merlinFileStore(),
                  store)),
              $ -> tuple(
                  new Server($.httpPort(), $.javalinLogging(), $.merlinFileStore()),
                  $.store())));

  public static final class UriJsonParser implements JsonParser<URI> {
    public static final JsonParser<URI> uriP = new UriJsonParser();

    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      return stringP.getSchema(anchors);
    }

    @Override
    public JsonParseResult<URI> parse(final JsonValue json) {
      final var result = stringP.parse(json);
      if (result instanceof JsonParseResult.Success<String> s) {
        try {
          return JsonParseResult.success(URI.create(s.result()));
        } catch (final IllegalArgumentException ex) {
          return JsonParseResult.failure("Invalid URI");
        }
      } else if (result instanceof JsonParseResult.Failure<?> f) {
        return f.cast();
      } else {
        throw new UnexpectedSubtypeError(JsonParseResult.class, result);
      }
    }

    @Override
    public JsonValue unparse(final URI value) {
      return stringP.unparse(value.toString());
    }
  }
}
