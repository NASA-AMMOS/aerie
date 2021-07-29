package gov.nasa.jpl.aerie.merlin.server.config;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public record AppConfiguration (
    int httpPort,
    JavalinLoggingState javalinLogging,
    Optional<Path> missionModelConfigPath,
    Path merlinFileStore,
    Store store
) {
  public AppConfiguration {
    Objects.requireNonNull(javalinLogging);
    Objects.requireNonNull(missionModelConfigPath);
    Objects.requireNonNull(merlinFileStore);
    Objects.requireNonNull(store);
  }

  public Path merlinJarsPath() { return merlinFileStore.resolve("jars"); }
  public Path merlinFilesPath() { return merlinFileStore.resolve("files"); }
}
