package gov.nasa.jpl.aerie.merlin.server.config;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.ProductParsers.JsonObjectParser;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.boolP;
import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.PathJsonParser.pathP;
import static gov.nasa.jpl.aerie.json.SumParsers.sumP;
import static gov.nasa.jpl.aerie.json.SumParsers.variant;
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

  private static final JsonParser<JavalinLoggingState> loggingStateP =
      boolP.map(Iso.of(
          untuple($ -> $ ? JavalinLoggingState.Enabled : JavalinLoggingState.Disabled),
          $ -> tuple($ == JavalinLoggingState.Enabled)));

  private record Server(int port, JavalinLoggingState loggingState, Path merlinFileStore) {}
  private static final JsonParser<Server> serverP =
      productP
          .field("port", intP)
          .optionalField("logging", loggingStateP)
          .field("file-store", pathP)
          .map(Iso.of(
              untuple((port, logging, merlinFileStorePath) -> new Server(
                  port,
                  logging.orElse(JavalinLoggingState.Disabled),
                  merlinFileStorePath)),
              $ -> tuple(
                  $.port(),
                  Optional.of($.loggingState()),
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

  private static final JsonObjectParser<MongoStore> mongoStoreP =
      productP
          .field("uri", uriP)
          .field("database", stringP)
          .field("collections", mongoCollectionsP)
          .map(Iso.of(
              untuple((uri, database, collections) -> new MongoStore(
                  uri,
                  database,
                  collections.plans(),
                  collections.activities(),
                  collections.missionModels(),
                  collections.results())),
              $ -> tuple(
                  $.uri(),
                  $.database(),
                  new Collections(
                      $.planCollection(),
                      $.activityCollection(),
                      $.adaptationCollection(),
                      $.simulationResultsCollection()))));

  private static final JsonObjectParser<InMemoryStore> inMemoryStoreP =
      productP
          .map(Iso.of(
              untuple($ -> new InMemoryStore()),
              $ -> tuple(Unit.UNIT)));

  private static final JsonParser<Store> storeP =
      sumP("type", Store.class, List.of(
          variant("mongo", MongoStore.class, mongoStoreP),
          variant("in-memory", InMemoryStore.class, inMemoryStoreP)));

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
