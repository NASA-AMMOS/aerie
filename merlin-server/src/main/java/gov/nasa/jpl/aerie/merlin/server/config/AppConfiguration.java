package gov.nasa.jpl.aerie.merlin.server.config;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public record AppConfiguration (
    int httpPort,
    JavalinLoggingState javalinLogging,
    Optional<Path> missionModelConfigPath,
    String missionModelDataPath,
    Store store
) {
  public AppConfiguration {
    Objects.requireNonNull(javalinLogging);
    Objects.requireNonNull(missionModelConfigPath);
    Objects.requireNonNull(missionModelDataPath);
    Objects.requireNonNull(store);
  }
}
