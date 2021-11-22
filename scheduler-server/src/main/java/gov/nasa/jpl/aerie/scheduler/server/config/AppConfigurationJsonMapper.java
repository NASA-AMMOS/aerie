package gov.nasa.jpl.aerie.scheduler.server.config;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;

import javax.json.JsonValue;
import java.nio.file.Path;
import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.boolP;
import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.PathJsonParser.pathP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

/**
 * handles de/serialization of service configuration options from/to json
 */
public final class AppConfigurationJsonMapper {

  /**
   * deserialize the provided top level json element into a configuration object
   *
   * @param json the json serialization of a configuration object to deserialize
   * @return the configuration object represented by the given json serialization
   */
  public static AppConfiguration fromJson(final JsonValue json) {
    return configP.parse(json).getSuccessOrThrow();
  }

  /**
   * serialize the given configuration object into a top level json element
   *
   * @param config the configuration object to serialize into json
   * @return a json value representing the provided configuration object
   */
  public static JsonValue toJson(final AppConfiguration config) {
    return configP.unparse(config);
  }


  /**
   * intermediate parser for the logging state enumeration: true=Enabled and false=Disabled
   */
  //TODO: seems like this should just be a BasicParsers.enumP (but wasn't in MerlinApp either, and no examples or docs)
  private static final JsonParser<JavalinLoggingState> loggingStateP =
      boolP.map(Iso.of(
          untuple(boolVal -> boolVal ? JavalinLoggingState.Enabled : JavalinLoggingState.Disabled),
          enumVal -> tuple(enumVal == JavalinLoggingState.Enabled)));

  /**
   * intermediate container for the http service endpoint options
   */
  private record Server(int port, JavalinLoggingState loggingState, Path merlinFileStore) {}

  /**
   * intermediate parser for the http service endpoint options within the "server" json node
   */
  private static final JsonParser<Server> serverP = productP
      .field("port", intP)
      .optionalField("logging", loggingStateP)
      .field("merlin-file-store", pathP)
      .map(Iso.of(
          untuple((port, logging, merlinFileStorePath) -> new Server(
              port,
              logging.orElse(JavalinLoggingState.Disabled),
              merlinFileStorePath)),
          server -> tuple(
              server.port(),
              Optional.of(server.loggingState()),
              server.merlinFileStore)));

  /**
   * parser for the top level json element in the configuration file; creates the full AppConfiguration record
   */
  //currently a tuple of one, but left for future expansion (stores, etc) parallel to MerlinApp's config
  private static final JsonParser<AppConfiguration> configP = productP
      .field("server", serverP)
      .map(Iso.of(
          untuple((server) -> new AppConfiguration(
              server.port(),
              server.loggingState(),
              server.merlinFileStore())),
          config -> tuple(
              new Server(config.httpPort(), config.javalinLogging(), config.merlinFileStore()))));


}
