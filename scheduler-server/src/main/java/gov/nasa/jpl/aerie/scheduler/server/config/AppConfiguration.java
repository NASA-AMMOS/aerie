package gov.nasa.jpl.aerie.scheduler.server.config;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @param httpPort the network port on which the scheduler should listen for http requests
 * @param javalinLogging controls the level of http access logging from javalin endpoints
 * @param merlinFileStore mounted filesystem path to the merlin file store (used as a backdoor to access mission
 *     model jars). should be the entry path, not the jar-specific subdirectory. note this path is distinct from any
 *     scheduler specific file store.
 */
//TODO: remove backdoor access to directly mounted merlinFileStore (eg via merlin endpoint for downloading mission jars)
public record AppConfiguration(
    int httpPort,
    JavalinLoggingState javalinLogging,
    Path merlinFileStore
)
{
  public AppConfiguration {
    Objects.requireNonNull(javalinLogging);
    Objects.requireNonNull(merlinFileStore);
    //NB: ok if the merlin file store not created yet at app init; just needs to exist by first use
  }

  /**
   * @return path to the mounted merlin file store subdirectory that houses mission model jar files
   */
  public Path merlinJarsPath() {
    //TODO: some cross-module synchronization of these magic path terms (if this backdoor persists)
    return merlinFileStore.resolve("jars");
  }

}
