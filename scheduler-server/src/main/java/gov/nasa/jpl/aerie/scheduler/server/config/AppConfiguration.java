package gov.nasa.jpl.aerie.scheduler.server.config;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @param httpPort the network port on which the scheduler should listen for http requests
 * @param javalinLogging controls the level of http access logging from javalin endpoints
 * @param merlinGraphqlURI endpoint of the merlin graphql service that should be used to fetch/store plan data
 * @param merlinFileStore mounted filesystem path to the merlin file store (used as a backdoor to access mission
 *     model jars). should be the entry path, not the jar-specific subdirectory. note this path is distinct from any
 *     scheduler specific file store.
 * @param missionRuleJarPath path to specific jar file to search for scheduling rules to load (as an interim
 *     solution for allowing scheduling rule configurability by users)
 */
//TODO: remove backdoor access to directly mounted merlinFileStore (eg via merlin endpoint for downloading mission jars)
public record AppConfiguration(
    int httpPort,
    JavalinLoggingState javalinLogging,
    URI merlinGraphqlURI,
    Path merlinFileStore,
    Path missionRuleJarPath,
    PlanOutputMode outputMode
)
{
  public AppConfiguration {
    Objects.requireNonNull(javalinLogging);
    Objects.requireNonNull(merlinGraphqlURI);
    Objects.requireNonNull(merlinFileStore);
    Objects.requireNonNull(missionRuleJarPath);
    Objects.requireNonNull(outputMode);
    //NB: ok if the merlin file store not created yet at app init; just needs to exist by first use
  }

  /**
   * @return path to the mounted merlin file store subdirectory that houses mission model jar files
   */
  public Path missionModelJarsDir() {
    //TODO: some cross-module synchronization of these magic path terms (if this backdoor persists)
    //NB: merlin seems to not actually use the .resolve("jars") subpath despite its mention in merlin...AppConfiguration
    //    (ref PostgresMissionModelRepository where the jar prefix path is hardcoded to just "merlin_file_store")
    return merlinFileStore;
  }

}
